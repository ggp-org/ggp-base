package org.ggp.base.util.statemachine.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a generic implementation of a Time-To-Live cache
 * that maps keys of type K to values of type V. It's backed
 * by a hashmap, and whenever a pair (K,V) is accessed, their
 * TTL is reset to the starting TTL (which is the parameter
 * passed to the constructor). On the other hand, when the
 * method prune() is called, the TTL of all of the pairs in the
 * map is decremented, and pairs whose TTL has reached zero are
 * removed.
 *
 * While this class implements the Map interface, keep in mind
 * that it only decrements the TTL of an entry when that entry
 * is accessed directly.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public final class TtlCache<K, V> implements Map<K,V>
{
	private final class Entry
	{
		public int ttl;
		public V value;

		public Entry(V value, int ttl)
		{
			this.value = value;
			this.ttl = ttl;
		}

		@Override
		@SuppressWarnings("unchecked")
        public boolean equals(Object o) {
		    if (o instanceof TtlCache.Entry) {
		        return ((Entry)o).value.equals(value);
		    }
		    return false;
		}
	}

	private final Map<K, Entry> contents;
	private final int ttl;

	public TtlCache(int ttl)
	{
		this.contents = new HashMap<K, Entry>();
		this.ttl = ttl;
	}

	@Override
	public synchronized boolean containsKey(Object key)
	{
		return contents.containsKey(key);
	}

	@Override
	public synchronized V get(Object key)
	{
		Entry entry = contents.get(key);
		if (entry == null)
		    return null;

		// Reset the TTL when a value is accessed directly.
		entry.ttl = ttl;
		return entry.value;
	}

	public synchronized void prune()
	{
		List<K> toPrune = new ArrayList<K>();
		for (K key : contents.keySet())
		{
			Entry entry = contents.get(key);
			if (entry.ttl == 0)
			{
				toPrune.add(key);
			}
			entry.ttl--;
		}

		for (K key : toPrune)
		{
			contents.remove(key);
		}
	}

	@Override
	public synchronized V put(K key, V value)
	{
		Entry x = contents.put(key, new Entry(value, ttl));
		if(x == null) return null;
		return x.value;
	}

	@Override
	public synchronized int size()
	{
		return contents.size();
	}

    @Override
	public synchronized void clear() {
        contents.clear();
    }

    @Override
	public synchronized boolean containsValue(Object value) {
        return contents.containsValue(value);
    }

    @Override
	public synchronized boolean isEmpty() {
        return contents.isEmpty();
    }

    @Override
	public synchronized Set<K> keySet() {
        return contents.keySet();
    }

    @Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
         for(Map.Entry<? extends K, ? extends V> anEntry : m.entrySet()) {
             this.put(anEntry.getKey(), anEntry.getValue());
         }
    }

    @Override
	public synchronized V remove(Object key) {
        return contents.remove(key).value;
    }

    @Override
	public synchronized Collection<V> values() {
        Collection<V> theValues = new HashSet<V>();
        for (Entry e : contents.values())
            theValues.add(e.value);
        return theValues;
    }

    private class entrySetMapEntry implements Map.Entry<K,V> {
        private K key;
        private V value;

        entrySetMapEntry(K k, V v) {
            key = k;
            value = v;
        }

        @Override
		public K getKey() { return key; }
        @Override
		public V getValue() { return value; }
        @Override
		public V setValue(V value) { return (this.value = value); }
    }

    @Override
	public synchronized Set<java.util.Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K,V>> theEntries = new HashSet<Map.Entry<K, V>>();
        for (Map.Entry<K, Entry> e : contents.entrySet())
            theEntries.add(new entrySetMapEntry(e.getKey(), e.getValue().value));
        return theEntries;
    }
}