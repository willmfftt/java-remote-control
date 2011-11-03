package com.wet.wired.jrc.frame.capture;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import com.wet.wired.jrc.frame.Frame;

public class ScreenGrabber implements FrameGrabber {

	private Robot robot;
	private Rectangle screenRectangle; 
	
	public ScreenGrabber() throws FrameCaptureException {
		initialize();
	}
	
	private void initialize() throws FrameCaptureException {
		try{
			robot = new Robot();
			screenRectangle = new Rectangle(Toolkit.getDefaultToolkit ().getScreenSize());
		} catch (Exception e) {
			throw new FrameCaptureException("Failed to initialize ScreenGrabber",e);
		}
	}
	
	@Override
	public Dimension getFrameSize() {
		return screenRectangle.getSize();
	}

	@Override
	public Frame grabFrame() {
		BufferedImage grab = robot.createScreenCapture(screenRectangle);
		int[] rawData = new int[screenRectangle.width*screenRectangle.height];
		grab.getRGB(0,0,screenRectangle.width,screenRectangle.height,rawData,0,screenRectangle.width);
		
		Frame frame = new Frame(screenRectangle.width,screenRectangle.height);
		frame.setData(rawData);
		return frame;
	}

}

