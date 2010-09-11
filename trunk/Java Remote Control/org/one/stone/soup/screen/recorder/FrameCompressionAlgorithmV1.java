package org.one.stone.soup.screen.recorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FrameCompressionAlgorithmV1 implements FrameCompressor,FrameDecompressor{
	private static final int ALPHA = 0xFF000000;
	
	public FrameCompressionAlgorithmV1(){}
	@Override
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
		
		while(inCursor<packet.getDataSize())
		{
			if(inCursor==packet.getDataSize()-1)
			{
				lastEntry = true;
			}
			
			if(packet.getCurrentFrame()[inCursor]==packet.getPreviousFrame()[inCursor])
			{
				red=0;
				green=0;
				blue=0;
			}
			else
			{
				red = (byte) ((packet.getCurrentFrame()[inCursor] & 0x00FF0000) >>> 16);
				green = (byte) ((packet.getCurrentFrame()[inCursor] & 0x0000FF00) >>> 8);
				blue = (byte) ((packet.getCurrentFrame()[inCursor] & 0x000000FF));
				
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
				if(inBlock==false)
				{
					if(uncompressedCursor>-1)
					{
						blocks++;
						hasChanges=true;
						packet.getCompressedFrame()[uncompressedCursor] = (byte)(blockSize + 0x80);
					}
					inBlock=true;
					blockSize = 0;
					blankBlocks = 0;
				}
				else if(blockSize==126 || lastEntry==true)
				{
					if(
						blockRed==0 &&
						blockGreen==0 &&
						blockBlue==0
					)
					{
						if(blankBlocks>0)
						{
							blankBlocks++;
							packet.getCompressedFrame()[outCursor-1] = (byte)blankBlocks;
						}
						else
						{
							blocks++;
							blankBlocks++;
							packet.getCompressedFrame()[outCursor] = (byte)0xFF;
							outCursor++;
							packet.getCompressedFrame()[outCursor] = (byte)blankBlocks;
							outCursor++;
						}
						if(blankBlocks==255)
						{
							blankBlocks = 0;
						}
					}
					else
					{
						blocks++;
						hasChanges=true;
						packet.getCompressedFrame()[outCursor] = (byte)blockSize;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockRed;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockGreen;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockBlue;
						outCursor++;
						
						blankBlocks = 0;
					}			
					inBlock=true;
					blockSize = 0;
				}
			}
			else
			{
				if(inBlock==true)
				{
					if(blockSize>0)
					{
						blocks++;
						hasChanges=true;
						packet.getCompressedFrame()[outCursor] = (byte)blockSize;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockRed;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockGreen;
						outCursor++;
						packet.getCompressedFrame()[outCursor] = blockBlue;
						outCursor++;
					}

					uncompressedCursor = -1;
					inBlock=false;
					blockSize = 0;
					
					blankBlocks = 0;
				}
				else if(blockSize==126 || lastEntry==true)
				{
					if(uncompressedCursor>-1)
					{
						blocks++;
						hasChanges=true;
						packet.getCompressedFrame()[uncompressedCursor] = (byte)(blockSize + 0x80);
					}
					
					uncompressedCursor = -1;
					inBlock=false;
					blockSize=0;
					
					blankBlocks = 0;					
				}


				if(uncompressedCursor == -1)
				{
					uncompressedCursor = outCursor;
					outCursor++;
				}
				
				packet.getCompressedFrame()[outCursor] = red;
				outCursor++;
				packet.getCompressedFrame()[outCursor] = green;
				outCursor++;
				packet.getCompressedFrame()[outCursor] = blue;
				outCursor++;
				
				blockRed = red;
				blockGreen = green;
				blockBlue = blue;
			}
			inCursor++;
			blockSize++;
		}
		
		try{
			ByteArrayOutputStream bO = new ByteArrayOutputStream();
			GZIPOutputStream zO = new GZIPOutputStream(bO);
			zO.write(packet.getCompressedFrame(),0,outCursor);
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
		
		byte[] zData = new byte[packet.getCompressedFrameSize()];
		System.arraycopy(packet.getCompressedFrame(), 0, zData, 0, zData.length);
		
		try{
			ByteArrayInputStream biStream = new ByteArrayInputStream( zData,0,zData.length );
			GZIPInputStream gzipInputStream = new GZIPInputStream( biStream );
			
			int sizeRead = gzipInputStream.read(packet.getCompressedFrame());
			int cursor=sizeRead;

			while(sizeRead>-1)
			{
				sizeRead = gzipInputStream.read(packet.getCompressedFrame(),cursor,10000);
				cursor += sizeRead;
			}
			
			packet.setCompressedFrameSize( cursor );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		int inCursor = 0;
		int outCursor = 0;
		
		int blockSize = 0;
		
		int rgb = 0xFF000000;
		
		//System.out.println("Combineing old:"+frame.previousData+" with new:"+frame.newData);
		
		while(inCursor<packet.getCompressedFrameSize() && outCursor<packet.getCurrentFrame().length)
		{
			if(packet.getCompressedFrame()[inCursor]==-1)
			{
				inCursor++;
				
				int count = (packet.getCompressedFrame()[inCursor] & 0xFF);
				inCursor++;
				
				int size = count*126;
				if(size>packet.getCurrentFrame().length)
				{
					size = packet.getCurrentFrame().length;
				}
				//System.arraycopy(frame.previousData,0,frame.newData,0,size);
				//outCursor+=size;
				
				for(int loop=0;loop<(126*count);loop++)
				{
					//frame.newData[outCursor]=blue;//frame.previousData[outCursor];
					packet.getCurrentFrame()[outCursor]=packet.getPreviousFrame()[outCursor];
					//newRawData[outCursor]=blue;
					outCursor++;
					if(outCursor==packet.getCurrentFrame().length)
					{
						break;
					}
				}
				
			}
			else if(packet.getCompressedFrame()[inCursor]<0) // uncomp
			{
				blockSize = packet.getCompressedFrame()[inCursor] & 0x7F;//(128+packed[inCursor]);
				inCursor++;
				
				for(int loop=0;loop<blockSize;loop++)
				{
					rgb = ((packet.getCompressedFrame()[inCursor] & 0xFF)<<16) | ((packet.getCompressedFrame()[inCursor+1] & 0xFF)<<8) | (packet.getCompressedFrame()[inCursor+2] & 0xFF) | ALPHA;
					if(rgb==ALPHA)
					{
						rgb=packet.getPreviousFrame()[outCursor];
					}
					//rgb = green;
					inCursor+=3;
					packet.getCurrentFrame()[outCursor]=rgb;
					outCursor++;
					if(outCursor==packet.getCurrentFrame().length)
					{
						break;
					}
				}
			}
			else
			{
				blockSize = packet.getCompressedFrame()[inCursor];
				inCursor++;
				rgb = ((packet.getCompressedFrame()[inCursor] & 0xFF)<<16) | ((packet.getCompressedFrame()[inCursor+1] & 0xFF)<<8) | (packet.getCompressedFrame()[inCursor+2] & 0xFF) | ALPHA;
				
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
						packet.getCurrentFrame()[outCursor]=packet.getPreviousFrame()[outCursor];						
					}
					else
					{
						packet.getCurrentFrame()[outCursor]=rgb;	
					}
					outCursor++;
					if(outCursor==packet.getCurrentFrame().length)
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
