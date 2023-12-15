package com.konloch;

import com.konloch.vortex.Server;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class TestSocketServer
{
	public static void main(String[] args)
	{
		Server server = new Server(1111, 2, null, client ->
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
					
				//announce the read
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
		}, client -> {});
		server.start();
		
		for(int i = 0; i < 2; i++)
		{
			new Thread(()->{
				while(true)
				{
					try
					{
						testConnection(server.getPort());
					}
					catch (Exception e)
					{
						e.printStackTrace();
						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException ex)
						{
							ex.printStackTrace();
						}
					}
				}
			}).start();
		}
	}
	
	private static void testConnection(int port) throws Exception
	{
		String message = "Hello World";
		int len = message.length()-1;
		
		Socket socket = new Socket("localhost", port);
		socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
		socket.getOutputStream().flush();
		
		long giveUP = System.currentTimeMillis();
		while(socket.getInputStream().available() <= len && System.currentTimeMillis()-giveUP <=7000)
		{
			Thread.sleep(1);
		}
		
		int read;
		while(socket.getInputStream().available() > 0
				&& (read = socket.getInputStream().read()) > 0)
		{
			System.out.print((char) read);
		}
		
		socket.close();
		
		System.out.println();
	}
}
