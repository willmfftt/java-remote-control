package org.one.stone.soup.screen.recorder;

public class CompDecomp {
	
	private CompDecomp() {}
	
	static int[] compress(int[] newFrame, int[] oldFrame) {
		
		final int frameLen = oldFrame.length;
		
		if (frameLen != oldFrame.length) {
			// TODO : deal with possible resolution change or is this done already?
		}
		
		if (newFrame.length > 0x0FFFFFFF) {
			// TODO : frame size limit exceeded (28 bits), do something drastic! (exception?)
		}
		
		// TODO : eliminate this array and update the newFrame array instead
		//		to improve cache hits (be aware that calling code has extracted
		//		all necessary data from newFrame at this stage!)
		int[] compFrame = new int[frameLen];
		
		int compCursor = 0;				// index for the current 'block' in the compressed frame array data
		
		// there are 2 types of block: an 'unchanged' block means a run of pixels identical to the corresponding
		// previous frame and a 'colour' block is a run of identical consecutive pixels.
		boolean unchangedBlock = false;
		boolean unchangedBlockEnd = false;
		boolean colourBlock = false;
		boolean colourBlockEnd = false;
		
		boolean inBlock = false;
		
		final int ID_UNC = 0x10000000;
		final int ID_COL = 0x20000000;
		
		int unchangedLen = 0;
		int colBlockLen = 0;
		int colBlockVal = 0;
		
		int newPixel;								// the current pixel value in the new frame array data
		int oldPixel;								// the current pixel value in the old frame array data
		
		int newPixelPrev;				// the previous pixel value in the new frame array data
													// the 4 most significant bits are initialised to 0xF
													// to reflect the reserved starting value
		
		int cursor = 1;						// index for the next pixel in the frame array
		
		newPixelPrev = 0xF0000000;
		newPixel = newFrame[0];
		oldPixel = oldFrame[0];
		if (newPixel == oldPixel) {
			unchangedBlock = true;
			unchangedLen = 1;
		}
		
		// main loop, iterates over entire pixel array
		while (cursor < frameLen) {
			newPixelPrev = newPixel;
			newPixel = newFrame[cursor];
			oldPixel = oldFrame[cursor];
			
			// if we were previously in both colour and unchanged runs
			// =======================================================
			if (colourBlock && unchangedBlock) {
				if (newPixel != newPixelPrev) {
					colourBlock = false;
				}
				if (newPixel != oldPixel) {
					unchangedBlock = false;
				}
				// now check if we are not in both types of run
				if (unchangedBlock) {
					if (colourBlock) {
						// in both runs still
						colBlockLen++;
						unchangedLen++;
						
					} else {
						// colour block has ended, decide if it is better than
						// current unchanged block
						if (colBlockLen > unchangedLen) {
							// write out current colour block
							compFrame[compCursor] = ID_COL | colBlockLen;
							compCursor++;
							compFrame[compCursor] = colBlockVal;
							compCursor++;
							unchangedLen = 1;  // unchanged block gets 'trimmed'
						} else {
							unchangedLen++;
						}
						colBlockLen = 0;
					}
				} else {
					// unchanged block has ended
					if (colourBlock) {
						if (unchangedLen > colBlockLen) {
							compFrame[compCursor] = ID_UNC | unchangedLen;
							compCursor++;
							colBlockLen = 0; // colour block is cancelled
							colourBlock = false;
						} else {
							colBlockLen++;
						}
						unchangedLen = 0;
					}
				}
			}
			
			// there are not current simultaneous runs
			// =======================================
			else {
				if (newPixel == oldPixel) {
					unchangedBlock = true;
					unchangedLen++;
				} else {
					if (unchangedBlock) {
						unchangedBlock = false;
						unchangedBlockEnd = true;
					}
				}

				if (newPixel == newPixelPrev) {
					if (!colourBlock) {
						// begin storing colour run state
						colourBlock = true;
						colBlockLen = 2;
						colBlockVal = newPixel;
					} else {
						// expand colour run size
						colBlockLen++;
					} 
				} else {
					if (colourBlock) {
						colourBlock = false;
						colourBlockEnd = true;
					}
				}
				
				if (colourBlockEnd) {
					compFrame[compCursor] = ID_COL | colBlockLen;
					compCursor++;
					compFrame[compCursor] = colBlockVal;
					compCursor++;
					colBlockLen = 0;
					colourBlockEnd = false;
				} else {
					if (unchangedBlockEnd) {
						compFrame[compCursor] = ID_UNC | unchangedLen;
						compCursor++;
						unchangedBlockEnd = false;
						unchangedLen = 0;
					} else {
						if (unchangedLen < 2 && colBlockLen == 0) {
							// no blocks to write, output a single unique pixel
							compFrame[compCursor] = newPixelPrev;
							compCursor++;
						}
					}
				}
			}
			cursor++;  // get ready for the next incoming pixel
		}
		
		// loop finished, deal with last value
		if (unchangedBlock) {
			if (colourBlock) {
				if (colBlockLen > unchangedLen) {
					compFrame[compCursor] = ID_COL + colBlockLen;
					compCursor++;
					compFrame[compCursor] = colBlockVal;
					compCursor++;
				} else {
					compFrame[compCursor] = ID_UNC + unchangedLen;
					compCursor++;
				}
			} else {
				compFrame[compCursor] = ID_UNC + unchangedLen;
				compCursor++;
			}
		} else {
			if (colourBlock) {
				compFrame[compCursor] = ID_COL + colBlockLen;
				compCursor++;
				compFrame[compCursor] = colBlockVal;
				compCursor++;
			} else {
				compFrame[compCursor] = newPixel;
				compCursor++;
			}
		}
		
		
		int[] outArray = new int[compCursor];		
		System.arraycopy(compFrame, 0, outArray, 0, compCursor);
		
		return outArray;
	}
	
	static int[] decompress(int[] oldFrame, int[] compFrame) {
		
		
		int[] newFrame = new int[oldFrame.length];
		
		int cursor = 0;
		int compCursor = 0;
		int compVal;
		
		// TODO : try different nesting of if-elses and orderings inside this
		//		loop and compare performance
		while (compCursor < compFrame.length) {
			compVal = compFrame[compCursor];
			if ((compVal & 0x10000000) == 0x10000000) {
				final int length = (compVal & 0x0FFFFFFF);
				System.arraycopy(oldFrame, cursor, newFrame, cursor, length);
				compCursor++;
				cursor += length;
			} else {
				if ((compVal & 0x20000000) == 0x20000000) {
					int count = (compVal & 0x0FFFFFFF);
					compCursor++;
					final int pixel = (compFrame[compCursor]);
					compCursor++;
					while (count > 0) {
						newFrame[cursor] = pixel;
						cursor++;
						count--;
					}
				} else {
					if (( (compVal & 0xF0000000) == 0)) {
						newFrame[cursor] = compFrame[compCursor];
						cursor++;
						compCursor++;
					}
				}
			}
		} //while end
		
		
		
		
		return newFrame;
	}

}
