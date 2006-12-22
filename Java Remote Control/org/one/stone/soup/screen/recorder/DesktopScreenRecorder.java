package org.one.stone.soup.screen.recorder;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

public class DesktopScreenRecorder extends ScreenRecorder{

	private Robot robot;
	
	public DesktopScreenRecorder(OutputStream oStream,ScreenRecorderListener listener)
	{
		super(oStream,listener);
	}

	public Rectangle initialiseScreenCapture()
	{		
		try{
			robot = new Robot();
		}
		catch(AWTException awe)
		{
			awe.printStackTrace();
			return null;
		}
		return new Rectangle( Toolkit.getDefaultToolkit ().getScreenSize() );

	}
	
	public Robot getRobot()
	{
		return robot;
	}

	public BufferedImage captureScreen(Rectangle recordArea)
	{
		return robot.createScreenCapture(recordArea);
	}	
}
