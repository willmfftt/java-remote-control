package org.one.stone.soup.screen.multicaster;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import org.one.stone.soup.screen.recorder.FrameCompressionAlgorithmV1;
import org.one.stone.soup.screen.recorder.FrameCompressionAlgorithmV2;
import org.one.stone.soup.screen.recorder.FrameDecompressor;
import org.one.stone.soup.screen.recorder.FramePacket;

public class ScreenMulticaster implements Runnable{

	private String id;
	private ScreenMulticasterSessionListener listener;
	private int frameSize;
	
	private FrameDecompressor decompressor;
	private FramePacket packet;
	private InputStream iStream;
	
	private Hashtable clients = new Hashtable();
	private OutputStream pingStream;
	private Socket socket;
	
	public class MulticasterClient
	{
		private Socket clientSocket;
		private OutputStream outputStream;
		private FrameCompressionAlgorithmV1 compressor;
		private Timer timeout;
		
		public class TimedOut extends TimerTask
		{
			public TimedOut()
			{
			}
			
			public void run()
			{
				try{
					outputStream.close();
					clientSocket.close();
				}
				catch(Exception e){}
			}
		}
		
		public MulticasterClient(Socket socket) throws IOException
		{
			this.clientSocket = socket;
			this.outputStream = clientSocket.getOutputStream();
			timeout = new Timer("Timeout Timer for "+socket);
			timeout.schedule(new TimedOut(),3000);
			
			this.compressor = new FrameCompressionAlgorithmV1();
		}
		
		public FrameCompressionAlgorithmV1 getCompressor()
		{
			return compressor;
		}
		
		public void resetTimeout()
		{
			timeout.cancel();
			timeout = new Timer("Timeout Timer for "+socket);
			timeout.schedule(new TimedOut(),3000);			
		}
	}
	
	public ScreenMulticaster(Socket socket,int frameSize,String id,ScreenMulticasterSessionListener listener) throws IOException
	{
		this.socket = socket;
		pingStream = socket.getOutputStream();
		iStream = socket.getInputStream();
		
		this.id = id;
		this.listener = listener;
		this.frameSize = frameSize;
		decompressor = new FrameCompressionAlgorithmV2();
		
		new Thread(this,"Screen Multicaster").start();
	}
	
	public void addClient(String alias,Socket socket) throws IOException
	{
		clients.put(alias,new MulticasterClient(socket));
	}
	
	public void run()
	{
		try{
			packet = new FramePacket(frameSize);
			while(true)
			{
				int time = packet.read(iStream);
				decompressor.decompress(packet);
				pingStream.write(1);
				pingStream.flush();
				
				Enumeration keys = clients.keys();
				while(keys.hasMoreElements())
				{
					String alias = (String)keys.nextElement();
					MulticasterClient client = (MulticasterClient)clients.get(alias);
					
					packet.write( client.outputStream,time );
					
					client.resetTimeout();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		listener.sessionClosed(id,this);
	}
}
