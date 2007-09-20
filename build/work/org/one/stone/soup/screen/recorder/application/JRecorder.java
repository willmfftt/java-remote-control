package org.one.stone.soup.screen.recorder.application;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import org.one.stone.soup.file.FileChangeListener;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.file.FileWatch;
import org.one.stone.soup.screen.recorder.DesktopScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;
import org.one.stone.soup.swing.JRootFrame;

public class JRecorder extends JRootFrame implements ScreenRecorderListener,ActionListener,FileChangeListener{

	private ScreenRecorder recorder;
	private File temp;
	
	private JButton control;
	private JLabel text;
	private int frameCount;
	
	public JRecorder()
	{
		super("Screen Recorder",new String[]{});
		
		control = new JButton("Start Recording");
		control.setActionCommand("start");
		control.addActionListener(this);
		this.getContentPane().add( control,BorderLayout.WEST );
		
		text = new JLabel("Ready to record");
		this.getContentPane().add( text,BorderLayout.SOUTH );
		
		this.pack();
		this.show();
	}

	public JRecorder(String command,String fileName)
	{
		super("Screen Recorder",new String[]{});
		
		if(command.equals("stop"))
		{
			FileHelper.buildFile(fileName+".control","STOP");
			System.out.println("Requested Stop Recording to "+fileName);
			System.exit(0);
		}
		else
		{
			FileHelper.buildFile(fileName+".control","RECORDING");
			startRecording(fileName);
			
			System.out.println("Started Recording to "+fileName);
			
			FileWatch fileWatch = new FileWatch(fileName+".control",1000);
			fileWatch.addListener(this);
		}
	}
	
	public void startRecording(String fileName)
	{
		if(recorder!=null)
		{
			return;
		}
		
		try{
			FileOutputStream oStream = new FileOutputStream(fileName);
			temp=new File(fileName);
			recorder = new DesktopScreenRecorder(oStream,this);
			recorder.startRecording();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void actionPerformed(ActionEvent ev)
	{
		if(ev.getActionCommand().equals("start") && recorder==null)
		{
			try{
				temp = File.createTempFile("temp","rec");
				
				startRecording( temp.getAbsolutePath() );
				control.setActionCommand("stop");
				control.setText("Stop Recording");				
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else if(ev.getActionCommand().equals("stop") && recorder!=null)
		{
			text.setText("Stopping");
			recorder.stopRecording();
		}
	}

	public void frameRecorded(boolean fullFrame)
	{
		frameCount++;
		if(text!=null)
		{
			text.setText("Frame: "+frameCount);
		}
	}

	public void recordingStopped()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.showSaveDialog(this);
		File target = chooser.getSelectedFile();
		if(target!=null)
		{
			FileHelper.copy(temp,target);
		}
		recorder = null;
		control.setActionCommand("start");
		control.setText("Start Recording");
		
		text.setText("Ready to record");
	}

	/* (non-Javadoc)
	 * @see wet.wired.swing.JRootFrame#destroy()
	 */
	public boolean destroy(Object source) {
		return true;
	}

	public void fileChanged(File file) {
		recorder.stopRecording();
		FileHelper.buildFile(temp.getAbsolutePath()+".control","STOPPED");
		
		System.out.println("Stopped Recording to "+temp);
		System.exit(0);
	}
	
	public static void main(String[] args)
	{
		if(args.length==2)
		{
			JRecorder recorder = new JRecorder(args[0],args[1]);
		}
		else
		{
			JRecorder recorder = new JRecorder();
		}
	}
}
