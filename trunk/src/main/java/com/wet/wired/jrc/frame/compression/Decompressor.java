package com.wet.wired.jrc.frame.compression;

import java.io.IOException;
import java.io.InputStream;

import com.wet.wired.jrc.frame.Frame;

public interface Decompressor {

	public void decompressFrame(InputStream in,int framePacketSize,Frame lastFrame,Frame currentFrame) throws IOException;
}
