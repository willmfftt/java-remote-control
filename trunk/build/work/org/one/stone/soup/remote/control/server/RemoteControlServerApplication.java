package org.one.stone.soup.remote.control.server;

import java.awt.Frame;
import java.io.OutputStream;

import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;

public interface RemoteControlServerApplication {

	public Frame getRootFrame();
	public void disconnect();
	public void setRequester(String requester);
	public ControlAdapter getControlAdapter();	
	public ScreenRecorder getScreenRecorder(OutputStream outputStream,ScreenRecorderListener listener);
		
}
