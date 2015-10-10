package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class DependencyGraphsTest extends Assert {
	@Test
	public void testSafeToposort() throws Exception {
		Set<Integer> allElements = Sets.newHashSet(1, 2, 3, 4, 5, 6, 7, 8);
		Multimap<Integer, Integer> graph = HashMultimap.create();

		graph.put(2, 1);
		graph.put(3, 2);
		graph.put(4, 2);
		graph.put(5, 3);
		graph.put(5, 4);
		graph.put(3, 4);
		graph.put(4, 3);
		graph.put(4, 6);
		graph.put(6, 7);
		graph.put(7, 8);
		graph.put(8, 3);

		List<Set<Integer>> ordering = DependencyGraphs.toposortSafe(allElements, graph);
		assertEquals(4, ordering.size());
		assertEquals(ImmutableSet.of(1), ordering.get(0));
		assertEquals(ImmutableSet.of(2), ordering.get(1));
		assertEquals(ImmutableSet.of(3, 4, 6, 7, 8), ordering.get(2));
		assertEquals(ImmutableSet.of(5), ordering.get(3));
	}
}
