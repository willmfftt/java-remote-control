package org.one.stone.soup.remote.control.server;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.one.stone.soup.swing.JRootFrame;

public class JavaControlAdapter implements ControlAdapter {

	private class Source
	{
		public Point position;
		public Component component;
	}
	
	private EventQueue queue;
	private Frame source;
	private int x=0;
	private int y=0;
	private boolean dragging = false;
	
	public JavaControlAdapter(Frame source)
	{
		this.source = source;
		queue = Toolkit.getDefaultToolkit().getSystemEventQueue();		
	}
	
	public void keyPress(int code) //TODO not working
	{
		Source clickSource = getClickSource();
		KeyEvent event = new KeyEvent(clickSource.component,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,code,(char)code);
		queue.postEvent(event);
	}

	public void keyRelease(int code) //TODO not working
	{
		Source clickSource = getClickSource();
		KeyEvent event = new KeyEvent(clickSource.component,KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,code,(char)code);
		queue.postEvent(event);
	}

	public void mouseMove(int x, int y) {
		this.x = x-5;
		this.y = y-50;		
		
		if(dragging) //TODO not working
		{
			MouseEvent event = new MouseEvent(source,MouseEvent.MOUSE_DRAGGED,System.currentTimeMillis(),14,x,y,1,false,MouseEvent.BUTTON1);
			queue.postEvent(event);
		}
		else
		{
			MouseEvent event = new MouseEvent(source,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,x,y,0,false);
			queue.postEvent(event);			
		}
		
		source.repaint(1);
	}

	public void mousePress(int code) {
		Source clickSource = getClickSource();

		MouseEvent event = new MouseEvent(clickSource.component,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),code,clickSource.position.x,clickSource.position.y,1,false,MouseEvent.BUTTON1);
		queue.postEvent(event);
		dragging = true;
		source.repaint();
	}

	public void mouseRelease(int code) {
		Source clickSource = getClickSource();
		
		MouseEvent event = new MouseEvent(clickSource.component,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),code,clickSource.position.x,clickSource.position.y,1,false,MouseEvent.BUTTON1);
		queue.postEvent(event);
		dragging = false;
		source.repaint();
	}

	private Source getClickSource()
	{
		Component clickSource = source;
		int posX = x;
		int posY = y;
		Component nextClickSource = ((JRootFrame)source).getContentPane().getComponentAt(posX,posY);
		while(nextClickSource!=null && nextClickSource!=clickSource)
		{
			clickSource = nextClickSource;
			Point pos = clickSource.getLocation();
			posX=posX-pos.x;
			posY=posY-pos.y;
			nextClickSource = clickSource.getComponentAt(posX,posY);
		}
		
		Source source = new Source();
		source.position = new Point(posX,posY);
		source.component = clickSource;
		
		return source;
	}
}
