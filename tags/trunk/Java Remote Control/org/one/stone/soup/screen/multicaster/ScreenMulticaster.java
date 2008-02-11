package org.one.stone.soup.screen.multicaster;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import org.one.stone.soup.screen.recorder.FrameCompressor;
import org.one.stone.soup.screen.recorder.FrameDecompressor;

public class ScreenMulticaster implements Runnable{

	private String id;
	private ScreenMulticasterSessionListener listener;
	private int frameSize;
	private FrameDecompressor decompressor;
	private Hashtable clients = new Hashtable();
	private OutputStream pingStream;
	private Socket socket;
	
	public class MulticasterClient
	{
		private Socket clientSocket;
		private OutputStream outputStream;
		private FrameCompressor compressor;
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
			
			this.compressor = new FrameCompressor(outputStream,frameSize);
		}
		
		public FrameCompressor getCompressor()
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
		InputStream iStream = socket.getInputStream();
		
		this.id = id;
		this.listener = listener;
		this.frameSize = frameSize;
		decompressor = new FrameDecompressor( iStream,frameSize );
		
		new Thread(this,"Screen Multicaster").start();
	}
	
	public void addClient(String alias,Socket socket) throws IOException
	{
		clients.put(alias,new MulticasterClient(socket));
	}
	
	public void run()
	{
		try{
			while(true)
			{
				FrameDecompressor.FramePacket frame = decompressor.unpack();
				pingStream.write(1);
				pingStream.flush();
				
				if(frame.getData()==null)
				{
					continue;
				}
				Enumeration keys = clients.keys();
				while(keys.hasMoreElements())
				{
					String alias = (String)keys.nextElement();
					MulticasterClient client = (MulticasterClient)clients.get(alias);
					FrameCompressor compressor = (FrameCompressor)(client).getCompressor();
					
					try{
						compressor.pack( frame.getData(),frame.getTimeStamp(),false );
					}
					catch(Exception e)
					{
						e.printStackTrace();
						clients.remove(alias);
					}
					
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
