package org.one.stone.soup.xapp.application.jrc.server;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JOptionPane;

import org.one.stone.soup.exception.ExceptionHelper;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.remote.control.server.ControlAdapter;
import org.one.stone.soup.remote.control.server.DesktopControlAdapter;
import org.one.stone.soup.remote.control.server.RemoteControlHubConnection;
import org.one.stone.soup.remote.control.server.RemoteControlServer;
import org.one.stone.soup.remote.control.server.RemoteControlServerApplication;
import org.one.stone.soup.screen.recorder.DesktopScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;
import org.one.stone.soup.stringhelper.StringArrayHelper;
import org.one.stone.soup.xapp.XApplication;
import org.one.stone.soup.xapp.XappRootApplication;
import org.one.stone.soup.xapp.components.XappTextArea;
import org.one.stone.soup.xapp.components.form.XForm;
import org.one.stone.soup.xapp.filebrowser.XappFileBrowser;
import org.one.stone.soup.xml.XmlElement;
import org.one.stone.soup.xml.stream.XmlLoader;

public class XappJavaRemoteControlServer extends XApplication implements RemoteControlServerApplication{

	private String currentFile=null;
	private RemoteControlServer server;
	private RemoteControlHubConnection hubConnection;
	private boolean serverRunning = false;
	
	private static String APP_DEFINITION_PATH = "./org/one/stone/soup/xapp/application/rdc/server/";
	
	public static void main(String[] args)
	{
		if(args.length==1)
		{
			APP_DEFINITION_PATH = args[0];
		}
		try{
			new XappJavaRemoteControlServer();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null,ExceptionHelper.getStackTrace(e));
		}
	}	
	
	public XappJavaRemoteControlServer() throws Exception
	{
		super(APP_DEFINITION_PATH+"jrc-server.xapp");
		
		initialise();
		
		showMessage("RDC Ready");
	}
	
	public void processDropAction(String file, int posX, int posY) {
		XmlElement data = XmlLoader.load(file);
		setData("server-settings",data);
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

	public void start() throws Exception
	{
		if(serverRunning==true)
		{
			XappRootApplication.displayMessage("The server is already running");
			return;
		}
		String useHub = XappRootApplication.getAPI().getField("server-settings","useHub");	
		if(useHub.equals("true"))
		{
			initialiseHub();
		}
		else
		{
			initialiseServer();
		}
	}
	
	public void stop()
	{
		if(serverRunning==false)
		{
			XappRootApplication.displayMessage("The server is not running");
			return;
		}
		serverRunning=false;
		server.stop();
		showMessage("RCD Server Stopped");
	}
	
	public void disconnect() {
		showMessage("Disconnected");

		if(hubConnection!=null)
		{
			hubConnection.disconnect();
		}
		
		boolean autoAccept = new Boolean(XappRootApplication.getAPI().getField("server-settings","autoAccept")).booleanValue();	
		String useHub = XappRootApplication.getAPI().getField("server-settings","useHub");	
		if(useHub.equals("true") && serverRunning==true)
		{
			showMessage("Reconnecting to hub");
			try{ Thread.sleep(5000); }catch(Exception e){}
			
			initialiseHub();
			server.setAutoAccept(autoAccept);
			showMessage("Hub reconnected");
		}
	}

	public ControlAdapter getControlAdapter() {
		return new DesktopControlAdapter();
	}

	public Frame getRootFrame() {
		return (Frame)XappRootApplication.getRoot().getFrame();
	}

	public void setRequester(String requester) {
		showMessage("Connected to by "+requester);
	}

	public ScreenRecorder getScreenRecorder(OutputStream outputStream,ScreenRecorderListener listener)
	{
		return new DesktopScreenRecorder(outputStream,listener);
	}

	private void initialiseHub()
	{
		boolean autoAccept = new Boolean(XappRootApplication.getAPI().getField("server-settings","autoAccept")).booleanValue();	
		String alias = XappRootApplication.getAPI().getField("server-settings","userName");
		String password = XappRootApplication.getAPI().getField("server-settings","password");
		String address = XappRootApplication.getAPI().getField("server-settings","hubAddress");
		int port = Integer.parseInt( XappRootApplication.getAPI().getField("server-settings","hubPort") );
		boolean useTunnel = new Boolean(XappRootApplication.getAPI().getField("server-settings","useTunnel")).booleanValue();	
		String tunnelHost = XappRootApplication.getAPI().getField("server-settings","tunnelHost");	
		
		server = new RemoteControlServer(this);
		server.setAutoAccept(autoAccept);
		try{
			
			hubConnection = new RemoteControlHubConnection(alias,password,server);
			
			try{
				if(useTunnel==true)
				{
					hubConnection.connect(tunnelHost,address,port);
				}
				else
				{
					hubConnection.connect(address,port);					
				}
			}
			catch(IOException ioe)
			{
				XappRootApplication.displayMessage("Login failed");
				hubConnection = null;
				return;
			}
			
			
			showMessage("RCD Server Connected to Hub at "+address+":"+port);
			serverRunning=true;
		}
		catch(Exception e)
		{
			XappRootApplication.displayException(e);
		}
	}

	private void initialiseServer() throws Exception
	{
		boolean autoAccept = new Boolean(XappRootApplication.getAPI().getField("server-settings","autoAccept")).booleanValue();	
		String address = XappRootApplication.getAPI().getField("server-settings","serverAddress");
		int port = Integer.parseInt( XappRootApplication.getAPI().getField("server-settings","serverPort") );
		
		server = new RemoteControlServer(this);
		server.setAutoAccept(autoAccept);
		server.start("Remote Control Server",address,port,2,1000);
		
		showMessage("RCD Server Started on "+address+":"+port);
		serverRunning=true;
	}
}
