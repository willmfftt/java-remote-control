package org.one.stone.soup.remote.control.client;

import java.awt.Component;

import org.one.stone.soup.screen.recorder.ScreenPlayerListener;

public interface RemoteControlClient extends ScreenPlayerListener{

	public abstract void setFullscreen(boolean state);
	public Component getView();
	public void dispose();
}