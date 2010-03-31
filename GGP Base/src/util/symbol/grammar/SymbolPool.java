package util.symbol.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SymbolPool
{

	private final static ConcurrentMap<String, SymbolAtom> atomPool = new ConcurrentHashMap<String, SymbolAtom>();
	private final static ConcurrentMap<List<Symbol>, SymbolList> listPool = new ConcurrentHashMap<List<Symbol>, SymbolList>();

	/**
	 * If the pool does not have a mapping for the given key, adds a mapping from key to value
	 * to the pool.
	 * 
	 * Note that even if you've checked to make sure that the pool doesn't contain the key,
	 * you still shouldn't assume that this method actually inserts the given value, since
	 * this class is accessed by multiple threads simultaneously.
	 * 
	 * @return the value mapped to by key in the pool
	 */
	private static <K,V> V addToPool(K key, V value, ConcurrentMap<K, V> pool) {
		V prevValue = pool.putIfAbsent(key, value);
		if(prevValue == null)
			return value;
		else
			return prevValue;
	}
	
	public static SymbolAtom getAtom(String value)
	{
		SymbolAtom ret = atomPool.get(value);
		if(ret == null)
			ret = addToPool(value, new SymbolAtom(value), atomPool);
		
		return ret;
	}

	public static SymbolList getList(List<Symbol> contents)
	{
		SymbolList ret = listPool.get(contents);
		if(ret == null)
			ret = addToPool(contents, new SymbolList(contents), listPool);
		
		return ret;
	}

	public static SymbolList getList(Symbol[] contents)
	{
		return getList(Arrays.asList(contents));
	}

}

