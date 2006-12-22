package org.one.stone.soup.screen.recorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class FrameDecompressor {
	private static final int ALPHA = 0xFF000000;

	public class FramePacket
	{
		private FramePacket( InputStream iStream,int expectedSize )
		{
			this.frameSize = expectedSize;
			this.iStream = iStream;
			previousData = new int[frameSize];
			previousData = new int[frameSize];
		}		
		
		private void nextFrame()
		{
			if(newData!=null)
			{
				previousData = newData;
				newData = null;
			}
		}
		
		private InputStream iStream;
		private int[] previousData;
		private int result;
		private long frameTimeStamp;
		private byte[] packed;
		private int frameSize;
		private int[] newData;
		
		public int[] getData()
		{
			return newData;
		}
		
		public int getResult()
		{
			return result;
		}
		
		public long getTimeStamp()
		{
			return frameTimeStamp;
		}
	}

	public FramePacket frame;
	
	public FrameDecompressor(InputStream iStream,int frameSize)
	{
		frame = new FramePacket( iStream,frameSize );
	}
	
	public FramePacket unpack() throws IOException
	{
		frame.nextFrame();
		
		//try{
			int time = frame.iStream.read();
			time = time << 8;
			time += frame.iStream.read();
			time = time << 8;
			time += frame.iStream.read();
			time = time << 8;
			time += frame.iStream.read();
			
			frame.frameTimeStamp = (long)time;
			//System.out.println("ft:"+frameTime);
			
			byte type = (byte)frame.iStream.read();
			//System.out.println("Packed Code:"+type);
			
			if(type<=0)
			{
				frame.result = type;
				return frame;
			}
		/*}
		catch(Exception e)
		{
			e.printStackTrace();
		}*/		

		ByteArrayOutputStream bO = new ByteArrayOutputStream();		
		try{
			int zSize = frame.iStream.read();
			zSize = zSize << 8;
			zSize += frame.iStream.read();
			zSize = zSize << 8;
			zSize += frame.iStream.read();
			zSize = zSize << 8;
			zSize += frame.iStream.read();

			byte[] zData = new byte[zSize];
			int readCursor = 0;
			int sizeRead = 0;
			
			while(sizeRead>-1)
			{
				readCursor+=sizeRead;
				if(readCursor>=zSize)
				{
					break;
				}
				
				sizeRead = frame.iStream.read(zData,readCursor,zSize-readCursor);			
			}

			ByteArrayInputStream bI = new ByteArrayInputStream(zData);		
			
			//ZipInputStream zI = new ZipInputStream(bI);
			//zI.getNextEntry();

			GZIPInputStream zI = new GZIPInputStream(bI);

			byte[] buffer = new byte[1000];
			sizeRead = zI.read(buffer);
			
			while(sizeRead>-1)
			{
				bO.write(buffer,0,sizeRead);
				bO.flush();
				
				sizeRead = zI.read(buffer);
			}
			bO.flush();
			bO.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			frame.result=0;
			return frame;
		}
		
		frame.packed = bO.toByteArray();
		//System.out.println("UnZipped To: "+packed.length);
		
		runLengthDecode(frame);
		
		return frame;
}

private void runLengthDecode(FramePacket data)
{				
		data.newData = new int[data.frameSize];
		
		int inCursor = 0;
		int outCursor = 0;
		
		int blockSize = 0;
		
		int rgb = 0xFF000000;
		
		while(inCursor<data.packed.length && outCursor<data.frameSize)
		{
			if(data.packed[inCursor]==-1)
			{
				inCursor++;
				
				int count = (data.packed[inCursor] & 0xFF);
				inCursor++;
				
				int size = count*126;
				if(size>data.newData.length)
				{
					size = data.newData.length;
				}
				System.arraycopy(data.previousData,0,data.newData,0,size);
				outCursor+=size;
				
				/*for(int loop=0;loop<(126*count);loop++)
				{
					data.newData[outCursor]=data.previousData[outCursor];
					//newRawData[outCursor]=blue;
					outCursor++;
					if(outCursor==data.newData.length)
					{
						break;
					}
				}*/
				
			}
			else if(data.packed[inCursor]<0) // uncomp
			{
				blockSize = data.packed[inCursor] & 0x7F;//(128+packed[inCursor]);
				inCursor++;
				
				for(int loop=0;loop<blockSize;loop++)
				{
					rgb = ((data.packed[inCursor] & 0xFF)<<16) | ((data.packed[inCursor+1] & 0xFF)<<8) | (data.packed[inCursor+2] & 0xFF) | ALPHA;
					if(rgb==ALPHA)
					{
						rgb=data.previousData[outCursor];
					}
					//rgb = green;
					inCursor+=3;
					data.newData[outCursor]=rgb;
					outCursor++;
					if(outCursor==data.newData.length)
					{
						break;
					}
				}
			}
			else
			{
				blockSize = data.packed[inCursor];
				inCursor++;
				rgb = ((data.packed[inCursor] & 0xFF)<<16) | ((data.packed[inCursor+1] & 0xFF)<<8) | (data.packed[inCursor+2] & 0xFF) | ALPHA;
				
				boolean transparent = false;
				if(rgb==ALPHA)
				{
					transparent = true;
				}
				//rgb = red;
				inCursor+=3;
				
				for(int loop=0;loop<blockSize;loop++)
				{
					if(transparent)
					{
						data.newData[outCursor]=data.previousData[outCursor];						
					}
					else
					{
						data.newData[outCursor]=rgb;	
					}
					outCursor++;
					if(outCursor==data.newData.length)
					{
						break;
					}
				}
			}
		}
		
		data.result = outCursor;
	}
	
}
