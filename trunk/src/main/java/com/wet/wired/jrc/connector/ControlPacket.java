package com.wet.wired.jrc.connector;

public class ControlPacket {
	//Control packets are sent in the format
	// COMMAND COMMAND_SPACE (ARG_0 COMMAND_SPACE ARG_1 etc) COMMAND_END
	
	public static final String COMMAND_SPACE = "\t";
	public static final String COMMAND_END = "\n";
	public static final String COMMAND_FRAME_SIZE = "frameSize";
	public static final String COMMAND_PACKET_SENT = "packetSent";
	public static final String COMMAND_KEY_PRESSED = "keyPressed";
	public static final String COMMAND_MOUSE_POSITION = "keyPressed";
	
	private ControlPacket() {}
	
	public static String getFrameSizePacket(int width,int height) {
		String command = COMMAND_FRAME_SIZE+COMMAND_SPACE;
		command+=width+COMMAND_SPACE;
		command+=height+COMMAND_SPACE;
		command+=COMMAND_END;
		
		return command;
	}
}
