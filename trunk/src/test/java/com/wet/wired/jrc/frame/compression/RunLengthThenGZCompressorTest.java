package com.wet.wired.jrc.frame.compression;

import java.awt.Canvas;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import com.wet.wired.jrc.frame.Frame;

public class RunLengthThenGZCompressorTest extends TestCase {

	public Frame blankFrame;
	public Frame blueFrame;
	public Frame newFrame;
	
	public void setUp() {
		blankFrame = new Frame(100,100);
		blankFrame.setAllPixels(0xFF000000);
		
		blueFrame = new Frame(100,100);
		blueFrame.setAllPixels(0xFF0000FF);
		
		newFrame = new Frame(100,100);
	}
	
	public void testCompressedSizeSmallerThanUncompressedSize() throws IOException {
		RunLengthThenGZCompressor rlgz = new RunLengthThenGZCompressor();
		
		Frame lastFrame = blankFrame;
		Frame currentFrame = blueFrame;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		rlgz.compressFrame(lastFrame, currentFrame, out);
		
		if(out.toByteArray().length > currentFrame.getDataSize()*4) {
			fail("Compressed size greater than original size");
		}
	}
	
	public void testCompressedSizeEqualsReportedSize() throws IOException {
		RunLengthThenGZCompressor rlgz = new RunLengthThenGZCompressor();
		
		Frame lastFrame = blankFrame;
		Frame currentFrame = blueFrame;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int size = rlgz.compressFrame(lastFrame, currentFrame, out);
		
		assertEquals(out.toByteArray().length, size);
	}
	
	public void testDecompressedFrameSameAsCompressedFrame() throws IOException {
		RunLengthThenGZCompressor rlgz = new RunLengthThenGZCompressor();
		
		Frame lastFrame = blankFrame;
		Frame currentFrame = blueFrame;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int size = rlgz.compressFrame(lastFrame, currentFrame, out);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		rlgz.decompressFrame(in, size, lastFrame, newFrame);
		
		assertEquals(currentFrame.getDataSize(),newFrame.getDataSize());
		
		for(int i=0;i<currentFrame.getDataSize();i++) {
			if(currentFrame.getData()[i]!=newFrame.getData()[i]) {
				fail("Decompressed data "+currentFrame.getData()[i]+" did not match "+newFrame.getData()[i]+" at index "+i);
			}
		}
	}
	
	public static Image loadImage(String fileName) {

		return new Canvas().getToolkit().createImage( fileName );
		
	}
}
