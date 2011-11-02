package org.one.stone.soup.screen.recorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FrameCompressionAlgorithmV1 implements FrameCompressor,FrameDecompressor{
	private static final int ALPHA = 0xFF000000;
	
	public FrameCompressionAlgorithmV1(){}

	public void compress(FramePacket packet)
	{	
		int inCursor = 0;
		int outCursor = 0;
		int blocks = 0;

		boolean inBlock = true;
		int blockSize = 0;
		byte blockRed = 0;
		byte blockGreen = 0;
		byte blockBlue = 0;
		
		int blankBlocks = 0;
		
		int uncompressedCursor = -1;
		
		byte red;
		byte green;
		byte blue;
		
		boolean hasChanges = false;
		boolean lastEntry = false;
		
		int[] prevFrame = packet.getPreviousFrame();
		int[] currFrame = packet.getCurrentFrame();
		byte[] compFrame = packet.getCompressedFrame();
		
		int dataSize = packet.getDataSize();
		
		while(inCursor < dataSize)
		{
			if(inCursor == dataSize - 1)
			{
				lastEntry = true;
			}
			
			if(currFrame[inCursor] == prevFrame[inCursor])
			{
				red=0;
				green=0;
				blue=0;
			}
			else
			{
				red = (byte) ((currFrame[inCursor] & 0x00FF0000) >>> 16);
				green = (byte) ((currFrame[inCursor] & 0x0000FF00) >>> 8);
				blue = (byte) ((currFrame[inCursor] & 0x000000FF));
				
				if(red==0 && green==0 && blue==0)
				{
					blue = 1;
				}
			}
			
			if(
				blockRed == red &&
				blockGreen == green &&
				blockBlue == blue
			)
			{
				if(inBlock == false)
				{
					if(uncompressedCursor > -1)
					{
						blocks++;
						hasChanges = true;
						compFrame[uncompressedCursor] = (byte)(blockSize + 0x80);
					}
					inBlock = true;
					blockSize = 0;
					blankBlocks = 0;
				}
				else if(blockSize == 126 || lastEntry == true)
				{
					if (
						blockRed == 0 &&
						blockGreen == 0 &&
						blockBlue == 0
					)
					{
						if (blankBlocks > 0)
						{
							blankBlocks++;
							compFrame[outCursor-1] = (byte) blankBlocks;
						}
						else
						{
							blocks++;
							blankBlocks++;
							compFrame[outCursor] = (byte)0xFF;
							outCursor++;
							compFrame[outCursor] = (byte)blankBlocks;
							outCursor++;
						}
						if(blankBlocks == 255)
						{
							blankBlocks = 0;
						}
					}
					else
					{
						blocks++;
						hasChanges = true;
						compFrame[outCursor] = (byte)blockSize;
						outCursor++;
						compFrame[outCursor] = blockRed;
						outCursor++;
						compFrame[outCursor] = blockGreen;
						outCursor++;
						compFrame[outCursor] = blockBlue;
						outCursor++;
						
						blankBlocks = 0;
					}			
					inBlock = true;
					blockSize = 0;
				}
			}
			else
			{
				if(inBlock == true)
				{
					if(blockSize > 0)
					{
						blocks++;
						hasChanges = true;
						compFrame[outCursor] = (byte) blockSize;
						outCursor++;
						compFrame[outCursor] = blockRed;
						outCursor++;
						compFrame[outCursor] = blockGreen;
						outCursor++;
						compFrame[outCursor] = blockBlue;
						outCursor++;
					}

					uncompressedCursor = -1;
					inBlock = false;
					blockSize = 0;
					
					blankBlocks = 0;
				}
				else if(blockSize == 126 || lastEntry == true)
				{
					if(uncompressedCursor>-1)
					{
						blocks++;
						hasChanges = true;
						compFrame[uncompressedCursor] = (byte)(blockSize + 0x80);
					}
					
					uncompressedCursor = -1;
					inBlock = false;
					blockSize = 0;
					
					blankBlocks = 0;					
				}


				if(uncompressedCursor == -1)
				{
					uncompressedCursor = outCursor;
					outCursor++;
				}
				
				compFrame[outCursor] = red;
				outCursor++;
				compFrame[outCursor] = green;
				outCursor++;
				compFrame[outCursor] = blue;
				outCursor++;
				
				blockRed = red;
				blockGreen = green;
				blockBlue = blue;
			}
			inCursor++;
			blockSize++;
		}
		
		packet.setUnzBytesSize(outCursor);
		try{
			ByteArrayOutputStream bO = new ByteArrayOutputStream();
			GZIPOutputStream zO = new GZIPOutputStream(bO);
			zO.write(compFrame,0,outCursor);
			zO.close();
			bO.close();
			packet.setCompressedFrame(bO.toByteArray());
			packet.setCompressedFrameSize(bO.size());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void decompress(FramePacket packet) {
		
		int[] prevFrame = packet.getPreviousFrame();
		int[] currFrame = packet.getCurrentFrame();
		int currFrameSize = currFrame.length;
		
		byte[] compFrame = packet.getCompressedFrame();
		int compFrameSize = 0;
		
		byte[] zData = new byte[packet.getCompressedFrameSize()];
		System.arraycopy(compFrame, 0, zData, 0, zData.length);
		
		try{
			ByteArrayInputStream biStream = new ByteArrayInputStream( zData,0,zData.length );
			GZIPInputStream gzipInputStream = new GZIPInputStream( biStream );
			
			int sizeRead = gzipInputStream.read(compFrame);
			int cursor=sizeRead;

			while(sizeRead>-1)
			{
				sizeRead = gzipInputStream.read(compFrame,cursor,10000);
				cursor += sizeRead;
			}
			
			compFrameSize = cursor;
			packet.setCompressedFrameSize(compFrameSize);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		int inCursor = 0;
		int outCursor = 0;
		
		int blockSize = 0;
		
		int rgb = 0xFF000000;
		
		//System.out.println("Combining old:"+frame.previousData+" with new:"+frame.newData);
		
		while(inCursor < compFrameSize && outCursor < currFrameSize)
		{
			if(compFrame[inCursor] == -1)
			{
				inCursor++;
				
				int count = (compFrame[inCursor] & 0xFF);
				inCursor++;
				
				int size = count*126;
				if( size > currFrameSize)
				{
					size = currFrameSize;
				}
				//System.arraycopy(frame.previousData,0,frame.newData,0,size);
				//outCursor+=size;
				
				for(int loop=0; loop < (126 * count); loop++)
				{
					//frame.newData[outCursor]=blue;//frame.previousData[outCursor];
					currFrame[outCursor] = prevFrame[outCursor];
					//newRawData[outCursor]=blue;
					outCursor++;
					if(outCursor == currFrameSize)
					{
						break;
					}
				}
				
			}
			else if (compFrame[inCursor] < 0) // uncomp
			{
				blockSize = compFrame[inCursor] & 0x7F;//(128+packed[inCursor]);
				inCursor++;
				
				for(int loop=0; loop < blockSize; loop++)
				{
					rgb = ((compFrame[inCursor] & 0xFF)<<16) 
						| ((compFrame[inCursor+1] & 0xFF) << 8)
						| (compFrame[inCursor+2] & 0xFF)
						| ALPHA;
					
					if(rgb == ALPHA)
					{
						rgb = prevFrame[outCursor];
					}
					//rgb = green;
					inCursor += 3;
					currFrame[outCursor] = rgb;
					outCursor++;
					if (outCursor == currFrame.length)
					{
						break;
					}
				}
			}
			else
			{
				blockSize = compFrame[inCursor];
				inCursor++;
				rgb = ((compFrame[inCursor] & 0xFF) << 16)
					| ((packet.getCompressedFrame()[inCursor+1] & 0xFF) << 8)
					| (packet.getCompressedFrame()[inCursor+2] & 0xFF)
					| ALPHA;
				
				boolean transparent = false;
				if(rgb == ALPHA)
				{
					transparent = true;
				}
				//rgb = red;
				inCursor += 3;
				
				for (int loop = 0; loop < blockSize; loop++)
				{
					if (transparent)
					{
						currFrame[outCursor] = prevFrame[outCursor];						
					}
					else
					{
						currFrame[outCursor] = rgb;	
					}
					outCursor++;
					if (outCursor == currFrameSize)
					{
						break;
					}
				}
			}
		}
		
		packet.setCompressedFrameSize(outCursor);
		packet.prepareFrame();
	}	
}
