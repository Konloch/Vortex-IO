package com.konloch.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class SocketClient
{
	private final ByteArrayOutputStream inputBuffer = new ByteArrayOutputStream();
	private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
	private final long uid;
	private final SocketChannel socket;
	private final String remoteAddress;
	private long lastNetworkActivity;
	private boolean inputRead = true;
	private boolean outputWrite;
	private int state;
	private int outputBufferProgress;
	private byte[] outputBufferCache;
	
	/**
	 * Construct a new socket client
	 */
	public SocketClient(long uid, SocketChannel socket)
	{
		this.uid = uid;
		this.socket = socket;
		this.lastNetworkActivity = System.currentTimeMillis();
		this.remoteAddress = socket.socket().getInetAddress().toString().replace("/","");
	}
	
	/**
	 * Write to a byte array the output buffer
	 *
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
	 *
	 * @return the output buffer write progress
	 */
	public int getOutputBufferProgress()
	{
		return outputBufferProgress;
	}
	
	/**
	 * Adds to the output buffer progress and then returns the current value
	 *
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
	 * Reset the last network activity
	 */
	protected void resetLastNetworkActivity()
	{
		lastNetworkActivity = System.currentTimeMillis();
	}
	
	/**
	 * Returns the unique user id for this specific connection
	 *
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
	 * Returns the last network activity for this socket
	 *
	 * @return a long representing the timestamp of the last time the socket read or wrote
	 */
	public long getLastNetworkActivity()
	{
		return lastNetworkActivity;
	}
	
	/**
	 * Returns true if the socket client is in the read state
	 *
	 * @return true if the socket client is in the read state
	 */
	public boolean isInputRead()
	{
		return inputRead;
	}
	
	/**
	 * Set the read state for the socket client
	 *
	 * @param inputRead set true to enable the read state for the socket client
	 */
	public void setInputRead(boolean inputRead)
	{
		this.inputRead = inputRead;
	}
	
	/**
	 * Returns true if the socket client is in the write state
	 *
	 * @return true if the socket client is in the write state
	 */
	public boolean isOutputWrite()
	{
		return outputWrite;
	}
	
	/**
	 * Set the write state for the socket client
	 *
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
	 *
	 * @return the ByteArrayOutputStream representing the input buffer
	 */
	public ByteArrayOutputStream getInputBuffer()
	{
		return inputBuffer;
	}
	
	/**
	 * Returns the output buffer
	 *
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
	 *
	 * @param outputBufferCache any byte array as the buffer cache
	 */
	public void setOutputBufferCache(byte[] outputBufferCache)
	{
		this.outputBufferCache = outputBufferCache;
		setOutputWrite(true);
	}
	
	/**
	 * Returns the bound NIO socket channel
	 *
	 * @return the bound NIO socket channel
	 */
	public SocketChannel getSocket()
	{
		return socket;
	}
}
