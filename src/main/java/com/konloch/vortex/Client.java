package com.konloch.vortex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class Client
{
	private final ByteArrayOutputStream inputBuffer = new ByteArrayOutputStream();
	private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
	private final Server server;
	private final SocketChannel socket;
	private final long uid;
	private final String remoteAddress;
	private long lastNetworkActivityRead;
	private long lastNetworkActivityWrite;
	private boolean inputRead = true;
	private boolean outputWrite;
	private int state;
	private int outputBufferProgress;
	private byte[] outputBufferCache;
	
	/**
	 * Construct a new socket client
	 * @param server the socket server this client will be bound to
	 * @param socket the socket channel this client is using for communication
	 * @param uid the unique user identifier this socket client is assigned
	 */
	public Client(Server server, SocketChannel socket, long uid)
	{
		this.uid = uid;
		this.socket = socket;
		this.server = server;
		this.lastNetworkActivityRead = this.lastNetworkActivityWrite = System.currentTimeMillis();
		this.remoteAddress = resolveRemoteAddress();
	}
	
	/**
	 * Write to a byte array the output buffer
	 * @param bytes any byte array
	 */
	public void write(byte[] bytes)
	{
		try
		{
			getOutputBuffer().write(bytes);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		setOutputWrite(true);
	}
	
	/**
	 * Returns the output buffer write progress
	 * @return the output buffer write progress
	 */
	public int getOutputBufferProgress()
	{
		return outputBufferProgress;
	}
	
	/**
	 * Adds to the output buffer progress and then returns the current value
	 * @param add the amount of progress to increase by
	 * @return the output buffer write progress
	 */
	public int outputBufferProgress(int add)
	{
		outputBufferProgress += add;
		return outputBufferProgress;
	}
	
	/**
	 * Resets the output buffer state
	 */
	public void resetOutputBuffer()
	{
		outputBufferCache = null;
		outputBufferProgress = 0;
		getOutputBuffer().reset();
	}
	
	/**
	 * Reset the last read network activity
	 */
	protected void resetLastNetworkActivityRead()
	{
		lastNetworkActivityRead = System.currentTimeMillis();
	}
	
	/**
	 * Reset the last write network activity
	 */
	protected void resetLastNetworkActivityWrite()
	{
		lastNetworkActivityWrite = System.currentTimeMillis();
	}
	
	/**
	 * Returns the unique user id for this specific connection
	 * @return the unique user id for this specific connection
	 */
	public long getUID()
	{
		return uid;
	}
	
	/**
	 * Returns the remote address for this socket
	 * @return a String containing the remote address for the socket
	 */
	public String getRemoteAddress()
	{
		return remoteAddress;
	}
	
	/**
	 * Returns the last read network activity for this socket
	 * @return a long representing the timestamp of the last time the socket read
	 */
	public long getLastNetworkActivityRead()
	{
		return lastNetworkActivityRead;
	}
	
	/**
	 * Returns the last write network activity for this socket
	 * @return a long representing the timestamp of the last time the socket writes
	 */
	public long getLastNetworkActivityWrite()
	{
		return lastNetworkActivityWrite;
	}
	
	/**
	 * Returns true if the socket client is in the read state
	 * @return true if the socket client is in the read state
	 */
	public boolean isInputRead()
	{
		return inputRead;
	}
	
	/**
	 * Set the read state for the socket client
	 * @param inputRead set true to enable the read state for the socket client
	 */
	public void setInputRead(boolean inputRead)
	{
		this.inputRead = inputRead;
	}
	
	/**
	 * Returns true if the socket client is in the write state
	 * @return true if the socket client is in the write state
	 */
	public boolean isOutputWrite()
	{
		return outputWrite;
	}
	
	/**
	 * Set the write state for the socket client
	 * @param outputWrite set true to enable the write state for the socket client
	 */
	public void setOutputWrite(boolean outputWrite)
	{
		this.outputWrite = outputWrite;
	}
	
	/**
	 * Get the socket state
	 * @return an integer representing the socket state
	 */
	public int getState()
	{
		return state;
	}
	
	/**
	 * Set the state for the socket
	 * @param state any integer representing the socket state
	 */
	public void setState(int state)
	{
		this.state = state;
	}
	
	/**
	 * Returns the input buffer
	 * @return the ByteArrayOutputStream representing the input buffer
	 */
	public ByteArrayOutputStream getInputBuffer()
	{
		return inputBuffer;
	}
	
	/**
	 * Returns the output buffer
	 * @return the ByteArrayOutputStream representing the output buffer
	 */
	public ByteArrayOutputStream getOutputBuffer()
	{
		return outputBuffer;
	}
	
	/**
	 * Return the output buffer cache
	 * @return the byte array representing the output buffer cache
	 */
	public byte[] getOutputBufferCache()
	{
		return outputBufferCache;
	}
	
	/**
	 * Set the output buffer cache
	 * @param outputBufferCache any byte array as the buffer cache
	 */
	public void setOutputBufferCache(byte[] outputBufferCache)
	{
		this.outputBufferCache = outputBufferCache;
		setOutputWrite(true);
	}
	
	/**
	 * Returns the bound NIO socket channel
	 * @return the bound NIO socket channel
	 */
	public SocketChannel getSocket()
	{
		return socket;
	}
	
	/**
	 * Returns the socket server this client is bound to
	 * @return the socket server this client is bound to
	 */
	public Server getServer()
	{
		return server;
	}
	
	/**
	 * Resolve the remote address
	 * @return the remote address, or an empty string if that failed
	 */
	private String resolveRemoteAddress()
	{
		if(socket == null)
			return ""; //return empty instead of null
		
		Socket javaSocket = socket.socket();
		
		if(javaSocket == null)
			return ""; //return empty instead of null
		
		return javaSocket.toString().replace("/","");
	}
}