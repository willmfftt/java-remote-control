package org.one.stone.soup.xapp.application.jrc.client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.one.stone.soup.constants.TimeConstants;
import org.one.stone.soup.exception.ExceptionHelper;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.io.Connection;
import org.one.stone.soup.net.SocketConnection;
import org.one.stone.soup.remote.control.client.RemoteClientController;
import org.one.stone.soup.remote.control.hub.HubHelper;
import org.one.stone.soup.screen.recorder.ScreenPlayer;
import org.one.stone.soup.stringhelper.StringArrayHelper;
import org.one.stone.soup.util.TimeWatch;
import org.one.stone.soup.xapp.XApplication;
import org.one.stone.soup.xapp.XappRootApplication;
import org.one.stone.soup.xapp.components.XappProgressBar;
import org.one.stone.soup.xapp.components.XappTextArea;
import org.one.stone.soup.xapp.components.form.XForm;
import org.one.stone.soup.xapp.filebrowser.XappFileBrowser;
import org.one.stone.soup.xapp.resource.manager.DefaultXuiResourceManager;
import org.one.stone.soup.xapp.swing.components.XappSwingApplicationFrame;
import org.one.stone.soup.xapp.swing.components.XappSwingScrollPane;
import org.one.stone.soup.xml.XmlElement;
import org.one.stone.soup.xml.stream.XmlLoader;

public class XappJavaRemoteControlClient extends XApplication implements RemoteClientController,MouseListener,MouseMotionListener,ActionListener{

	private String currentFile=null;
	
	private boolean clientRunning = false;
	private boolean fullscreen = false;
	
	private XappJavaRemoteControlView client;
	private JPanel panel;
	
	private Connection mainConnection;
	private Connection controlConnection;
	private ScreenPlayer screenPlayer;
	private String serverKey;
	private TimeWatch timeoutTimer;
	
	private long frameMonitorTime = 0;
	private int frameMonitorCount = 0;
	
	private static String APP_DEFINITION_PATH = "./org/one/stone/soup/xapp/application/rdc/client/";
	
	public static void main(String[] args)
	{
		if(args.length==1)
		{
			APP_DEFINITION_PATH = args[0];
		}
		try{
			new XappJavaRemoteControlClient();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null,ExceptionHelper.getStackTrace(e));
		}
	}	
	
	public XappJavaRemoteControlClient() throws Exception
	{
		super(APP_DEFINITION_PATH+"jrc-client.xapp");
		
		initialise();
		timeoutTimer = new TimeWatch(30000);
		timeoutTimer.addActionListener(this);
		timeoutTimer.setActionCommand("timedOut");
		
		showMessage("RDC Ready");
	}
	
	public void processDropAction(String file, int posX, int posY) {
		XmlElement data = XmlLoader.load(file);
		setData("client-settings",data);
	}

	public void processTreeAction(XmlElement dataRoot, XmlElement dataSource,
			boolean rightButton, int posX, int posY) {
		// TODO Auto-generated method stub

	}

	public void processPopupAction(XmlElement data) {
		// TODO Auto-generated method stub

	}

	public void processPost(XmlElement data) {
	}

	public void clearStatus()
	{
		XmlElement statusData = ((XForm)XappRootApplication.getComponent("status")).getData();
		XmlElement status = statusData.getElementByName("status");
		String value = status.getValue();
		if(value==null)
		{
			return;
		}
		else
		{
			status.removeData(0);
		}
		
		((XForm)XappRootApplication.getComponent("status")).setData( statusData );
	}
	
	public void appendStatusMessage(String statusMessage)
	{
		XappTextArea dataField = (XappTextArea)XappRootApplication.getComponentStore().getXappComponent("status.status");
		String data = dataField.getData();
		String[] lines = StringArrayHelper.parseFields(data,'\n');
		if(lines.length>10)
		{
			data = StringArrayHelper.arrayToString(lines,"\n",lines.length-10,lines.length)+'\n';
			dataField.setData( data );
		}
		dataField.append(statusMessage+'\n');
	}

	public void showMessage(String message) {
		appendStatusMessage( message );		
	}
	
	public boolean save()
	{
		if(currentFile==null || currentFile.equals("*"))
		{
			return saveAs();
		}
		else
		{
			FileHelper.buildFile(currentFile,getData().toXml());
			return true;
		}		
	}

	public boolean open()
	{
		XappFileBrowser browser = XappRootApplication.getComponentFactory().buildFileBrowser(" xml file","xml","xml");
		browser.setOpenDir(currentFile+".xml");
		String file = browser.getOpenFile();
		
		if(file!=null)
		{
			setData(XmlLoader.load(file));
			currentFile = file;
			getFrame().setTitle( getTitle()+" - "+new File(file).getName() );
			return true;
		}
		return false;
	}

	public boolean saveAs()
	{
		XappFileBrowser browser = XappRootApplication.getComponentFactory().buildFileBrowser(" xml","xml","xml");
		
		String file=null;
		
		if(currentFile==null)
		{
			file = browser.getSaveAsFile( "compilation.xml" );			
		}
		else
		{
			file = browser.getSaveAsFile( new File(currentFile).getName() );
		}
		
		if(file!=null)
		{
			FileHelper.buildFile(file,getData().toXml());
			currentFile = file;
			getFrame().setTitle( getTitle()+" - "+new File(file).getName() );
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public XmlElement getData()
	{
		return getFormData("server-settings");
	}
	
	public void setData(XmlElement data)
	{
		setData("server-settings",data);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent action) {
		
		if(action.getActionCommand().equals("connect"))
		{
			connect();
		}
		else if(action.getActionCommand().equals("disconnect") )
		{
			disconnect();
		}
		else if(action.getActionCommand().equals("timedOut") )
		{
			showMessage("Timed out waiting for next frame.");
			disconnect();
		}
	}

	public void connect()
	{
		if(clientRunning==true)
		{
			XappRootApplication.displayMessage("Client already connected.");
			return;
		}
		
		clientRunning = true;
		
		String address = XappRootApplication.getAPI().getField("client-settings","serverAddress");
		int port = Integer.parseInt( XappRootApplication.getAPI().getField("client-settings","serverPort") );
		String useHub = XappRootApplication.getAPI().getField("client-settings","useHub");	
		
		String hubAlias = XappRootApplication.getAPI().getField("client-settings","userName");
		
//		boolean useTunnel = new Boolean(XappRootApplication.getAPI().getField("client-settings","useTunnel")).booleanValue();	
//		String tunnelHost = XappRootApplication.getAPI().getField("client-settings","tunnelHost");	

		boolean viewOnly = new Boolean(XappRootApplication.getAPI().getField("client-settings","viewOnly")).booleanValue();		
		
		try{
			/*if(useTunnel==true)
			{
				mainConnection = new HttpTunnelConnection(tunnelHost,address,port);
			}
			else
			{*/
				mainConnection = new SocketConnection( new Socket(address,port),address+":"+port );
			//}
		
			client = new XappJavaRemoteControlView(new DefaultXuiResourceManager(),this);
			showClient();
			
			if(useHub.equals("true"))
			{
				showMessage("Logging main into hub.");
				loginToHub("client.screen",mainConnection,null);
				showMessage("Logged main in.");
			}
			
			showMessage("Logged in. Sending alias "+hubAlias+" to server.");
			HubHelper.writeLine(hubAlias,mainConnection.getOutputStream());
			showMessage("Done.");
			
			InputStream iStream = mainConnection.getInputStream();
	
			showMessage("Reading key.");
			String targetKey = HubHelper.readLine(iStream);
			if(targetKey.length()==0)
			{
				XappRootApplication.displayMessage("Connection refused");
				disconnect();
				return;
			}
			
			//System.out.println("Key:"+targetKey);
			
			screenPlayer = new ScreenPlayer(iStream,client);
			
			clientRunning = true;
						
			/*if(useTunnel==true)
			{
				controlConnection = new HttpTunnelConnection(tunnelHost,address,port);
			}
			else
			{*/
				controlConnection = new SocketConnection( new Socket(address,port),address+":"+port );
			//}
			
			if(useHub.equals("true"))
			{
				showMessage("Logging control into hub.");
				loginToHub("client.control",controlConnection,serverKey);
				showMessage("Logged control in.");
			}
	
			//System.out.println("Send Key:"+targetKey);			
			HubHelper.writeLine(targetKey,controlConnection.getOutputStream());							
			showMessage("Key Sent.");
	
			if( viewOnly==false )
			{
				showMessage("Sending Full Control request.");
				HubHelper.writeLine("fullControl",controlConnection.getOutputStream());
				showMessage("Full Control request sent.");
	
				client.getView().addMouseListener(this);
				client.getView().addMouseMotionListener(this);
				client.getView().addKeyListener(this);
			}
			else
			{
				System.out.println("Sending Flow Control request.");				
				HubHelper.writeLine("flowControl",controlConnection.getOutputStream());
				System.out.println("Flow Control request sent.");
			}
		
			System.out.println("Screen Player Started");
			
			screenPlayer.play();
			
			timeoutTimer.start();
			
			//client.doLayout();
			//panel.doLayout();
			//((XappSwingScrollPane)XappRootApplication.getComponent("view")).doLayout();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			disconnect();
		}
		
	}

	public void doLayout()
	{
		panel.doLayout();
	}
	
	public void showClient()
	{
		panel = new JPanel();
		panel.setLayout( new FlowLayout(FlowLayout.LEFT) );
		panel.add(client);
		
		((XappSwingScrollPane)XappRootApplication.getComponent("view")).add(panel);
		client.addKeyListener(this);
		panel.addKeyListener(this);
		((XappSwingApplicationFrame)this.getFrame()).addKeyListener(this);
	}
	
	public void disconnect()
	{
		if(clientRunning==false)
		{
			XappRootApplication.displayMessage("Client not connected.");
			return;
		}
		
		timeoutTimer.stop();
		
		try{
			client.getView().removeMouseListener(this);
			client.getView().removeMouseMotionListener(this);
			client.getView().removeKeyListener(this);
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
		try{ mainConnection.close(); }catch(Exception e){}
		try{ controlConnection.close(); }catch(Exception e){}
		
		screenPlayer=null;
		
		try {
			client.dispose();
			client = null;
		}catch(Exception e){}
		
		try{
			clientRunning=false;
		}catch(Exception e){}
		showMessage("Disconnected from Remote Machine");
	}

	public void keyPressed(KeyEvent e) 
	{
		if(e.getKeyCode()==KeyEvent.VK_F11)
		{
			setFullscreen();
		}
		
		XmlElement packet = new XmlElement("keyPressed");
		packet.addAttribute("code",""+e.getKeyCode());
		sendCommand(packet);
	}

	public void setFullscreen()
	{
		if(client!=null)
		{
			fullscreen = !fullscreen;
			client.setFullscreen(fullscreen);
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
		XmlElement packet = new XmlElement("keyReleased");
		packet.addAttribute("code",""+e.getKeyCode());
		sendCommand(packet);
	}

	public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
	}

	public void mouseMoved(MouseEvent e)
	{	
		XmlElement packet = new XmlElement("mouseMoved");
		packet.addAttribute("x",""+e.getX());
		packet.addAttribute("y",""+e.getY());
		sendCommand(packet);
	}

	public void mousePressed(MouseEvent e)
	{
		XmlElement packet = new XmlElement("mousePressed");
		packet.addAttribute("code",""+e.getModifiers());
		sendCommand(packet);
	}

	public void mouseReleased(MouseEvent e)
	{
		XmlElement packet = new XmlElement("mouseReleased");
		packet.addAttribute("code",""+e.getModifiers());
		sendCommand(packet);
	}

	public void requestNextFrame()
	{
		frameMonitorCount++;
		
		if(System.currentTimeMillis()-frameMonitorTime>TimeConstants.SECOND_MILLIS*10)
		{
			XappProgressBar progress = ((XappProgressBar)XappRootApplication.getComponent("rates.frameRate"));
			progress.setData(""+(frameMonitorCount*3));
			progress.setTitle("Frames / Minute:"+(frameMonitorCount*6));
			
			frameMonitorTime=System.currentTimeMillis();			
			frameMonitorCount=0;
		}
		
		XmlElement packet = new XmlElement("nextFrame");
		sendCommand(packet);
		
		timeoutTimer.reset();
	}

	private synchronized void sendCommand(XmlElement packet)
	{
		if(controlConnection==null)
		{
			return;
		}
		try{
			controlConnection.getOutputStream().write(packet.toXmlNoBeautify().getBytes());
			controlConnection.getOutputStream().flush();
			
			//System.out.println("Sent: "+packet.toXmlNoBeautify());
		}
		catch(Exception e){
			System.out.println("Failed to send "+packet.toXmlNoBeautify());
			disconnect();
		}
	}

	private void loginToHub(String subdomain,Connection connection,String password) throws IOException
	{
		String hubAlias = XappRootApplication.getAPI().getField("client-settings","userName");
		password = XappRootApplication.getAPI().getField("client-settings","password");		
		String serverAlias = XappRootApplication.getAPI().getField("client-settings","connectTo");
	
		serverKey = HubHelper.login(subdomain,connection.getOutputStream(),connection.getInputStream(),hubAlias,password);
	
		showMessage("Sending server alias "+serverAlias);		
		HubHelper.writeLine(serverAlias,connection.getOutputStream());		
		
		showMessage("Logged into hub.");		
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent e) {
		((XappSwingApplicationFrame)getFrame()).setCursor(Cursor.CROSSHAIR_CURSOR);		
	}

	public void mouseExited(MouseEvent e) {
		((XappSwingApplicationFrame)getFrame()).setCursor(Cursor.DEFAULT_CURSOR);		
	}
}
