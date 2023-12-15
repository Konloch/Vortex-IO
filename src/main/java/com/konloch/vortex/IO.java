package com.konloch.vortex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Konloch
 * @since 3/1/2023
 */
class IO implements Runnable
{
	private final Server server;
	private final List<Client> clients = new ArrayList<>();
	
	/**
	 * Construct a new SocketServerIO
	 * @param server the SocketServer this IO Handler is bound to
	 */
	public IO(Server server)
	{
		this.server = server;
	}
	
	/**
	 * Process the IO using blocking IO functions, reading doesn't block but writing does
	 */
	@Override
	public void run()
	{
		ByteBuffer buffer = ByteBuffer.allocate(server.getIOAmount());
		while (server.isRunning())
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
					if(client == null)
						return true;
					
					boolean remove = !client.getSocket().isConnected();
					
					if (remove && server.getOnDisconnect() != null)
						server.getOnDisconnect().run(client);
					
					return remove;
				});
				
				//iterate thru all clients
				for (Client client : clients)
				{
					if(client == null)
						continue;
					
					try
					{
						//if the client has been disconnected, do not try to process anything
						if (!client.getSocket().isConnected())
							continue;

						//timeout if there is no network activity
						if (Math.min(now - client.getLastNetworkActivityWrite(),
								now - client.getLastNetworkActivityRead()) > server.getTimeout())
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
						
						((java.nio.Buffer) buffer).clear();
						
						//processing writing (only write when asked to)
						if (client.isOutputWrite())
						{
							boolean fromCache = client.getOutputBufferCache() != null;
							if (client.getOutputBuffer().size() > 0 || fromCache)
							{
								int offset = client.getOutputBufferProgress();
								
								int readMax = server.getIOAmount();
								
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
								((java.nio.Buffer) buffer).flip();
								
								//reset the network activity
								client.resetLastNetworkActivityWrite();
								
								//sent the buffer
								client.getSocket().write(buffer);
								
								//clear the buffer
								((java.nio.Buffer) buffer).clear();
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
						server.getRequestHandler().run(client);
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
	 * Return the socket client list containing the connected clients
	 * @return the Socket Client list containing the connected clients
	 */
	public List<Client> getClients()
	{
		return clients;
	}
}
