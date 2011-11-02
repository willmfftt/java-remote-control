package com.wet.wired.jrc.frame.compression;

import java.io.IOException;
import java.io.OutputStream;

import com.wet.wired.jrc.frame.Frame;

public interface Compressor {

	public int compressFrame(Frame lastFrame,Frame currentFrame,OutputStream out) throws IOException;
}
