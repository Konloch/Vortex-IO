package com.konloch.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class SocketClient
{
	private final ByteArrayOutputStream inputBuffer = new ByteArrayOutputStream();
	
	//TODO this should be swapped to a ByteArrayOutputStream, however it needs to be able to shrink, so maybe a new type is needed
	private final LinkedList<Byte> outputBuffer = new LinkedList<>();
	
	private final long uid;
	private final SocketChannel socket;
	private final String remoteAddress;
	private long lastNetworkActivity;
	private boolean inputRead;
	private boolean outputWrite;
	private int state;
	
	/**
	 * Construct a new socket client
	 */
	public SocketClient(long uid, SocketChannel socket)
	{
		this.uid = uid;
		this.socket = socket;
		this.lastNetworkActivity = System.currentTimeMillis();
		
		String remoteAddressTMP;
		try
		{
			remoteAddressTMP = socket.getRemoteAddress().toString();
		}
		catch (IOException e)
		{
			remoteAddressTMP = null;
			e.printStackTrace();
		}
		
		this.remoteAddress = remoteAddressTMP;
	}
	
	/**
	 * Write to a byte array the output buffer
	 *
	 * @param bytes any byte array
	 */
	public void write(byte[] bytes)
	{
		//write in reverse order
		for(int i = bytes.length-1; i >= 0; i--)
			getOutputBuffer().push(bytes[i]);
		
		setOutputWrite(true);
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
	 * @return the byte LinkedList representing the output buffer
	 */
	public LinkedList<Byte> getOutputBuffer()
	{
		return outputBuffer;
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
