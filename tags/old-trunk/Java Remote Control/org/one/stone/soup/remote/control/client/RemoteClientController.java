package org.one.stone.soup.remote.control.client;

import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;

public interface RemoteClientController extends KeyListener{

	public void actionPerformed( ActionEvent actionEvent );
	public void requestNextFrame();
}
