package com.konloch.vortex.interfaces;

import com.konloch.vortex.Client;

/**
 * @author Konloch
 * @since 2/28/2023
 */
public interface ClientRunnable
{
	/**
	 * Runnable function
	 * @param client the client reference
	 */
	void run(Client client);
}
