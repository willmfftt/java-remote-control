package org.one.stone.soup.screen.multicaster;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.one.stone.soup.screen.recorder.DesktopScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;
import org.one.stone.soup.swing.JRootFrame;
import org.one.stone.soup.xml.XmlElement;

public class JScreenShare extends JRootFrame implements ScreenRecorderListener{

	private ScreenRecorder recorder;

	private JLabel text;
	private int frameCount;
	
	private String address;
	private int port;
	private String page;
	private Socket socket; 
	
	public JScreenShare(String address,int port,String page)
	{
		super("Screen Share",new String[]{});
		
		this.address = address;
		this.port = port;
		this.page = page;
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,2));
		panel.setBackground(Color.black);
		
		JLabel frameLabel = new JLabel("Frame: 0 Time: 0");
		frameLabel.setBackground(Color.black);
		frameLabel.setForeground(Color.red);
		text=new JLabel("No recording selected");
		text.setBackground(Color.black);
		text.setForeground(Color.red);
		
		panel.add(text);
		panel.add(frameLabel);
		
		this.getContentPane().add( panel,BorderLayout.SOUTH );
		
		this.pack();
		this.setVisible(true);
		
		start();
	}

	public void start()
	{
		try{
			XmlElement header = new XmlElement("Recorder");
			Rectangle screen = new Rectangle( Toolkit.getDefaultToolkit ().getScreenSize() );
			
			socket = new Socket(address,80);
			socket.getOutputStream().write( ("GET "+page+" HTTP/1.1\r\n").getBytes() );
			socket.getOutputStream().write( ("\r\n\r\n").getBytes() );
			
			socket.getOutputStream().write(header.toXml().getBytes());
			socket.getOutputStream().flush();
			
			recorder = new DesktopScreenRecorder(socket.getOutputStream(),this,true);
			recorder.startRecording();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	public void frameRecorded(boolean fullFrame)
	{
		frameCount++;
		text.setText("Frame: "+frameCount);		
	}

	public void recordingStopped()
	{
		recorder = null;
		
		text.setText("Ready to record");
	}

	/* (non-Javadoc)
	 * @see wet.wired.swing.JRootFrame#destroy()
	 */
	public boolean destroy(Object source) {
		return true;
	}

	public static void main(String[] args)
	{
		JScreenShare recorder = new JScreenShare(args[0],Integer.parseInt(args[1]),args[2]);
	}
}
