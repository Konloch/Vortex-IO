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
	private final SocketServerIOHandler[] threadPool;
	private SocketClientIsAllowed networkConnectionFilter;
	private SocketClientRunnable requestHandler;
	private SocketClientRunnable onDisconnect;
	private int threadPoolCounter;
	private boolean running;
	private int ioAmount = 1024;
	private int timeout = 30_000;
	private long uidCounter;
	
	/**
	 * Construct a new Socket Server
	 *
	 * @param port any port between 0-65,535
	 * @param requestHandler the request handler
	 * @throws IOException thrown if any IO issues are encountered.
	 */
	public SocketServer(int port, SocketClientRunnable requestHandler) throws IOException
	{
		this(port, 1, null, requestHandler, null);
	}
	
	/**
	 * Construct a new Socket Server
	 *
	 * @param port any port between 0-65,535
	 * @param threadPool the amount of threads that will be started
	 * @param networkConnectionFilter the pre-requst filter
	 * @param requestHandler the request handler
	 * @param onDisconnect called any time the client disconnects
	 * @throws IOException thrown if any IO issues are encountered.
	 */
	public SocketServer(int port, int threadPool, SocketClientIsAllowed networkConnectionFilter,
	                    SocketClientRunnable requestHandler, SocketClientRunnable onDisconnect) throws IOException
	{
		this.port = port;
		this.server = ServerSocketChannel.open();
		this.threadPool = new SocketServerIOHandler[threadPool];
		this.networkConnectionFilter = networkConnectionFilter;
		this.requestHandler = requestHandler;
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
			SocketServerIOHandler socketIO = new SocketServerIOHandler(this);
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
				if(networkConnectionFilter == null || networkConnectionFilter.allowed(client))
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
	
	/**
	 * Returns the port the socket server is bound to
	 *
	 * @return the port the socket server is bound to
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * Returns true if the socket server is still running
	 *
	 * @return true if the socket server is still running
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	/**
	 * Stops the socket server
	 *
	 * @return this instance for method chaining
	 */
	public SocketServer stopSocketServer()
	{
		running = false;
		return this;
	}
	
	/**
	 * Returns true if the socket server has been stopped
	 *
	 * @return true if the socket server has been stopped
	 */
	public boolean hasStopped()
	{
		return !running;
	}
	
	/**
	 * Returns the request handler
	 *
	 * @return the request handler
	 */
	public SocketClientRunnable getRequestHandler()
	{
		return requestHandler;
	}
	
	/**
	 * Set the request handler
	 *
	 * @param requestHandler any request handler
	 * @return this instance for method chaining
	 */
	public SocketServer setRequestHandler(SocketClientRunnable requestHandler)
	{
		this.requestHandler = requestHandler;
		return this;
	}
	
	/**
	 * Returns the onDisconnect handler
	 *
	 * @return the onDisconnect handler
	 */
	public SocketClientRunnable getOnDisconnect()
	{
		return onDisconnect;
	}
	
	/**
	 * Set the onDisconnect handler
	 *
	 * @param onDisconnect any onDisconnet handler
	 * @return this instance for method chaining
	 */
	public SocketServer setOnDisconnect(SocketClientRunnable onDisconnect)
	{
		this.onDisconnect = onDisconnect;
		return this;
	}
	
	/**
	 * Returns the network connection filter
	 *
	 * @return the network connection filder
	 */
	public SocketClientIsAllowed getNetworkConnectionFilter()
	{
		return networkConnectionFilter;
	}
	
	/**
	 * Sets the network connection filter
	 * @param networkConnectionFilter any network connection filter
	 * @return this instance for method chaining
	 */
	public SocketServer setNetworkConnectionFilter(SocketClientIsAllowed networkConnectionFilter)
	{
		this.networkConnectionFilter = networkConnectionFilter;
		return this;
	}
	
	/**
	 * Returns the Socket Client set for the supplied thread pool index
	 * @param index any integer to represent the thread pool index
	 * @return the Socket Client Set for the supplied thread pool index
	 */
	public Set<SocketClient> getClients(int index)
	{
		return threadPool[index].getClients();
	}
	
	/**
	 * Returns the timeout value in milliseconds for network inactivity
	 * @return the timeout value in milliseconds for network inactivity
	 */
	public int getTimeout()
	{
		return timeout;
	}
	
	/**
	 * Set the timeout value for network activity
	 *
	 * @param timeout any integer representing the milliseconds for timeout from network activity
	 * @return this instance for method chaining
	 */
	public SocketServer setTimeout(int timeout)
	{
		this.timeout = timeout;
		return this;
	}
	
	/**
	 * Return the default size of the byte buffers
	 *
	 * @return default size of the byte buffers
	 */
	public int getIOAmount()
	{
		return ioAmount;
	}
	
	/**
	 * Set the default size of the byte buffers
	 *
	 * @param ioAmount any integer as the default size of the byte buffers
	 * @return this instance for method chaining
	 */
	public SocketServer setIOAmount(int ioAmount)
	{
		this.ioAmount = ioAmount;
		
		return this;
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
