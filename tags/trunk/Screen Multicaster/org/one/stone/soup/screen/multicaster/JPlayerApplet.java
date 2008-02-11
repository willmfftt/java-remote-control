package org.one.stone.soup.screen.multicaster;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.InputStream;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.one.stone.soup.screen.recorder.ScreenPlayer;
import org.one.stone.soup.screen.recorder.ScreenPlayerListener;
import org.one.stone.soup.stringhelper.StringGenerator;
import org.one.stone.soup.xml.XmlElement;

public class JPlayerApplet extends JApplet implements ScreenPlayerListener{

	private ScreenPlayer player;
	
	private ImageIcon icon;
	
	private JLabel text;
	private JLabel frameLabel;
	
	private Socket socket;
	private int frameCount;
	private long startTime;
	
	public JPlayerApplet()
	{	
		System.out.println("Build 1.0");

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,2));
		panel.setBackground(Color.black);
		
		frameLabel = new JLabel("Frame: 0 Time: 0");
		frameLabel.setBackground(Color.black);
		frameLabel.setForeground(Color.red);
		text=new JLabel("Not Connected");
		text.setBackground(Color.black);
		text.setForeground(Color.red);
		
		panel.add(text);
		panel.add(frameLabel);
		
		this.getContentPane().add( panel,BorderLayout.SOUTH );
	}

	public void init()
	{
		String address = getParameter("address");
		if(address==null)
		{
			address=this.getCodeBase().getHost();
		}
		String port = getParameter("port");
		if(port==null)
		{
			port = "80";
		}
		String page = getParameter("page");
		if(page==null)
		{
			page = "/OpenForum/ScreenShare/Play";
		}
		String id = getParameter("id");
		if(id==null)
		{
			id = JOptionPane.showInputDialog(this,"Enter the WebCast ID");
		}
		if(id==null)
		{
			return;
		}
		
		try{	
			XmlElement header = new XmlElement("Player");
			header.addAttribute("id",id);
			header.addChild("alias").addValue( StringGenerator.generateUniqueId() );
			
			System.out.println("Opening "+address+":"+port+" page:"+page);
			
			socket = new Socket(address,80);
			socket.getOutputStream().write( ("GET "+page+" HTTP/1.1\r\n").getBytes() );
			socket.getOutputStream().write( ("\r\n\r\n").getBytes() );
			
			socket.getOutputStream().write(header.toXml().getBytes());
			socket.getOutputStream().flush();
			
			InputStream iStream = socket.getInputStream();
			player = new ScreenPlayer(iStream,this);
			player.setRealtime(true);
			frameCount=0;
			player.play();
			startTime = System.currentTimeMillis();

			text.setText("Connected to "+address+":"+port);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	public void playerStopped()
	{
		text.setText("Not Connected");	
		player=null;		
	}

	public void showNewImage(Image image)
	{
		if(icon==null)
		{
			icon = new ImageIcon(image);
			JLabel label = new JLabel(icon);
			
			JScrollPane scrollPane = new JScrollPane(label);
			scrollPane.setSize(image.getWidth(this),image.getHeight(this));
			
			this.getContentPane().add(scrollPane,BorderLayout.CENTER);
			
			setVisible(true);
		}
		else
		{
			icon.setImage(image);
		}
				
		repaint(0);
	}

	public void newFrame()
	{
		if(frameCount==0)
		{
			player.pause();
		}
		
		frameCount++;
		long time = System.currentTimeMillis()-startTime;
		String seconds = ""+time/1000;
		String milliseconds = ""+time%1000;
		milliseconds = StringGenerator.pad(milliseconds,4,'0')+milliseconds;
		frameLabel.setText("Frame:"+frameCount+" Time:"+seconds+"."+milliseconds);		
	}
	
}
