package com.konloch.vortex;

import com.konloch.vortex.interfaces.IsAllowed;
import com.konloch.vortex.interfaces.ClientRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public class Server extends Thread
{
	private final String hostname;
	private final int port;
	private ServerSocketChannel server;
	private final IO[] threadPool;
	private IsAllowed networkConnectionFilter;
	private ClientRunnable requestHandler;
	private ClientRunnable onDisconnect;
	private int threadPoolCounter;
	private boolean running;
	private boolean bound;
	private int ioAmount = 1024;
	private int timeout = 30_000;
	private long uidCounter;
	
	/**
	 * Construct a new Socket Server
	 * @param port any port between 0-65,535
	 * @param requestHandler the request handler
	 * @throws IOException thrown if any IO issues are encountered.
	 */
	public Server(int port, ClientRunnable requestHandler) throws IOException
	{
		this(port, 1, null, requestHandler, null);
	}
	
	/**
	 * Construct a new Socket Server
	 * @param port any port between 0-65,535
	 * @param threadPool the amount of threads that will be started
	 * @param networkConnectionFilter the pre-requst filter
	 * @param requestHandler the request handler
	 * @param onDisconnect called any time the client disconnects
	 */
	public Server(int port, int threadPool, IsAllowed networkConnectionFilter,
	              ClientRunnable requestHandler, ClientRunnable onDisconnect)
	{
		this("0.0.0.0", port, threadPool, networkConnectionFilter, requestHandler, onDisconnect);
	}

	/**
	 * Construct a new Socket Server
	 * @param hostname the hostname the socket will bind to
	 * @param port any port between 0-65,535
	 * @param threadPool the amount of threads that will be started
	 * @param networkConnectionFilter the pre-requst filter
	 * @param requestHandler the request handler
	 * @param onDisconnect called any time the client disconnects
	 */
	public Server(String hostname, int port, int threadPool, IsAllowed networkConnectionFilter,
	              ClientRunnable requestHandler, ClientRunnable onDisconnect)
	{
		this.hostname = hostname;
		this.port = port;
		this.threadPool = new IO[threadPool];
		this.networkConnectionFilter = networkConnectionFilter;
		this.requestHandler = requestHandler;
		this.onDisconnect = onDisconnect;
	}
	
	/**
	 * Bind to the socket port
	 * @return this instance for method chaining
	 * @throws IOException thrown if any IO issues are encountered.
	 */
	public Server bind() throws IOException
	{
		if(bound)
			return this;
		
		this.server = ServerSocketChannel.open();
		//bind and configure non-blocking
		server.bind(new InetSocketAddress(hostname, port));
		server.configureBlocking(false);
		bound = true;
		return this;
	}
	
	/**
	 * Starts the thread pool and waits for all incoming connections
	 */
	@Override
	public void run()
	{
		//attempt to auto bind on initial start
		if(!bound)
		{
			try
			{
				bind();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if(running)
			return;
		
		running = true;
		
		for(int i = 0; i < threadPool.length; i++)
		{
			IO socketIO = new IO(this);
			new Thread(threadPool[i] = socketIO).start();
		}
		
		while(running)
		{
			try
			{
				//accept connection is a non-blocking call.
				//it will only allow the thread to rest if an incoming connection did not queue
				//this keeps the socket server ready for burst connections but able to rest when there are none coming in
				if(!acceptConnection())
				{
					try
					{
						Thread.sleep(1);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Attempts to accept an incoming socket connection, if there is not one in the queue it will return false.
	 * @return true if a connection has been accepted
	 * @throws IOException thrown if any IO issues are encountered.
	 */
	public boolean acceptConnection() throws IOException
	{
		SocketChannel channel = server.accept();
		
		if(channel == null)
			return false;
		
		//enable nio
		channel.configureBlocking(false);
		
		//build the socket client instance
		Client client = new Client(this, channel, uidCounter++);
		
		//verify the socket client is allowed in
		if(networkConnectionFilter == null || networkConnectionFilter.allowed(client))
		{
			//TODO thread pool should be assigned to the thread pool with the lowest amount of clients
			threadPool[threadPoolCounter++].getClients().add(client);
			
			if (threadPoolCounter >= threadPool.length)
				threadPoolCounter = 0;
		}
		else
		{
			try
			{
				client.getSocket().close();
				client.getSocket().socket().close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(getOnDisconnect() != null)
					getOnDisconnect().run(client);
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the port the socket server is bound to
	 * @return the port the socket server is bound to
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * Returns true if the socket server is still running
	 * @return true if the socket server is still running
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	/**
	 * Stops the socket server
	 * @return this instance for method chaining
	 */
	public Server stopSocketServer()
	{
		running = false;
		return this;
	}
	
	/**
	 * Returns true if the socket server has been stopped
	 * @return true if the socket server has been stopped
	 */
	public boolean hasStopped()
	{
		return !running;
	}
	
	/**
	 * Returns the request handler
	 * @return the request handler
	 */
	public ClientRunnable getRequestHandler()
	{
		return requestHandler;
	}
	
	/**
	 * Set the request handler
	 * @param requestHandler any request handler
	 * @return this instance for method chaining
	 */
	public Server setRequestHandler(ClientRunnable requestHandler)
	{
		this.requestHandler = requestHandler;
		return this;
	}
	
	/**
	 * Returns the onDisconnect handler
	 * @return the onDisconnect handler
	 */
	public ClientRunnable getOnDisconnect()
	{
		return onDisconnect;
	}
	
	/**
	 * Set the onDisconnect handler
	 * @param onDisconnect any onDisconnet handler
	 * @return this instance for method chaining
	 */
	public Server setOnDisconnect(ClientRunnable onDisconnect)
	{
		this.onDisconnect = onDisconnect;
		return this;
	}
	
	/**
	 * Returns the network connection filter
	 * @return the network connection filter
	 */
	public IsAllowed getNetworkConnectionFilter()
	{
		return networkConnectionFilter;
	}
	
	/**
	 * Sets the network connection filter
	 * @param networkConnectionFilter any network connection filter
	 * @return this instance for method chaining
	 */
	public Server setNetworkConnectionFilter(IsAllowed networkConnectionFilter)
	{
		this.networkConnectionFilter = networkConnectionFilter;
		return this;
	}
	
	/**
	 * Returns the socket client list for the supplied thread pool index
	 * @param index any integer to represent the thread pool index
	 * @return the socket client list for the supplied thread pool index
	 */
	public List<Client> getClients(int index)
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
	 * @param timeout any integer representing the milliseconds for timeout from network activity
	 * @return this instance for method chaining
	 */
	public Server setTimeout(int timeout)
	{
		this.timeout = timeout;
		return this;
	}
	
	/**
	 * Return the default size of the byte buffers
	 * @return default size of the byte buffers
	 */
	public int getIOAmount()
	{
		return ioAmount;
	}
	
	/**
	 * Set the default size of the byte buffers
	 * @param ioAmount any integer as the default size of the byte buffers
	 * @return this instance for method chaining
	 */
	public Server setIOAmount(int ioAmount)
	{
		this.ioAmount = ioAmount;
		
		return this;
	}
	
	/**
	 * Alert that this is a library
	 * @param args program launch arguments
	 */
	public static void main(String[] args)
	{
		throw new RuntimeException("Incorrect usage - for information on how to use this correctly visit https://konloch.com/Vortex-IO/");
	}
}
