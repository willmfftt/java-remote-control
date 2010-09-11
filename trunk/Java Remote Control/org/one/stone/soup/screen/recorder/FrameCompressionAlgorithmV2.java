package org.one.stone.soup.screen.recorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class FrameCompressionAlgorithmV2 implements FrameCompressor,FrameDecompressor{

	@Override
	public void compress(FramePacket packet) {
		
		try{
			int[] compressed = CompDecomp.compress(packet.getCurrentFrame(), packet.getPreviousFrame());
			ByteArrayOutputStream bO = new ByteArrayOutputStream();
			DataOutputStream dO = new DataOutputStream(bO);
			for(int cursor=0;cursor<compressed.length;cursor++)
			{
				dO.writeInt(compressed[cursor]);
			}
			bO.flush();
			bO.close();
			
			packet.setCompressedFrame(bO.toByteArray());
			packet.setCompressedFrameSize(bO.size());
		}
		catch(Exception e){e.printStackTrace();}
	}

	@Override
	public void decompress(FramePacket packet) {
		
		try{
			int intArraySize = packet.getCompressedFrameSize()/4;
			int[] compressed = new int[intArraySize];
			ByteArrayInputStream bI = new ByteArrayInputStream(packet.getCompressedFrame(),0,packet.getCompressedFrameSize());
			DataInputStream dI = new DataInputStream(bI);
			
			for(int cursor=0;cursor<intArraySize;cursor++)
			{
				compressed[cursor] = dI.readInt();
			}
			
			CompDecomp.decompress(packet.getPreviousFrame(), compressed);
		}
		catch(Exception e){e.printStackTrace();}
	}

}
