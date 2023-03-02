package com.konloch.echo;

import com.konloch.socket.SocketClient;
import com.konloch.socket.SocketServer;

import java.io.IOException;
import java.util.HashMap;

/**
 * An echo server implementation that contains a 1024 character buffer.
 *
 * It will echo-back the contents of the buffer if it has reached 1024 characters, or encountered a carriage return.
 *
 * @author Konloch
 * @since 2/28/2023
 */
public class CREchoServer
{
	private final SocketServer server;
	private final HashMap<Long, ConnectedClientData> connected = new HashMap<>();
	
	public CREchoServer(int port, int threadPool) throws IOException
	{
		server = new SocketServer(port, threadPool,
		
		//accept all connected and don't try to filter any
		client -> true,
		
		//process the client IO
		client ->
		{
			ConnectedClientData data = getConnectedClientData(client);
			
			switch(client.getState())
			{
				//signal we want to start reading into the buffer
				case 0:
					//signal that we want to start reading and to fill up the buffer
					client.setInputRead(true);
					
					//advance to stage 1
					client.setState(1);
					break;
				
				//wait until the stream has signalled the buffer has reached the end
				case 1:
					//when the buffer is full advance to stage 2
					if(!client.isInputRead())
						client.setState(2);
					break;
				
				//echo back
				case 2:
					//get the bytes written
					byte[] bytes = client.getInputBuffer().toByteArray();
					
					//reset the input buffer
					client.getInputBuffer().reset();
					
					try
					{
						data.buffer.write(bytes);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					
					//read the bytes and look for any carriage return
					boolean writeBack = data.buffer.size() >= 1024;
					for(byte b : bytes)
					{
						char c = (char) b;
						if(c == '\n' || c == '\r')
						{
							writeBack = true;
							break;
						}
					}
					
					//echo the bytes back
					if(writeBack)
					{
						client.write(data.buffer.toByteArray());
						data.buffer.reset();
					}
					
					//loop back to stage 0
					client.setState(0);
					break;
			}
		},
		
		//on client disconnect remove the cached data
		client -> connected.remove(client.getUID()));
	}
	
	public ConnectedClientData getConnectedClientData(SocketClient client)
	{
		if(!connected.containsKey(client.getUID()))
			connected.put(client.getUID(), new ConnectedClientData());
		
		return connected.get(client.getUID());
	}
	
	public void start()
	{
		server.start();
	}
	
	public static void main(String[] args)
	{
		try
		{
			CREchoServer echoServer = new CREchoServer(7, 1);
			echoServer.start();
			
			System.out.println("Carriage Return Echo Server running on port " + echoServer.server.getPort());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}