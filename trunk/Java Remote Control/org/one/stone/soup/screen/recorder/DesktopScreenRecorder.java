package org.one.stone.soup.screen.recorder;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.peer.RobotPeer;
import java.io.OutputStream;

import sun.awt.ComponentFactory;

public class DesktopScreenRecorder extends ScreenRecorder{

	private Robot robot;
	private RobotPeer peer;
	
	public DesktopScreenRecorder(OutputStream oStream,ScreenRecorderListener listener,boolean showMouse)
	{
		super(oStream,listener,showMouse);
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
		
		GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		try{
			peer = ((ComponentFactory)toolkit).createRobot(robot, screen);
		}
		catch(Exception e){ e.printStackTrace(); }
		
		return new Rectangle( Toolkit.getDefaultToolkit ().getScreenSize() );

	}
	
	public Robot getRobot()
	{
		return robot;
	}

	public int[] captureScreen(Rectangle recordArea)
	{
		if(showMouse==false)
		{
			return peer.getRGBPixels(recordArea);
		}
		else
		{
			Point mousePosition = MouseInfo.getPointerInfo().getLocation();
			BufferedImage image = robot.createScreenCapture(recordArea);
		
			Polygon pointer = new Polygon(new int[]{0,16,10,8},new int[]{0,8,10,16},4);
			Polygon pointerShadow = new Polygon(new int[]{6,21,16,14},new int[]{1,9,11,17},4);
			
			Graphics2D grfx = image.createGraphics();
			grfx.translate(mousePosition.x,mousePosition.y);
			grfx.setColor( new Color(100,100,100,100) );
			grfx.fillPolygon( pointerShadow );
			grfx.setColor( new Color(100,100,255,200) );
			grfx.fillPolygon( pointer );
			grfx.setColor( Color.red );
			grfx.drawPolygon( pointer );
			grfx.dispose();
			
			int[] rawData = new int[recordArea.width*recordArea.height];
			image.getRGB(0,0,recordArea.width,recordArea.height,rawData,0,recordArea.width);
			
			return rawData;
		}
	}	
}