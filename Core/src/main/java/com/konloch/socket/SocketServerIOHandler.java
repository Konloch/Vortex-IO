package com.konloch.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Konloch
 * @since 3/1/2023
 */
class SocketServerIOHandler implements Runnable
{
	private final SocketServer socketServer;
	private final Set<SocketClient> clients = new HashSet<>();
	
	/**
	 * Construct a new SocketServerIO
	 *
	 * @param socketServer the SocketServer this IO Handler is bound to
	 */
	public SocketServerIOHandler(SocketServer socketServer)
	{
		this.socketServer = socketServer;
	}
	
	/**
	 * Process the IO using blocking IO functions, reading doesn't block but writing does
	 */
	@Override
	public void run()
	{
		ByteBuffer buffer = ByteBuffer.allocate(socketServer.getIOAmount());
		while (socketServer.isRunning())
		{
			try
			{
				if (clients.isEmpty())
				{
					//sleep only while the server is not processing data
					try
					{
						Thread.sleep(1);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					
					continue;
				}
				
				long now = System.currentTimeMillis();
				
				//remove any clients not connected
				clients.removeIf(client ->
				{
					boolean remove = !client.getSocket().isConnected();
					
					if (remove && socketServer.getOnDisconnect() != null)
						socketServer.getOnDisconnect().run(client);
					
					return remove;
				});
				
				//iterate thru all clients
				for (SocketClient client : clients)
				{
					try
					{
						//if the client has been disconnected, do not try to process anything
						if (!client.getSocket().isConnected())
							continue;

						//timeout if there is no network activity
						if (Math.min(now - client.getLastNetworkActivityWrite(),
								now - client.getLastNetworkActivityRead()) > socketServer.getTimeout())
						{
							client.getSocket().close();
							continue;
						}
						
						//process reading (always in the reading state unless disconnected)
						client.getSocket().read(buffer);
						
						if (buffer.position() > 0)
						{
							client.resetLastNetworkActivityRead();
							client.getInputBuffer().write(buffer.array(), 0, buffer.position());
						}
						else
							client.setInputRead(false);
						
						buffer.clear();
						
						//processing writing (only write when asked to)
						if (client.isOutputWrite())
						{
							boolean fromCache = client.getOutputBufferCache() != null;
							if (client.getOutputBuffer().size() > 0 || fromCache)
							{
								int offset = client.getOutputBufferProgress();
								
								int readMax = socketServer.getIOAmount();
								
								if(!fromCache)
									client.setOutputBufferCache(client.getOutputBuffer().toByteArray());
								
								//dump the buffer
								byte[] bufferDump = client.getOutputBufferCache();
								
								if(offset+readMax > bufferDump.length)
									readMax = bufferDump.length - offset;
								
								//clear the buffer
								if(client.outputBufferProgress(readMax) >= bufferDump.length)
									client.resetOutputBuffer();
								
								//write what we can to the socket
								buffer.put(bufferDump, offset, readMax);
								
								//flip the stored data
								buffer.flip();
								
								//reset the network activity
								client.resetLastNetworkActivityWrite();
								
								//sent the buffer
								client.getSocket().write(buffer);
								
								//clear the buffer
								buffer.clear();
							}
							else
							{
								client.setOutputWrite(false);
							}
						}
					}
					catch (IOException e)
					{
						//ignore IO exceptions as they get thrown often
						//e.printStackTrace();
						
						try
						{
							client.getSocket().close();
						}
						catch (Exception ex)
						{
							e.printStackTrace();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					try
					{
						socketServer.getRequestHandler().run(client);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			catch (ConcurrentModificationException e)
			{
				//ignore concurrent modifications, just run it again
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Return the Socket Client Set containing the connected clients
	 *
	 * @return the Socket Client set containing the connected clients
	 */
	public Set<SocketClient> getClients()
	{
		return clients;
	}
}
