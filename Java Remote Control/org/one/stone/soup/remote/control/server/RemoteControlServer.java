/*
 * Created on 28-Aug-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.one.stone.soup.remote.control.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JOptionPane;

import org.one.stone.soup.io.Connection;
import org.one.stone.soup.net.SocketConnection;
import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;
import org.one.stone.soup.server.PlainServer;
import org.one.stone.soup.server.RouterThread;
import org.one.stone.soup.stringhelper.StringGenerator;

/**
 * @author Nicholas Cross
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RemoteControlServer extends PlainServer implements ScreenRecorderListener {
	
	private String connectionId;
	private String key;
	private String requester;
	private boolean hasConnection = false;
	private boolean autoAccept = false;
	private boolean readyForFrame = false;
	
	private int framesInProgress = 0;
	
	private ScreenRecorder screenRecorder;
	private RemoteControlServerApplication owner;
	private ControlConnection controlConnection;
		
	public RemoteControlServer(RemoteControlServerApplication owner)
	{
		this.owner = owner;
	}
	
	public void setAutoAccept(boolean state)
	{
		autoAccept = state;
	}
	
	/* (non-Javadoc)
	 * @see wet.wired.server.Server#route(java.net.Socket, wet.wired.server.RouterThread)
	 */
	public void route(Socket socket, RouterThread routerThread) {
		
		SocketConnection connection = new SocketConnection( socket,socket.getInetAddress().getHostAddress() );
		route( connection,routerThread );
	}

	public void route(Connection connection, RouterThread routerThread) {
		try{
					
			if(hasConnection)
			{
				System.out.println("Connection Requested for "+connection.getAlias());

				if(connection.getAlias().equals(connectionId))
				{
					String testKey = readLine(connection.getInputStream());
					
					if(!testKey.toString().equals(key))
					{
						try
						{
							connection.close();
						} catch(Exception e){}
						return;
					}
					System.out.println("Request Key Passed");
					
					String connectionType = readLine(connection.getInputStream());
					
					if(routerThread!=null)
					{
						routerThread.stopTimer();
					}
					System.out.println("Connection Requested Type "+connectionType);
					
					openConnection( connectionType.toString(),connection,requester );

					System.out.println("Connection created. Type "+connectionType);
				}
			}
			else
			{
				InputStream iStream = connection.getInputStream();
					
				requester = readLine(iStream);
				
				if( autoAccept==false && JOptionPane.showConfirmDialog(owner.getRootFrame(),"Accept Connection from "+requester)!=JOptionPane.OK_OPTION )
				{
					try
					{
						connection.close();
					} catch(Exception e){}
					return;
				}
				else
				{
					key = StringGenerator.generateUniqueId();
					connection.getOutputStream().write( (key+'\n').getBytes() );
					connection.getOutputStream().flush();
					
					owner.setRequester( requester );			
				}
	
				//int identifier = socket.getInputStream().read();
				hasConnection = true;
				connectionId = connection.getAlias();
				
				if(routerThread!=null)
				{
					routerThread.stopTimer();
				}
				
				screenRecorder = owner.getScreenRecorder(connection.getOutputStream(),this);
				
				//frameRecorded(); //wait for client frame request
				
				screenRecorder.startRecording();

				System.out.println("Screen Recorder Started for "+connectionId);				
			}
		}
		catch(Exception e)
		{
		}
		finally
		{
		}
	}
	
	/* (non-Javadoc)
	 * @see wet.wired.server.Server#initialize()
	 */
	public void initialize() {	}

	private void openConnection(String command,Connection connection,String requester)
	{
		if(command.equals("fullControl"))
		{
			try{
				openControlConnection(connection.getOutputStream(),connection.getInputStream(),requester,true);
			}
			catch(Exception e){}
		}
		else
		{
			try{
				openControlConnection(connection.getOutputStream(),connection.getInputStream(),requester,false);
			}
			catch(Exception e){}
		}
	}
	
/*	private void openConnection(String command,Socket socket,String requester)
	{
		if(command.equals("fullControl"))
		{
			try{
				openControlConnection(socket.getOutputStream(),socket.getInputStream(),requester,true);
			}
			catch(Exception e){}
		}
		else
		{
			try{
				openControlConnection(socket.getOutputStream(),socket.getInputStream(),requester,false);
			}
			catch(Exception e){}
		}
	}*/
	
	private void openControlConnection(OutputStream oStream,InputStream iStream,String requester,boolean fullControl)
	{
		if( autoAccept==false && fullControl==true)
		{
			if(JOptionPane.showConfirmDialog(owner.getRootFrame(),"Accept Control by "+requester)!=JOptionPane.OK_OPTION )
			{
				fullControl=false;
			}
		}
		try
		{
			controlConnection = new ControlConnection(this,oStream,iStream,fullControl,owner.getControlAdapter());
			controlConnection.process();
		} catch(Exception e){}
	}
	
	public void stop()
	{
		super.stop();
		disconnect();
	}
	
	public void disconnect()
	{
		if(screenRecorder!=null)
		{
			screenRecorder.stopRecording();
		}
		screenRecorder = null;

		setReadyForFrame();
		
		if(controlConnection!=null)
		{
			controlConnection.stop();
			controlConnection=null;
		}
					
		hasConnection=false;
		System.out.println("Disconnected");

		owner.disconnect();
	}
	
	public void setReadyForFrame()
	{
		readyForFrame=true;
		framesInProgress--;
	}

	public void sendKeyFrame()
	{
		screenRecorder.sendKeyFrame();
	}
	
	public void frameRecorded(boolean fullFrame) throws IOException
	{	
		int count=0;
		while( controlConnection==null && count<500)
		{
			try{ Thread.sleep(10); }catch(Exception e){}
			count++;
		}
		if(count==500)
		{
			throw new IOException( "Frame timeout" );
		}
		
		try{
			controlConnection.sendFrameMarker(fullFrame);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			disconnect();
			return;
		}
		
		framesInProgress++;
		count=0;
		while(framesInProgress>2 && screenRecorder.isRecording() && count<500)
		{
			try{ Thread.sleep(10); }catch(Exception e){}
			count++;
		}
		if(count==500)
		{
			throw new IOException( "Frame timeout" );
		}
		
		readyForFrame=false;
	}

	public void recordingStopped() {
		disconnect();
	}

	public static String readLine(InputStream iStream) throws IOException
	{
		int in = iStream.read();
		StringBuffer buffer = new StringBuffer();
		while(in!=-1 && in!='\n' && buffer.length()<100)
		{
			if(in!=0) // keep alive byte
			{
				buffer.append((char)in);
			}
						
			in = iStream.read();
		}
		
		return buffer.toString();
	}
	
	public static void writeLine(String line,OutputStream outputStream) throws IOException
	{
		outputStream.write( (line+"\n").getBytes() );
		outputStream.flush();				
	}
}
