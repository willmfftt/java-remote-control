/*
 * Created on 25-Oct-05
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.one.stone.soup.remote.control.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.one.stone.soup.xml.XmlElement;
import org.one.stone.soup.xml.XmlParser;
import org.one.stone.soup.xml.stream.XmlLoader;

/**
 * @author Nik Cross
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ControlConnection implements Runnable{

	private OutputStream oStream;
	private InputStream iStream;
	private Thread thread;
	private boolean running = false;
	private boolean fullControl = false;
	private RemoteControlServer server;
	private ControlAdapter adapter;

	public ControlConnection(RemoteControlServer server,OutputStream oStream,InputStream iStream,boolean fullControl,ControlAdapter adapter)
	{
		this.adapter = adapter;
		this.fullControl = fullControl;
		this.server = server;
		this.iStream = iStream;
		this.oStream = oStream;
	}
	
	public void process()
	{
		thread = new Thread(this,"Control Thread");
		thread.start();
	}
	
	public void run()
	{
		try{
			XmlParser parser = XmlLoader.getStandardParser();
			XmlElement packet = null;
			
			running = true;
			
			while(running)
			{
				packet = parser.parseElement(iStream);
				
				if(packet==null)
				{
					break;
				}
				//System.out.println("Request:"+packet.toXml());
				
				if(packet.getName().equals("nextFrame"))
				{
					server.setReadyForFrame();
				}
				else if(packet.getName().equals("sendKeyFrame"))
				{
					server.sendKeyFrame();
				}
				else if(fullControl) 
				{
					if(packet.getName().equals("keyPressed"))
					{
						adapter.keyPress( Integer.parseInt(packet.getAttributeValueByName("code")) );
					}
					else if(packet.getName().equals("keyReleased"))
					{
						adapter.keyRelease( Integer.parseInt(packet.getAttributeValueByName("code")) );
					}
					else if(packet.getName().equals("mouseMoved"))
					{
						adapter.mouseMove( Integer.parseInt(packet.getAttributeValueByName("x")),Integer.parseInt(packet.getAttributeValueByName("y")));
					}
					else if(packet.getName().equals("mousePressed"))
					{
						adapter.mousePress( Integer.parseInt(packet.getAttributeValueByName("code")) );
					}
					else if(packet.getName().equals("mouseReleased"))
					{
						adapter.mouseRelease( Integer.parseInt(packet.getAttributeValueByName("code")) );
					}
				}
			}
			
			running = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		try{ iStream.close(); }catch(Exception e){}
		server.disconnect();
	}
	
	public void sendFrameMarker(boolean fullFrame) throws IOException
	{
		if(running==false)
		{
			return;
		}
		if(fullFrame)
		{
			oStream.write( "<fullFrame/>".getBytes() );
		}
		else
		{
			oStream.write( "<frame/>".getBytes() );
		}
		oStream.flush();
	}
	
	public void stop()
	{
		try{
			running = false;
			iStream.close();
		}
		catch(Exception e){}
		
		/*try{
			thread.stop();
		}
		catch(Exception e)
		{}*/
	}
}
