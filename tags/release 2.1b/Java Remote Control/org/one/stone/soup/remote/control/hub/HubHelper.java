package org.one.stone.soup.remote.control.hub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HubHelper {

	public static String login(String subdomain,Socket socket,String userName,String password) throws IOException
	{
		return login(subdomain,socket.getOutputStream(),socket.getInputStream(),userName,password);
	}
	
	public static String login(String subdomain,OutputStream oStream,InputStream iStream,String userName,String password) throws IOException
	{
		System.out.println("Logging into hub. Sending "+subdomain);		
		writeLine(subdomain,oStream);
		System.out.println("Waiting for response.");		
		String line = readLine(iStream);
		System.out.println("Received "+line);		
					
		System.out.println("Sending alias "+userName);		
		writeLine( userName+":"+password,oStream );
		
		System.out.println("Waiting for response.");		
		line = readLine(iStream);
		System.out.println("Received "+line);		
		if(line.equals("BYE"))
		{
			iStream.close();
			oStream.close();
			System.out.println("Login to hub failed.");
			return null;
		}
		else if(( line.indexOf("HELLO ")==0 ))
		{
			String serverKey = line.substring( line.indexOf("HELLO ")+6 );
			System.out.println("Logged into hub.");
			return serverKey;
		}
		iStream.close();
		oStream.close();
		System.out.println("Login to hub failed.");
		return null;
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
		
		System.out.println("< "+buffer.toString());
		
		return buffer.toString();
	}

	public static void writeLine(String line,OutputStream outputStream) throws IOException
	{
		outputStream.write( (line+"\n").getBytes() );
		outputStream.flush();
		System.out.println("> "+line);
	}	
	
}
