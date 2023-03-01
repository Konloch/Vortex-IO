package com.konloch.echo;

import com.konloch.socket.SocketServer;

import java.io.IOException;

/**
 * RFC-862 compliant echo server.
 *
 * @author Konloch
 * @since 2/28/2023
 */
public class EchoServer extends SocketServer
{
	public EchoServer(int port) throws IOException
	{
		super(port, client ->
		{
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
					
					//echo the bytes back
					client.write(bytes);
					
					//loop back to stage 0
					client.setState(0);
					break;
			}
		});
	}
	
	public static void main(String[] args)
	{
		try
		{
			EchoServer server = new EchoServer(7);
			server.start();
			
			System.out.println("Echo Server running on port " + server.getPort());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}