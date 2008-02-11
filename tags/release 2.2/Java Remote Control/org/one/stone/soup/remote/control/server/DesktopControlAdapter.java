package org.one.stone.soup.remote.control.server;

import java.awt.Robot;

public class DesktopControlAdapter implements ControlAdapter {

	private Robot robot;
	
	public DesktopControlAdapter()
	{
		try{
			robot = new Robot();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void keyPress(int code) {
		robot.keyPress(code);
	}

	public void keyRelease(int code) {
		robot.keyRelease(code);
	}

	public void mouseMove(int x, int y) {
		robot.mouseMove(x,y);
	}

	public void mousePress(int code) {
		robot.mousePress(code);
	}

	public void mouseRelease(int code) {
		robot.mouseRelease(code);
	}

}
