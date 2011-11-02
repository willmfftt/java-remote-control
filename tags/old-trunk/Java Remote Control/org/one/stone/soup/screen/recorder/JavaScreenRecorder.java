package org.one.stone.soup.screen.recorder;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.OutputStream;

import org.one.stone.soup.grfx.ScreenCapture;

public class JavaScreenRecorder extends ScreenRecorder implements ScreenCapture{

	private boolean waitingForCapture = false;
	private BufferedImage renderedImage = null;
	private Graphics heldGraphics = null;
	private int width;
	private int height;
	private Component source;
	
	public JavaScreenRecorder(Component source,int width,int height,OutputStream outStream,ScreenRecorderListener listener,boolean showMouse)
	{
		super(outStream,listener,showMouse);
		
		this.source = source;
		this.width = width;
		this.height = height;
	}
	
	public Rectangle initialiseScreenCapture() {
		return new Rectangle(width,height);
	}

	public int[] captureScreen(Rectangle recordArea)
	{
		waitingForCapture = true;
		int count=0;
		while(waitingForCapture && count<50)
		{
			try{ Thread.sleep(10); }catch(Exception e){}
			count++;
		}
		source.repaint();
		while(waitingForCapture)
		{
			try{ Thread.sleep(10); }catch(Exception e){}
		}

		int[] rawData = new int[recordArea.width*recordArea.height];
		renderedImage.getRGB(0,0,recordArea.width,recordArea.height,rawData,0,recordArea.width);
		return rawData;
	}		
	
	public Graphics prePaint(Graphics grfx,int width,int height)
	{
		if(waitingForCapture)
		{
			heldGraphics = grfx;
			if(renderedImage==null)
			{
				renderedImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
			}
			grfx = renderedImage.getGraphics();
		}
		
		return grfx;
	}
	
	public void postPaint(ImageObserver observer)
	{
		if(waitingForCapture && renderedImage!=null)
		{
			heldGraphics.drawImage(renderedImage,0,0,observer);
			waitingForCapture=false;
		}		
	}
	
	public Image getSnapshot()
	{
		waitingForCapture=true;
		while(waitingForCapture)
		{
			try{Thread.sleep(50);}catch(Exception e){}
		}
		return renderedImage;
	}
}
