/*
 * Created on 25-Oct-05
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.one.stone.soup.remote.control.server;

import java.io.IOException;
import java.net.Socket;

import javax.swing.JOptionPane;

import org.one.stone.soup.io.Connection;
import org.one.stone.soup.net.SocketConnection;
import org.one.stone.soup.remote.control.hub.HubHelper;
import org.one.stone.soup.server.http.client.HttpTunnelConnection;
import org.one.stone.soup.stringhelper.StringGenerator;


/**
 * @author Nik Cross
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RemoteControlHubConnection implements Runnable{
	
	private String hubAlias;
	private Connection screenConnection;
	private Connection controlConnection;
	private RemoteControlServer server;
	
	private String hubPassword;
	
	public RemoteControlHubConnection(String alias,String password,RemoteControlServer server)
	{
		this.server = server;
		this.hubAlias = alias;
		this.hubPassword = password;
	}
	
	/*
	 * write connection type (screen)
	 * login
	 * 
	 * write connection type (control)
	 * login
	 * 
	 */
	public void connect(String address,int port) throws IOException
	{
		connect(null,address,port);
	}
	public void connect(String tunnelUrl,String address,int port) throws IOException
	{	
		String tunnelId = null;
		
		if(tunnelUrl!=null)
		{
			tunnelId = StringGenerator.generateUniqueId();
			screenConnection = new HttpTunnelConnection(tunnelUrl,address,port,tunnelId);
		}
		else
		{
			screenConnection = new SocketConnection( new Socket(address,port),address+":"+port );
		}
		
		//RemoteControlServer.writeLine("server.screen",screenSocket.getOutputStream());
		//String pebble = login(screenSocket,hubPassword);
		String pebble = HubHelper.login("server.screen",screenConnection.getOutputStream(),screenConnection.getInputStream(),hubAlias,hubPassword);
		
		if(tunnelUrl!=null)
		{
			controlConnection = new HttpTunnelConnection(tunnelUrl,address,port,tunnelId);
		}
		else
		{
			controlConnection = new SocketConnection( new Socket(address,port),address+":"+port );
		}		
		//RemoteControlServer.writeLine("server.control",controlSocket.getOutputStream());
		//login(controlSocket,pebble);
		if( pebble==null || HubHelper.login("server.control",controlConnection.getOutputStream(),controlConnection.getInputStream(),hubAlias,pebble)==null )
		{
			throw new IOException("Login failed");
		}
		
		new Thread(this,"Hub Connection").start();
	}
	
	public void run()
	{
		try{
			//InputStream iStream = screenConnection.getInputStream();
			
			/*String request = RemoteControlServer.readLine( iStream );
			
			if(request.equals("connect"))
			{*/
				//packet.getAttributeValueByName("alias");
				
				server.route(screenConnection,null);
				server.route(controlConnection,null);
			//}
					
		}
		catch(Exception e){}
	}
	
	public void disconnect()
	{
		try{
			screenConnection.close();
		}catch(Exception e){}
		try{
			controlConnection.close();
		}catch(Exception e){}
	}
}
