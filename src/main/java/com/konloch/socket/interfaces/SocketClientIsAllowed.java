package com.konloch.socket.interfaces;

import com.konloch.socket.SocketClient;

/**
 * @author Konloch
 * @since 3/1/2023
 */
public interface SocketClientIsAllowed
{
	boolean allowed(SocketClient client);
}
