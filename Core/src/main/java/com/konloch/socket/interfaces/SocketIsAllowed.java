package com.konloch.socket.interfaces;

import java.net.Socket;

/**
 * @author Konloch
 * @since 3/1/2023
 */
public interface SocketIsAllowed
{
	boolean allowed(Socket socket);
}
