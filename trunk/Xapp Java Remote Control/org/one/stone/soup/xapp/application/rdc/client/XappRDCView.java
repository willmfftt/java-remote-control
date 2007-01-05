package org.one.stone.soup.xapp.application.rdc.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.one.stone.soup.grfx.ImageFactory;
import org.one.stone.soup.remote.control.client.RemoteControlClient;
import org.one.stone.soup.xapp.resource.manager.XuiResourceManager;
import org.one.stone.soup.xapp.swing.components.XappSwingImage;

public class XappRDCView extends XappSwingImage implements RemoteControlClient {

	private XappRDCClient controller;
	private JFrame tempFrame;
	private ImageIcon viewIcon;
	
	public XappRDCView(XuiResourceManager resourceManager,XappRDCClient controller)
	{
		super(resourceManager);
		this.controller = controller;
		
		viewIcon = new ImageIcon( ImageFactory.loadImage("jar://resources/wait.png") );
		setIcon(viewIcon);
	}
	
	public void setFullscreen(boolean state) {
		GraphicsDevice device = this.getGraphicsConfiguration().getDevice();
		if(device.isFullScreenSupported())
		{

			if(state)
			{
				this.getParent().remove( this );
				tempFrame = new JFrame();
				tempFrame.setUndecorated(true);
				tempFrame.addKeyListener(controller);
				
				tempFrame.add(this,BorderLayout.CENTER);
				
				device.setFullScreenWindow(tempFrame);
				this.setVisible(true);
			}
			else
			{
				controller.showClient();

				device.setFullScreenWindow(null);
				tempFrame.dispose();
				tempFrame=null;
				
				this.setVisible(true);
			}
		}
	}

	public Component getView() {
		return this;
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void showNewImage(Image image) {
		if(image==null)
		{
			return;
		}
		viewIcon.setImage(image);
		this.repaint(0);
	}

	public void playerStopped() {}

	public void newFrame() {
		controller.requestNextFrame();
	}

}
