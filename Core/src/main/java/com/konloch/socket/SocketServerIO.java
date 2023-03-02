package com.konloch.socket;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Konloch
 * @since 3/1/2023
 */
class SocketServerIO implements Runnable
{
	private final SocketServer socketServer;
	private final Set<SocketClient> clients = new HashSet<>();
	
	public SocketServerIO(SocketServer socketServer)
	{
		this.socketServer = socketServer;
	}
	
	/**
	 * Process the IO using blocking IO functions, reading doesn't block but writing does
	 */
	@Override
	public void run()
	{
		while (socketServer.isRunning())
		{
			try
			{
				if (clients.isEmpty())
				{
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
						if (now - client.getLastNetworkActivity() > socketServer.getTimeout())
						{
							client.getSocket().close();
							continue;
						}
						
						//process reading
						if (client.isInputRead() && client.getSocket().isOpen())
						{
							ByteBuffer buffer = ByteBuffer.allocate(socketServer.getIOAmount());
							client.getSocket().read(buffer);
							
							if (buffer.position() > 0)
								client.getInputBuffer().write(buffer.array(), 0, buffer.position());
							else
								client.setInputRead(false);
							
							buffer.clear();
						}
						
						//processing writing
						if (client.isOutputWrite())
						{
							ByteBuffer buffer = ByteBuffer.allocate(socketServer.getIOAmount());
							boolean bufferHasData = false;
							
							if (!client.getOutputBuffer().isEmpty())
							{
								for (int i = 0; i < socketServer.getIOAmount(); i++)
								{
									if(!bufferHasData)
										bufferHasData = true;
									
									buffer.put(client.getOutputBuffer().pop());
									
									if (client.getOutputBuffer().isEmpty())
										break;
								}
								
								buffer.flip();
							}
							
							if(bufferHasData)
							{
								client.getSocket().write(buffer);
								buffer.clear();
							}
							else
							{
								client.setOutputWrite(false);
							}
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					try
					{
						socketServer.getOnProcess().run(client);
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
	
	public Set<SocketClient> getClients()
	{
		return clients;
	}
}
