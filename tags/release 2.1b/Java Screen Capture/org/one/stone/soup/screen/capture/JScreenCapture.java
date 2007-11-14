package org.one.stone.soup.screen.capture;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.one.stone.soup.swing.JRootFrame;
import org.one.stone.soup.swing.SimpleFileFilter;

public class JScreenCapture extends JRootFrame implements ActionListener {

	private Robot robot;
	private Rectangle recordArea;

	public static final void main(String[] args)
	{
		try{
			new JScreenCapture(args);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	public JScreenCapture(String[] args) throws AWTException
	{
		super("JScreenCapture",args);

		robot = new Robot();
		recordArea = new Rectangle( Toolkit.getDefaultToolkit ().getScreenSize() );

		JButton capture = new JButton("Capture Screen");
		capture.setActionCommand("capture");
		capture.addActionListener(this);
		getContentPane().add(capture);

		pack();
		setVisible(true);
	}

	public boolean destroy(Object source) {
		return true;
	}

	public void actionPerformed(ActionEvent event)
	{
		captureScreen();
	}

	private void captureScreen()
	{
		this.setVisible(false);
		BufferedImage image = robot.createScreenCapture(recordArea);

		String targetFile = chooseFile();
		this.setVisible(true);

		if(targetFile == null)
		{
			return;
		}

		try{
			FileOutputStream outStream = new FileOutputStream(targetFile);
			ImageIO.write(image,"PNG",outStream);
			outStream.flush();
			outStream.close();
		}
		catch(Exception e)
		{
			JOptionPane.showConfirmDialog(this,e.getMessage());
		}
	}

	public String chooseFile() {
		JFileChooser chooser = new JFileChooser();

		chooser.setDialogTitle("Save As");
		chooser.setFileFilter(new SimpleFileFilter("png","png"));

		chooser.showSaveDialog(this);

		File file = chooser.getSelectedFile();

		if(file==null)
		{
			return null;
		}
		else
		{
			try{
				return file.toString();
			}
			catch(Exception e)
			{
				return null;
			}
		}
	}
}

