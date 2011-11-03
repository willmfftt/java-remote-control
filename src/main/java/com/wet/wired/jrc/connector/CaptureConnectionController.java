package com.wet.wired.jrc.connector;

import java.io.OutputStream;

public interface CaptureConnectionController extends ConnectionController {

	public OutputStream openCaptureStream();
	public void closeCaptureStream();
}
