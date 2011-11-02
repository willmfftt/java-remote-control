package com.wet.wired.java.remote.control.frame.capture;

import java.awt.Dimension;

import com.wet.wired.java.remote.control.frame.Frame;

public interface FrameGrabber {

	public Dimension getScreenSize();
	public Frame grabFrame();
}
