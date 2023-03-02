package com.konloch.socket;

import com.konloch.socket.interfaces.SocketClientIsAllowed;
import com.konloch.socket.interfaces.SocketClientRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class SocketServer extends Thread
{
	private final int port;
	private final ServerSocketChannel server;
	private final SocketServerIO[] threadPool;
	private SocketClientIsAllowed canConnect;
	private SocketClientRunnable onProcess;
	private SocketClientRunnable onDisconnect;
	private int threadPoolCounter;
	private boolean running;
	private int ioAmount = 256;
	private int timeout = 30_000;
	private long uidCounter;
	
	public SocketServer(int port, SocketClientRunnable onProcess) throws IOException
	{
		this(port, 1, null, onProcess, null);
	}
	
	public SocketServer(int port, int threadPool, SocketClientIsAllowed canConnect,
	                    SocketClientRunnable onProcess, SocketClientRunnable onDisconnect) throws IOException
	{
		this.port = port;
		this.server = ServerSocketChannel.open();
		this.threadPool = new SocketServerIO[threadPool];
		this.canConnect = canConnect;
		this.onProcess = onProcess;
		this.onDisconnect = onDisconnect;
		
		//bind and configure non-blocking
		server.bind(new InetSocketAddress("localhost", port));
		server.configureBlocking(false);
	}
	
	/**
	 * Starts the thread pool and waits for all incoming connections
	 */
	@Override
	public void run()
	{
		if(running)
			return;
		
		running = true;
		
		for(int i = 0; i < threadPool.length; i++)
		{
			SocketServerIO socketIO = new SocketServerIO(this);
			new Thread(threadPool[i] = socketIO).start();
		}
		
		while(running)
		{
			try
			{
				SocketChannel channel = server.accept();
				if(channel == null)
					continue;
				
				//enable nio
				channel.configureBlocking(false);
				
				//build the socket client instance
				SocketClient client = new SocketClient(uidCounter++, channel);
				
				//verify the socket client is allowed in
				if(canConnect == null || canConnect.allowed(client))
				{
					//TODO thread pool should be assigned to the thread pool with the lowest amount of clients
					threadPool[threadPoolCounter++].getClients().add(client);
					
					if (threadPoolCounter >= threadPool.length)
						threadPoolCounter = 0;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public int getPort()
	{
		return port;
	}
	
	public boolean isRunning()
	{
		return running;
	}
	
	public SocketServer stopSocketServer()
	{
		running = false;
		return this;
	}
	
	public boolean hasStopped()
	{
		return !running;
	}
	
	public SocketClientRunnable getOnProcess()
	{
		return onProcess;
	}
	
	public SocketServer setOnProcess(SocketClientRunnable onProcess)
	{
		this.onProcess = onProcess;
		return this;
	}
	
	public SocketClientRunnable getOnDisconnect()
	{
		return onDisconnect;
	}
	
	public SocketServer setOnDisconnect(SocketClientRunnable onDisconnect)
	{
		this.onDisconnect = onDisconnect;
		return this;
	}
	
	public SocketClientIsAllowed getCanConnect()
	{
		return canConnect;
	}
	
	public SocketServer setCanConnect(SocketClientIsAllowed canConnect)
	{
		this.canConnect = canConnect;
		return this;
	}
	
	public Set<SocketClient> getClients(int index)
	{
		return threadPool[index].getClients();
	}
	
	public SocketServer setTimeout(int timeout)
	{
		this.timeout = timeout;
		return this;
	}
	
	public int getTimeout()
	{
		return timeout;
	}
	
	public SocketServer setIOAmount(int ioAmount)
	{
		this.ioAmount = ioAmount;
		
		return this;
	}
	
	public int getIOAmount()
	{
		return ioAmount;
	}
	
	/**
	 * Alert that this is a library
	 *
	 * @param args program launch arguments
	 */
	public static void main(String[] args)
	{
		throw new RuntimeException("Incorrect usage - for information on how to use this correctly visit https://konloch.com/Socket-Server/");
	}
}
