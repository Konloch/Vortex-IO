package com.konloch;

import com.konloch.vortex.Server;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class TestSocketServer
{
	//change this value to increase the amount of concurrent clients
	private static final int CLIENT_TEST_THREADS = 2;
	
	//change this value to adjust the time till timeout in ms
	private static final int CLIENT_TIMEOUT = 7000;
	
	//change this value to increase the amount of server processing threads
	private static final int SERVER_THREAD_POOL = 2;
	
	//change this value to adjust the time till timeout in ms
	private static final int SERVER_TIMEOUT = 30_000;
	
	private static long lastDataTransferBPS;
	private static AtomicLong totalAmountOfDataTransferred = new AtomicLong();
	private static AtomicLong dataTransferBPS = new AtomicLong();
	private static AtomicLong lastTransfer = new AtomicLong();
	
	public static void main(String[] args)
	{
		Server server = setupServer();
		server.setTimeout(SERVER_TIMEOUT);
		testServer(server.getPort(), CLIENT_TEST_THREADS);
		
		while(true)
		{
			try
			{
				Thread.sleep(10);
				
				int connectedClientsAmount = 0;
				for(int i = 0; i < SERVER_THREAD_POOL; i++)
					connectedClientsAmount += server.getClients(i).size();
				
				System.out.println("Connected Clients: " + connectedClientsAmount + ", Total Bytes Transferred: " + totalAmountOfDataTransferred + ", Bytes Transferred Per Second: " + lastDataTransferBPS);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static Server setupServer()
	{
		Server server = new Server(1111, SERVER_THREAD_POOL, null, client ->
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
		return server;
	}
	
	private static void testServer(int port, int threads)
	{
		for(int i = 0; i < threads; i++)
		{
			new Thread(()->{
				while(true)
				{
					try
					{
						testConnection(port);
					}
					catch (Throwable e)
					{
						//ignore exceptions and wait 100 ms
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
		
		long readTimer = System.currentTimeMillis();
		while(socket.getInputStream().available() <= len && System.currentTimeMillis()-readTimer <= CLIENT_TIMEOUT)
		{
			Thread.sleep(1);
		}
		
		int read;
		while(socket.getInputStream().available() > 0
				&& (read = socket.getInputStream().read()) > 0)
		{
			if(System.currentTimeMillis()-lastTransfer.get() >= 1000)
			{
				lastDataTransferBPS = dataTransferBPS.get();
				lastTransfer.set(System.currentTimeMillis());
				dataTransferBPS.set(0);
			}
			
			//log two bytes per character we read
			totalAmountOfDataTransferred.addAndGet(2);
			dataTransferBPS.addAndGet(2);
			
			//enable for echo output
			//System.out.print((char) read);
		}
		
		socket.close();
		
		//enable for echo output
		//System.out.println();
	}
}
