package org.one.stone.soup.screen.multicaster;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.one.stone.soup.screen.recorder.FrameCompressor;
import org.one.stone.soup.screen.recorder.FrameDecompressor;

public class ScreenMulticaster implements Runnable{

	private int frameSize;
	private FrameDecompressor decompressor;
	private Hashtable compressors = new Hashtable();
	
	public ScreenMulticaster(InputStream iStream,int frameSize)
	{
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
				
				Enumeration keys = compressors.keys();
				while(keys.hasMoreElements())
				{
					String alias = (String)keys.nextElement();
					FrameCompressor compressor = (FrameCompressor)compressors.get(alias);
					
					try{
						compressor.pack( frame.getData(),frame.getTimeStamp() );
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
	}
}
