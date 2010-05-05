package util.propnet.factory;

import java.util.List;

import util.gdl.grammar.Gdl;
import util.propnet.architecture.PropNet;
import util.propnet.serialization.PropNetCache;

/**
 * The CachedPropNetFactory class augments the ordinary @PropNetFactory class
 * by using the @PropNetCache to look up existing, pre-built propnets.
 */
public final class CachedPropNetFactory
{
	/**
	 * Creates a PropNet from a game description, either using the PropNetCache
	 * or the regular PropNetFactory.
	 * 
	 * @param description
	 *            A game description.
	 * @return An equivalent PropNet.
	 */
	public static PropNet create(List<Gdl> description)
	{
	    // First, try to look up the network using the cache.
        PropNet theNet = PropNetCache.loadNetworkFromCache(description);
        if(theNet != null) return theNet;
        
        // Otherwise, fall back to the regular PropNetFactory.
        return PropNetFactory.create(description);
	}
}