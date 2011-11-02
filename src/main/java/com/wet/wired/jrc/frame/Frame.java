package com.wet.wired.jrc.frame;

import java.awt.Dimension;

public class Frame {

	private int[] data;
	private Dimension size;
	
	public Frame(int width,int height) {
		size = new Dimension(width,height);
		data = new int[width*height];
	}
	
	public int getDataSize() {
		return data.length;
	}
	public Dimension getFrameSize() {
		return size;
	}
	public int[] getData() {
		return data;
	}
	public void setData(int[] data) {
		this.data=data;
	}
	
	public void setPixel(int width,int height,int color) {
		data[width+(size.width*height)]=color;
	}
	public void setAllPixels(int color) {
		for(int i=0;i<size.width*size.height;i++) {
			data[i] = color;
		}
	}
}
