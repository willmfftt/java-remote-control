package org.one.stone.soup.screen.recorder;

public class FrameCompressorFactory {
	/*
	 * Convenient location to switch the pluggable compression
	 * algorithm
	 */
	static private Object fca = new FrameCompressionAlgorithmV1();
	//static private Object fca = new FrameCompressionAlgorithmV2();

	
	private FrameCompressorFactory() {
	}
	
	public static FrameCompressor getFrameCompressor() {
		return (FrameCompressor) fca;
	}
	
	public static FrameDecompressor getFrameDecompressor() {
		return (FrameDecompressor) fca;
	}
}
