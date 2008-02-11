package org.one.stone.soup.remote.control.hub;

public interface SessionListener {

	public void sessionOpened(String owner,String user);
	
	public void sessionClosed(String owner,String user);
	
	public void logData( int sessionCount,long dataSent,long maxDataSize );
	
}
