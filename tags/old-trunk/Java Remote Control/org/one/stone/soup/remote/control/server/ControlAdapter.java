package org.one.stone.soup.remote.control.server;

public interface ControlAdapter {

	public void keyPress(int code);
	public void keyRelease(int code);
	public void mouseMove(int x,int y);
	public void mousePress(int code);
	public void mouseRelease(int code);
	
	
}
