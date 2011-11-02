package org.one.stone.soup.screen.recorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FrameCompressionAlgorithmV3 implements FrameCompressor,FrameDecompressor{

	private final byte ALPHA_B = (byte) 0x80;
	private final int ALPHA_I = 0xFF000000;
	private final byte UNIQ_B = (byte) 0x80;
	private final int UNIQ_I = 0x8000000;
	
	private final byte BLK_MASK_B = (byte) 0xE0; 
	private final int BLK_MASK_I = 0xE0000000;
	
	private final byte BLK_TYPE_MASK_B = (byte) 0x40;
	private final int BLK_TYPE_MASK_I = 0x40000000;
	private final byte BLK_SIZE_MASK_B = (byte) 0x20;
	private final int BLK_SIZE_MASK_I = 0x20000000;
	private final byte BLK_U_B = (byte) 0x00;
	private final byte BLK_C_B = (byte) 0x40;
	
	private final byte BLK_US_B = (byte) 0x00;
	private final int  BLK_US_I = 0x00000000;
	private final byte BLK_UL_B = (byte) 0x20;
	private final int  BLK_UL_I = 0x20000000;
	private final byte BLK_CS_B = (byte) 0x40;
	private final int  BLK_CS_I = 0x40000000;
	private final byte BLK_CL_B = (byte) 0x60;
	private final int  BLK_CL_I = 0x60000000;
	
	private final int  RED_MASK_5 	= 0x00F80000;
	private final int  GREEN_MASK_5_A = 0x0000C000;
	private final int  GREEN_MASK_5_B = 0x00003800;
	private final int  BLUE_MASK_5 	= 0x000000F8;
	
	// linear mapping of 5 bit R,G and B values to 8 bit values (reduces artifacts)
	// calculated using floating point values of (i * 255) / 32, where 0 <= i <= 31
	// is a 5 bit value of a sub-pixel
	public static final int[] rgbVals = {0x00, 0x08, 0x10, 0x19, 0x21, 0x29, 0x31, 0x3A,
						   		  		 0x42, 0x4A, 0x52, 0x5A, 0x63, 0x6B, 0x73, 0x7B,
						   		  		 0x84, 0x8C, 0x94, 0x9C, 0xA5, 0xAD, 0xB5, 0xBD,
						   		  		 0xC5, 0xCE, 0xD6, 0xDE, 0xE6, 0xEF, 0xF7, 0xFF};
	
	public void compress(FramePacket packet) {
		
		final int[] oldData = packet.getPreviousFrame();
		final int[] newData = packet.getCurrentFrame();
		final byte[] compData = packet.getCompressedFrame();
		
		final int frameLen = oldData.length;
		
		if (frameLen != oldData.length) {
			// TODO : deal with possible resolution change or is this done already?
		}
		
		if (newData.length > 0x0FFFFFFF) {
			// TODO : frame size limit exceeded (28 bits), do something drastic! (exception?)
		}
		
		int compCursor = 0;					// index for the current 'block' in the compressed frame array data
		

		// there are 2 types of block: an 'unchanged' block means a run of pixels identical to the corresponding
		// previous frame and a 'colour' block is a run of identical consecutive pixels.

		boolean inUnchRun = false;			// true if the current cursor position is in a unchanged run
		boolean unchRunEnd = false;			// true if current cursor position has just left an unchanged run
		boolean inColRun = false;			// true if the current cursor position is in a colour run
		boolean colRunEnd = false;			// true if current cursor position has just left an colour run
		
		boolean inRunPrev = false;			// true if the previous pixel is considered part of a run
		
		int unchRunLen = 0;					// length of the current unchanged run
		int colRunLen = 1;					// length of the current colour run
		int colRunVal = 0;					// the ARGB value of the current colour run 

		int newPixel;						// the current ARGB value in the new frame array data
		int oldPixel;						// the current ARGB value in the old frame array data
		
		int newPixelPrev;					// the previous pixel value in the new frame array data
		boolean pixelsLeft;
		
		// compare the first pixels
		newPixel = newData[0];
		oldPixel = oldData[0];
		if (newPixel == oldPixel) {
			inUnchRun = true;
			unchRunLen = 1;
		}
		
		// main loop, iterates over entire pixel array
		int cursor = 0;
		while (cursor < frameLen) {
			cursor++;
			inRunPrev = (inUnchRun || inColRun);
			newPixelPrev = newPixel;
			
			// TODO
			// SORT THIS BIT OUT FOR THE LAST RUN
			if (cursor != frameLen) {
				newPixel = newData[cursor];
				oldPixel = oldData[cursor];
			} else {
				if (inColRun)
				colRunEnd = true;
			if (inUnchRun)
				unchRunEnd = true;
			}
			// -------------------------------
			
			if (newPixel == oldPixel) {
				inUnchRun = true;
				unchRunLen++;
			} else {
				if (inUnchRun) {
					unchRunEnd = true;
					inUnchRun = false;
				}
			}
			
			if (newPixel == newPixelPrev) {
				if(!inColRun) {
					inColRun = true;
					inRunPrev = true;
					colRunVal = newPixel;
				}
				colRunLen++;
			} else {
				if (inColRun) {
					colRunEnd = true;
					inColRun = false;
				}
			}
		
			// if ended run(s) are detected, determine whether to keep or discard
			if (colRunEnd || unchRunEnd) {
				if (colRunLen > unchRunLen) {
					unchRunLen = inUnchRun ? 1 : 0;
					unchRunEnd = false;
				} else {
					if (colRunEnd) {
						colRunEnd = false;
						if (!unchRunEnd && (unchRunLen == colRunLen)) {
							// special case: don't drop a pixel when there is a colour
							// run ending and an ongoing unchanged run of same length
							inRunPrev = false;
						}
					}
					colRunLen = 1;
				}
			}
				
			// determine which, if any runs need to be written as compressed blocks
			if (!inRunPrev) {
				// UNIQUE PIXEL (2 bytes)
				// (keep the single alpha bit from the RGB data to indicate unique pixel)
				
				// THIS MAY BE BETTER......
				//newPixelPrev += ((newPixelPrev & 0x00040404) << 1) - ((((newPixelPrev & 0x00F8F8F8) + 0x00040404) & 0x01010100) >>> 5); 
				
				// .....THAN THIS.....
				// rounding for truncated RGB values
				if (((newPixelPrev & 0x00040000) != 0) && ((newPixelPrev & 0x00FF0000) < 0x00FF0000))
					newPixelPrev += 0x00080000;
				if (((newPixelPrev & 0x00000400) != 0) && ((newPixelPrev & 0x0000FF00) < 0x0000FF00))
					newPixelPrev += 0x00000800;
				if (((newPixelPrev & 0x00000004) != 0) && ((newPixelPrev & 0x000000FF) < 0x000000FF))
					newPixelPrev += 0x00000008;
				
				compData[compCursor++] = (byte) ( ((newPixelPrev & RED_MASK_5) >>> 17)
												| ((newPixelPrev & GREEN_MASK_5_A) >>> 14) );
				compData[compCursor++] = (byte) ( ((newPixelPrev & GREEN_MASK_5_B) >>> 6)
												| ((newPixelPrev & BLUE_MASK_5) >>> 3) );
			}
			
			if (colRunEnd) {
				// rounding for truncated RGB values
				if (((newPixelPrev & 0x00040000) == 1) && ((newPixelPrev & 0x00FF0000) < 0x00FF0000))
					newPixelPrev += 0x00080000;
				if (((newPixelPrev & 0x00000400) == 1) && ((newPixelPrev & 0x0000FF00) < 0x0000FF00))
					newPixelPrev += 0x00000800;
				if (((newPixelPrev & 0x00000004) == 1) && ((newPixelPrev & 0x000000FF) < 0x000000FF))
					newPixelPrev += 0x00000008;
				
				if (colRunLen < 64) {
					// SHORT COLOUR BLOCK (3 bytes compressed)
					// (run length fits in 6 bits (run length < 64), first 3 bits will be 010)
					compData[compCursor++] = (byte) ( (colRunLen & 0x0000001F) | BLK_CS_B );
					compData[compCursor++] = (byte) ( ((colRunLen & 0x00002000) << 2)
													| ((newPixelPrev & RED_MASK_5) >>> 17)
													| ((newPixelPrev & GREEN_MASK_5_A) >>> 14));
					compData[compCursor++] = (byte) ( ((newPixelPrev & GREEN_MASK_5_B) >>> 6)
													| ((newPixelPrev & BLUE_MASK_5) >>> 3) );
				} else {
					// LONG COLOUR BLOCK (5 bytes compressed)
					// (run length fits in 22 bits (run length < 4194304), first 3 bits will be 011
					compData[compCursor++] = (byte) ( ((colRunLen & 0x001F0000) >>> 16 )| BLK_CL_B );
					compData[compCursor++] = (byte) ( (colRunLen & 0x0000FF00) >>> 8);
					compData[compCursor++] = (byte) (colRunLen & 0x000000FF);
					
					compData[compCursor++] = (byte) ( ((colRunLen & 0x00200000) >>> 14)
													| ((newPixelPrev & RED_MASK_5) >>> 17)
													| ((newPixelPrev & GREEN_MASK_5_A) >>> 14));
					compData[compCursor++] = (byte) ( ((newPixelPrev & GREEN_MASK_5_B) >>> 6)
													| ((newPixelPrev & BLUE_MASK_5) >>> 3) );
				}
				colRunEnd = false;
				colRunLen = 1;
				
			} else {
				if (unchRunEnd) {
					if (unchRunLen < 32) {
						// SHORT UNCHANGED BLOCK (1 byte compressed)
						// (run length fits in 5 bits (run length < 32), first 3 bits will be 000)
						compData[compCursor++] = (byte) (unchRunLen & 0x0000001F);
					} else {
						// LONG UNCHANGED BLOCK (4 bytes compressed)
						// (run length uses 29 bits (run length < 536870912), first 3 bits will be 001)
						compData[compCursor++] = (byte) ( ((unchRunLen & 0x1F000000) | BLK_UL_I) >>> 24 );
						compData[compCursor++] = (byte) ( (unchRunLen & 0x00FF0000) >>> 16 );
						compData[compCursor++] = (byte) ( (unchRunLen & 0x0000FF00) >>> 8 );
						compData[compCursor++] = (byte) (unchRunLen & 0x000000FF);
					}
					unchRunEnd = false;
					unchRunLen = 1;
				}
			}
		} // main while loop end
		
//		// Deal with last pixel
//		newPixelPrev = newPixel;
//		if (inColRun)
//			colRunEnd = true;
//		if (inUnchRun)
//			unchRunEnd = true;
//		
//		// if ended run(s) are detected, determine whether to keep or discard
//		if (colRunEnd || unchRunEnd) {
//			if (colRunLen > unchRunLen) {
//				unchRunEnd = false;
//			} else {
//				if (colRunEnd) {
//					colRunEnd = false;
//					if (!unchRunEnd && (unchRunLen == colRunLen)) {
//						// special case: don't drop a pixel when there is a colour
//						// run ending and an ongoing unchanged run of same length
//						inRunPrev = false;
//					}
//				}
//				colRunLen = 1;
//			}
//		}
//			
//		// determine what, if anything needs to be written out
//		if (colRunEnd) {
////			// adjust to the nearest 5 bit value
////			if ((colRunVal & 0x0000007) > 0x00000003)
////				colRunVal += 0x00000004;
////			if ((colRunVal & 0x0000700) > 0x00000300)
////				colRunVal += 0x00000400;
////			if ((colRunVal & 0x0070000) > 0x00030000)
////				colRunVal += 0x00040000;
//			
//			if (colRunLen < 64) {
//				// SHORT COLOUR BLOCK (3 bytes compressed)
//				// (run length fits in 6 bits (run length < 64), first 3 bits will be 010)
//				compData[compCursor++] = (byte) ( (colRunLen & 0x0000001F) | BLK_CS_B );
//				compData[compCursor++] = (byte) ( ((colRunLen & 0x00002000) << 2)
//												| ((colRunVal & RED_MASK) >>> 17)
//												| ((colRunVal & GREEN_MASK_1) >>> 14));
//				compData[compCursor++] = (byte) ( ((colRunVal & GREEN_MASK_2) >>> 6)
//												| ((colRunVal & BLUE_MASK) >>> 3) );
//			} else {
//				// LONG COLOUR BLOCK (5 bytes compressed)
//				// (run length fits in 22 bits (run length < 4194304), first 3 bits will be 011
//				compData[compCursor++] = (byte) ( ((colRunLen & 0x001F0000) >>> 16 )| BLK_CL_B );
//				compData[compCursor++] = (byte) ( (colRunLen & 0x0000FF00) >>> 8);
//				compData[compCursor++] = (byte) (colRunLen & 0x000000FF);
//				
//				compData[compCursor++] = (byte) ( ((colRunLen & 0x00200000) >>> 14)
//												| ((colRunVal & RED_MASK) >>> 17)
//												| ((colRunVal & GREEN_MASK_1) >>> 14));
//				compData[compCursor++] = (byte) ( ((colRunVal & GREEN_MASK_2) >>> 6)
//												| ((colRunVal & BLUE_MASK) >>> 3) );
//			}
//			
//		} else {
//			if (unchRunEnd) {
//				if (unchRunLen < 32) {
//					// SHORT UNCHANGED BLOCK (1 byte compressed)
//					// (run length fits in 5 bits (run length < 32), first 3 bits will be 000)
//					compData[compCursor++] = (byte) (unchRunLen & 0x0000001F);
//				} else {
//					// LONG UNCHANGED BLOCK (4 bytes compressed)
//					// (run length uses 29 bits (run length < 536870912), first 3 bits will be 001)
//					compData[compCursor++] = (byte) (((unchRunLen & 0x1F000000) | BLK_UL_I) >>> 24 );
//					compData[compCursor++] = (byte) ((unchRunLen & 0x00FF0000) >>> 16);
//					compData[compCursor++] = (byte) ((unchRunLen & 0x0000FF00) >>> 8);
//					compData[compCursor++] = (byte) (unchRunLen & 0x000000FF);
//				}
//			} else {
//				if (!inRunPrev) {
//					// UNIQUE PIXEL (2 bytes)
//					// (keep the single alpha bit from the RGB data to indicate unique pixel)
//					int uniquePixel = newPixelPrev;
////					// adjust to the nearest 5 bit value
////					if ((uniquePixel & 0x0000007) > 0x00000003)
////						uniquePixel += 0x00000004;
////					if ((uniquePixel & 0x0000700) > 0x00000300)
////						uniquePixel += 0x00000400;
////					if ((uniquePixel & 0x0070000) > 0x00030000)
////						uniquePixel += 0x00040000;
//					
//					
//					
//					compData[compCursor++] = (byte) ( ((uniquePixel & RED_MASK) >>> 17)
//													| ((uniquePixel & GREEN_MASK_1) >>> 14) );
//					compData[compCursor++] = (byte) ( ((uniquePixel & GREEN_MASK_2) >>> 6)
//													| ((uniquePixel & BLUE_MASK) >>> 3) );
//				}
//			}
//		}
		
		// an encoded length of four indicates a single unchanged run for whole array
		if (compCursor > 4) {
			// if there are changes, compress the data further
			packet.setUnzBytesSize(compCursor);
			try {
				ByteArrayOutputStream bO = new ByteArrayOutputStream();
				GZIPOutputStream zO = new GZIPOutputStream(bO);
				zO.write(compData,0,compCursor);
				zO.close();
				bO.close();
				packet.setCompressedFrame(bO.toByteArray());
				packet.setCompressedFrameSize(bO.size());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			// no changes, indicate this with zero length
			packet.setCompressedFrameSize(0);
		}
	}


	public void decompress(FramePacket packet) {
		
		GZIPInputStream gzipInputStream = null;
		int frameSize = packet.getFrameSize();
		int[] compData = new int[frameSize];
		int readCursor = 0;
		int unzSize = packet.getUnzBytesSize(); 
		byte[] compBytes = new byte[unzSize * 4];
		
		try {
			ByteArrayInputStream biStream = new ByteArrayInputStream( packet.getCompressedFrame(),0,packet.getCompressedFrameSize() );
			gzipInputStream = new GZIPInputStream( biStream );
		}
		catch (Exception e) { e.printStackTrace(); }
		
		try {
			int sizeRead = gzipInputStream.read(compBytes);
			readCursor=sizeRead;
			while(readCursor < unzSize) {
				sizeRead = gzipInputStream.read(compBytes,readCursor,(unzSize - readCursor));
				readCursor += sizeRead;
			}
		} catch (Exception e) {  e.printStackTrace(); }

		
		int compLen = readCursor;
			
		int[] newFrame = packet.getCurrentFrame();
		int cursor = 0;
		int compCursor = 0;
		byte compByte1, compByte2, compByte3, compByte4, compByte5;
		int blockCount;
		int colRunVal;
		
		// WIP
		// -------------------------------------------------------------------
		while (compCursor < compLen) {
			// read the next byte
			compByte1 = compBytes[cursor];
			if ((compByte1 & ALPHA_B) == UNIQ_B ) {
				// unique pixel, write it out
				compCursor++;
				compByte2 = compBytes[compCursor++];
				newFrame[cursor++] = (rgbVals[(compByte1 & 0x7C) >>> 2] << 16) // red
						  		   | (rgbVals[((compByte1 & 0x03) << 3) | ((compByte2 & 0x0E) >>> 5)] << 8) // green
						  		   | (rgbVals[(compByte2 & 0x1F)]) //blue
						  		   | ALPHA_I;
				
//				colRunVal = (0xFF00000 | (int) ((compByte1 & 0x7C) << 17))
//				  | (int) ((compByte1 & 0x03) << 14)
//				  | (int) ((compByte2 & 0x0E) << 6)
//				  | (int) ((compByte2 & 0x1F) << 3);
				
			} else {
				// a block is detected
				if ((compByte1 & BLK_TYPE_MASK_B) == BLK_U_B) {
					// unchanged block
					if ((compByte1 & BLK_MASK_B) == BLK_US_B) {
						// short unchanged block
						blockCount = (int) (compByte1 & 0x1F);
						compCursor++;
					} else {
						// long unchanged block
						compCursor++;
						blockCount =  (int) ((compByte1 & 0x1F) << 24)
									| (int) ((compBytes[compCursor++] & 0xFF) << 16)
									| (int) ((compBytes[compCursor++] & 0xFF) << 8)
									| (int) ((compBytes[compCursor++] & 0xFF));
					}
					// skip the unchanged pixels
					cursor += blockCount;
					
				} else {
					// colour block
					if ((compByte1 & BLK_MASK_B) == BLK_CS_B) {
						// short colour block
						compCursor++;
						compByte2 = compBytes[compCursor++];
						compByte3 = compBytes[compCursor++];
						blockCount = (int) (compByte1 & 0x1F)
								   | (int) ((compByte2 & 0x80) >>> 2); // don't forget that bit!
						colRunVal = (rgbVals[(compByte2 & 0x7C) >>> 2] << 16) // red
				  		   		  | (rgbVals[((compByte2 & 0x03) << 3) | ((compByte3 & 0x0E) >>> 5)] << 8) // green
				  		   		  | (rgbVals[(compByte3 & 0x1F)]) //blue
				  		   		  | ALPHA_I;
						
//						colRunVal = (int) (0xFF000000 | ((compByte2 & 0x7C) << 17))
//								  | (int) ((compByte2 & 0x03) << 14)
//								  | (int) ((compByte3 & 0x0E) << 5)
//								  | (int) ((compByte3 & 0x1F) << 3);
						
					} else {
						// long colour block
						compCursor++;
						compByte2 = compBytes[compCursor++];
						compByte3 = compBytes[compCursor++];
						compByte4 = compBytes[compCursor++];
						compByte5 = compBytes[compCursor++];
						blockCount = (int) (((compByte1 & 0x1F) | (int) ((compByte4 & 0x80)) << 16))
						   		   | (int) ((compByte2 & 0xFF) << 8)
						   		   | (int) (compByte3 & 0xFF);
						colRunVal = (rgbVals[(compByte4 & 0x7C) >>> 2] << 16)// red
		  		   		  		  | (rgbVals[((compByte4 & 0x03) << 3) | ((compByte5 & 0x0E) >>> 5)] << 8) // green
		  		   		  		  | (rgbVals[(compByte5 & 0x1F)]) //blue
		  		   		  		  | ALPHA_I;
						
//						colRunVal = (int) (0xFF000000 | ((compByte4 & 0x7C) << 17))
//						  | (int) ((compByte4 & 0x03) << 14)
//						  | (int) ((compByte5 & 0x0E) << 6)
//						  | (int) ((compByte5 & 0x1F) << 3);
						
					}
					// write out the colour block
					for (int i = 0; i < blockCount; i++) {
						newFrame[cursor++] = colRunVal;
					}
				}
			
			}
		}		
		// -------------------------------------------------------------------
		packet.prepareFrame();
	}
}