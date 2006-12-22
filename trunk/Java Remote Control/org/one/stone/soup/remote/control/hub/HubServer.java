package org.one.stone.soup.remote.control.hub;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;

import org.one.stone.soup.authentication.client.Authenticator;
import org.one.stone.soup.authentication.server.AccessCheck;
import org.one.stone.soup.authentication.server.Login;
import org.one.stone.soup.constants.TimeConstants;
import org.one.stone.soup.io.Connection;
import org.one.stone.soup.io.ConnectionListener;
import org.one.stone.soup.net.SocketToSocketConnection;
import org.one.stone.soup.server.PlainServer;
import org.one.stone.soup.server.RouterThread;
import org.one.stone.soup.server.Server;
import org.one.stone.soup.server.http.Logger;
import org.one.stone.soup.stringhelper.StringArrayHelper;
import org.one.stone.soup.util.TimeWatch;

/**
 * @author Nik Cross
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class HubServer extends PlainServer implements ConnectionListener,ActionListener{

	private Hashtable sessions = new Hashtable();
	private Authenticator authenticator;
	private String alias;
	private Logger logger;
	
	private SessionListener sessionListener;
	private long lastDataSize = 0;
	private long maxDataSize = 1;
	
	public class Session implements Runnable
	{
		String owner;
		String user;
		
		String alias;
		Socket serverSocket;
		Socket clientSocket;
		SocketToSocketConnection connection;
		
		public Session()
		{
			new Thread(this,"Session keep alive").start();
		}
		
		public void run()
		{
			while(alive && !active)
			{				
				if(serverSocket!=null)
				{
					try{
						Thread.sleep(1000);
						
						serverSocket.getOutputStream().write(0);
						serverSocket.getOutputStream().flush();
					}
					catch(Exception e)
					{
						//e.printStackTrace();
						alive=false;
					}
				}
			}
		}
		
		boolean alive = true;
		boolean active = false;
	}

	private TimeWatch connectionChecker;
	
	public HubServer(String address,int port,String alias,Authenticator authenticator,Logger logger)
	{
		this.alias = alias;
		this.authenticator = authenticator;
		this.logger = logger;
		start("Remote Control Hub",address,port,100,1000);
		
		connectionChecker = new TimeWatch(5000);
		connectionChecker.setActionCommand("check connections");
		connectionChecker.addActionListener(this);
		connectionChecker.start();
	}

	public void stop()
	{
		super.stop();
		
		Enumeration keys = sessions.keys();
		while(keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			Session session = (Session)sessions.get(key);
			
			try{
				session.clientSocket.close();
			}catch(Exception e){}
			try{
				session.serverSocket.close();
			}catch(Exception e){}
		}
	}
	
	public void setSessionListener(SessionListener listener)
	{
		this.sessionListener = listener;
	}
	
	public void actionPerformed(ActionEvent ae)
	{	
		int sessionCount = 0;
		long dataSent = 0;
		
		try{
			Enumeration enumerator = sessions.keys();

			while(enumerator.hasMoreElements())
			{
				String key = enumerator.nextElement().toString();
				Session session = (Session)sessions.get(key);
				if(session.alive==false)
				{
					try{ session.clientSocket.close(); }catch(Exception e){}
					try{ session.serverSocket.close(); }catch(Exception e){}
					try{ session.connection.close(); }catch(Exception e){}
					if(sessionListener!=null)
					{
						sessionListener.sessionClosed(session.owner,session.user);
					}

					sessions.remove(key);
					
					Login login = new Login(null,session.owner,null);
					authenticator.logout(login);
					
					System.out.println(login.getResult()+" Session "+key+" closed and now removed.");
				}
				else
				{
					sessionCount++;
					dataSent+=session.connection.getDataSentSize()+session.connection.getDataReceivedSize();
				}
			}
		} catch(Exception e){}
		
		long dataSize = dataSent-lastDataSize;
		if(dataSize>maxDataSize)
		{
			maxDataSize = dataSize;
		}
		lastDataSize = dataSent;
		sessionListener.logData( sessionCount,dataSize,maxDataSize );
		
		connectionChecker.start();
	}

	/* (non-Javadoc)
	 * @see wet.wired.server.Server#route(java.net.Socket, wet.wired.server.RouterThread)
	 */
	public void route(Socket socket, RouterThread router) {
		
		try{			
			Login login = login(socket);
			if(login==null)
			{
				return;
			}
			
			String[] args = StringArrayHelper.parseFields( login.getSubdomain(),'.' );
			String type = args[0];
			String flavour = args[1];
			
			if(type.equals("client"))
			{
				makeClientConnection(socket,login,flavour);
			}
			else if(type.equals("server"))
			{
				makeServerConnection(socket,login,flavour);
			}
			else
			{
				socket.close();
			}
		}
		catch(Exception e)
		{
			try{ socket.close(); }catch(Exception e2){}
		}
	}

	private void makeClientConnection(Socket socket,Login login,String flavour) throws IOException
	{
		InputStream iStream = socket.getInputStream();
		String request = readLine( iStream );
		
		Session session = (Session)sessions.get( request+"."+flavour );
		
		connectSession( login.getUser().getName(),request+"."+flavour,session,socket );

		System.out.println("Client.control created. Alias:"+request);		
	}

	private void makeServerConnection(Socket socket,Login login,String flavour) throws IOException
	{
		Session session = new Session();
		session.serverSocket = socket;
		session.owner = login.getUser().getName();
		sessions.put( login.getUser().getName()+"."+flavour,session );
		
		System.out.println("Server.screen created. Alias:"+login.getUser().getName());	
	}
	private Login login(Socket socket) throws IOException
	{
		InputStream iStream = socket.getInputStream();
		
		String connectionType = readLine( iStream );
		
		writeLine( "LOGIN",socket.getOutputStream() );
		String userName = readLine(iStream);
		String password = null;
		String[] args = StringArrayHelper.parseFields(userName,':');
		if(args.length!=2)
		{
			logger.log("Hub Server login failed for request "+userName);

			writeLine( "BYE",socket.getOutputStream() );
			socket.close();
			return null;	
		}
		else
		{
			userName=args[0];
			password=args[1];
		}
		
		Login login = new Login(alias,userName,password);
		
		if(authenticate(login,connectionType)==false)
		{
			System.out.println("Login failed:"+login.getResult());

			logger.log("Hub Server login failed for "+userName);

			writeLine( "BYE",socket.getOutputStream() );
			socket.close();
			return null;										
		}
		
		writeLine( "HELLO "+login.getPebble(),socket.getOutputStream() );
		logger.log("Hub Server "+userName+" logged in");

		login.setSubdomain(connectionType);
		return login;
	}
	
	/* (non-Javadoc)
	 * @see wet.wired.server.Server#initialize()
	 */
	public void initialize() {}

	
	private void connectSession(String user,String alias,Session session,Socket client)
	{
		session.active=true;
		session.alive=true;
		session.user = user;

		session.clientSocket = client;
		try{
			session.connection = new SocketToSocketConnection(session.clientSocket,session.serverSocket,alias,this,TimeConstants.SECOND_MILLIS*15);
			logger.log("Hub session "+alias+" connected for "+user);
		}
		catch(Exception e)
		{
			//XappRootApplication.displayException(e);
			
			try{
				session.clientSocket.close();
			}catch(Exception e2){}
			try{
				session.serverSocket.close();
			}catch(Exception e2){}
			
			return;
		}
		
		if(sessionListener!=null)
		{
			sessionListener.sessionOpened(session.owner,session.user);
		}
	}
	
	public void connectionClosed(Connection connection)
	{
		String alias = connection.getAlias();
		System.out.println("Closing connection "+alias);
		try{
			connection.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		Session session = (Session)sessions.get(alias);
		if(session==null)
		{
			return;
		}
		
		session.active=false;
		session.alive=false;
		
		if(sessionListener!=null)
		{
			sessionListener.sessionClosed(session.owner,session.user);
		}		
	}
	
	public static String readLine(InputStream iStream) throws IOException
	{
		int in = iStream.read();
		StringBuffer buffer = new StringBuffer();
		while(in!=-1 && in!='\n' && buffer.length()<100)
		{
			if(in!=0) // keep alive byte
			{
				buffer.append((char)in);
			}
						
			in = iStream.read();
		}
		
		return buffer.toString();
	}
	
	public static void writeLine(String line,OutputStream outputStream) throws IOException
	{
		outputStream.write( (line+"\n").getBytes() );
		outputStream.flush();				
	}
	
	public boolean authenticate(Login login,String subdomain)
	{	
		login.setPebble(login.getPassword());
		
		login = authenticator.login(login);
		if(login.isLoggedIn()==true)
		{
			AccessCheck check = authenticator.userCanAccess(login,alias,subdomain);
			if(check.isAllowed()==true)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return false;
	}
}
