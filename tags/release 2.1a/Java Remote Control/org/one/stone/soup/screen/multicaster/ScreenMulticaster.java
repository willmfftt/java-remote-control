package org.one.stone.soup.screen.multicaster;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.one.stone.soup.screen.recorder.FrameCompressor;
import org.one.stone.soup.screen.recorder.FrameDecompressor;

public class ScreenMulticaster implements Runnable{

	private String id;
	private ScreenMulticasterSessionListener listener;
	private int frameSize;
	private FrameDecompressor decompressor;
	private Hashtable compressors = new Hashtable();
	private OutputStream pingStream;
	
	public ScreenMulticaster(OutputStream pingStream,InputStream iStream,int frameSize,String id,ScreenMulticasterSessionListener listener)
	{
		this.id = id;
		this.listener = listener;
		this.pingStream = pingStream;
		this.frameSize = frameSize;
		decompressor = new FrameDecompressor( iStream,frameSize );
		
		new Thread(this,"Screen Multicaster").start();
	}
	
	public void addClient(String alias,OutputStream oStream)
	{
		compressors.put(alias,new FrameCompressor(oStream,frameSize));
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
				Enumeration keys = compressors.keys();
				while(keys.hasMoreElements())
				{
					String alias = (String)keys.nextElement();
					FrameCompressor compressor = (FrameCompressor)compressors.get(alias);
					
					try{
						compressor.pack( frame.getData(),frame.getTimeStamp(),false );
					}
					catch(Exception e)
					{
						e.printStackTrace();
						compressors.remove(alias);
					}
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
