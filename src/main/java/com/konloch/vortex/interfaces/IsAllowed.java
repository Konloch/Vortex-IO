package com.konloch.vortex.interfaces;

import com.konloch.vortex.Client;

/**
 * @author Konloch
 * @since 3/1/2023
 */
public interface IsAllowed
{
	boolean allowed(Client client);
}
