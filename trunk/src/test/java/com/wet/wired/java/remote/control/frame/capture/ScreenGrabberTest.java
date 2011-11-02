package com.wet.wired.java.remote.control.frame.capture;

import java.awt.Dimension;

import junit.framework.TestCase;

import com.wet.wired.java.remote.control.frame.Frame;

public class ScreenGrabberTest extends TestCase {

	public void testScreenSizeReturned() {
		Dimension screenSize = new ScreenGrabber().getScreenSize();
		
		assertNotNull(screenSize);
		if(screenSize.width==0 || screenSize.height==0) {
			fail( "Screen size must not be 0 width or height" );
		}
	}
	
	public void testFrameReturned() {
		Frame frame = new ScreenGrabber().grabFrame();
		
		assertNotNull(frame);
		
		if(frame.getDataSize()==0) {
			fail("Frame data size must not be 0");
		}
		Dimension frameSize = frame.getFrameSize();
		if(frameSize.width==0 || frameSize.height==0) {
			fail( "Frame size must not be 0 width or height" );
		}
	}
	
	public void testFrameSizeMatchesScreenSize() {
		ScreenGrabber screenGrabber = new ScreenGrabber();
		Frame frame = screenGrabber.grabFrame();
		Dimension screenSize = screenGrabber.getScreenSize();
		Dimension frameSize = frame.getFrameSize();
		
		if(frameSize.width!=screenSize.width || frameSize.height!=screenSize.width) {
			fail( "Frame size must match Screen size" );
		}
	}
}
