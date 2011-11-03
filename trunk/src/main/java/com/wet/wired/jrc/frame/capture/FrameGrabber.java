package com.wet.wired.jrc.frame.capture;

import java.awt.Dimension;

import com.wet.wired.jrc.frame.Frame;

public interface FrameGrabber {
	public Dimension getFrameSize();
	public Frame grabFrame();
}
