package com.konloch.vortex.interfaces;

import com.konloch.vortex.Client;

/**
 * @author Konloch
 * @since 3/1/2023
 */
public interface IsAllowed
{
	/**
	 * Used for filtering
	 * @param client the client reference
	 * @return true if allowed, false if not
	 */
	boolean allowed(Client client);
}
