package com.wet.wired.jrc.connector;

import java.io.InputStream;

public interface ReplayConnectionController extends ConnectionController {

	public InputStream openReplayStream();
	public void closeReplayStream();
}
