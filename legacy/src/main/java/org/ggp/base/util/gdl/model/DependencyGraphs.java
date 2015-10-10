package org.ggp.base.util.gdl.model;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * When dealing with GDL, dependency graphs are often useful. DependencyGraphs
 * offers a variety of functionality for dealing with dependency graphs expressed
 * in the form of SetMultimaps.
 *
 * These multimaps are paired with sets of all nodes, to account for the
 * possibility of nodes not included in the multimap representation.
 *
 * All methods assume that keys in multimaps depend on their associated values,
 * or in other words are downstream of or are children of those values.
 */
public class DependencyGraphs {
    private DependencyGraphs() {}

    /**
     * Returns all elements of the dependency graph that match the
     * given predicate, and any elements upstream of those matching
     * elements.
     *
     * The graph may contain cycles.
     *
     * Each key in the dependency graph depends on/is downstream of
     * its associated values.
     */
    public static <T> ImmutableSet<T> getMatchingAndUpstream(
            Set<T> allNodes,
            SetMultimap<T, T> dependencyGraph,
            Predicate<T> matcher) {
        Set<T> results = Sets.newHashSet();

        Deque<T> toTry = Queues.newArrayDeque();
        toTry.addAll(Collections2.filter(allNodes, matcher));

        while (!toTry.isEmpty()) {
            T curElem = toTry.remove();
            if (!results.contains(curElem)) {
                results.add(curElem);
                toTry.addAll(dependencyGraph.get(curElem));
            }
        }
        return ImmutableSet.copyOf(results);
    }

    /**
     * Returns all elements of the dependency graph that match the
     * given predicate, and any elements downstream of those matching
     * elements.
     *
     * The graph may contain cycles.
     *
     * Each key in the dependency graph depends on/is downstream of
     * its associated values.
     */
    public static <T> ImmutableSet<T> getMatchingAndDownstream(
            Set<T> allNodes,
            SetMultimap<T, T> dependencyGraph,
            Predicate<T> matcher) {
        return getMatchingAndUpstream(allNodes, reverseGraph(dependencyGraph), matcher);
    }

    public static <T> SetMultimap<T, T> reverseGraph(SetMultimap<T, T> graph) {
        return Multimaps.invertFrom(graph, HashMultimap.<T, T>create());
    }

    /**
     * Given a dependency graph, return a topologically sorted
     * ordering of its components, stratified in a way that allows
     * for recursion and cycles. (Each set in the list is one
     * unordered "stratum" of elements. Elements may depend on elements
     * in earlier strata or the same stratum, but not in later
     * strata.)
     *
     * If there are no cycles, the result will be a list of singleton
     * sets, topologically sorted.
     *
     * Each key in the given dependency graph depends on/is downstream of
     * its associated values.
     */
    public static <T> List<Set<T>> toposortSafe(
            Set<T> allElements,
            Multimap<T, T> dependencyGraph) {
        Set<Set<T>> strataToAdd = createAllStrata(allElements);
        SetMultimap<Set<T>, Set<T>> strataDependencyGraph = createStrataDependencyGraph(dependencyGraph);
        List<Set<T>> ordering = Lists.newArrayList();

        while (!strataToAdd.isEmpty()) {
            Set<T> curStratum = strataToAdd.iterator().next();
            addOrMergeStratumAndAncestors(curStratum, ordering,
                    strataToAdd, strataDependencyGraph,
                    Lists.<Set<T>>newArrayList());
        }
        return ordering;
    }

    private static <T> void addOrMergeStratumAndAncestors(Set<T> curStratum,
            List<Set<T>> ordering, Set<Set<T>> toAdd,
            SetMultimap<Set<T>, Set<T>> strataDependencyGraph,
            List<Set<T>> downstreamStrata) {
        if (downstreamStrata.contains(curStratum)) {
            int mergeStartIndex = downstreamStrata.indexOf(curStratum);
            List<Set<T>> toMerge = downstreamStrata.subList(mergeStartIndex, downstreamStrata.size());
            mergeStrata(Sets.newHashSet(toMerge), toAdd, strataDependencyGraph);
            return;
        }
        downstreamStrata.add(curStratum);
        for (Set<T> parent : ImmutableList.copyOf(strataDependencyGraph.get(curStratum))) {
            //We could merge away the parent here, so we protect against CMEs and
            //make sure the parent is still in toAdd before recursing.
            if (toAdd.contains(parent)) {
                addOrMergeStratumAndAncestors(parent, ordering, toAdd, strataDependencyGraph, downstreamStrata);
            }
        }
        downstreamStrata.remove(curStratum);
        // - If we've added all our parents, we will still be in toAdd
        //   and none of our dependencies will be in toAdd. Add to the ordering.
        // - If there was a merge upstream that we weren't involved in,
        //   we will still be in toAdd, but we will have (possibly new)
        //   dependencies that are still in toAdd. Do nothing.
        // - If there was a merge upstream that we were involved in,
        //   we won't be in toAdd anymore. Do nothing.
        if (!toAdd.contains(curStratum)) {
            return;
        }
        for (Set<T> parent : strataDependencyGraph.get(curStratum)) {
            if (toAdd.contains(parent)) {
                return;
            }
        }
        ordering.add(curStratum);
        toAdd.remove(curStratum);
    }

    //Replace the old strata with the new stratum in toAdd and strataDependencyGraph.
    private static <T> void mergeStrata(Set<Set<T>> toMerge,
            Set<Set<T>> toAdd,
            SetMultimap<Set<T>, Set<T>> strataDependencyGraph) {
        Set<T> newStratum = ImmutableSet.copyOf(Iterables.concat(toMerge));
        for (Set<T> oldStratum : toMerge) {
            toAdd.remove(oldStratum);
        }
        toAdd.add(newStratum);
        //Change the keys
        for (Set<T> oldStratum : toMerge) {
            Collection<Set<T>> parents = strataDependencyGraph.get(oldStratum);
            strataDependencyGraph.putAll(newStratum, parents);
            strataDependencyGraph.removeAll(oldStratum);
        }
        //Change the values
        for (Entry<Set<T>, Set<T>> entry : ImmutableList.copyOf(strataDependencyGraph.entries())) {
            if (toMerge.contains(entry.getValue())) {
                strataDependencyGraph.remove(entry.getKey(), entry.getValue());
                strataDependencyGraph.put(entry.getKey(), newStratum);
            }
        }
    }

    private static <T> Set<Set<T>> createAllStrata(Set<T> allElements) {
        Set<Set<T>> result = Sets.newHashSet();
        for (T element : allElements) {
            result.add(ImmutableSet.of(element));
        }
        return result;
    }

    private static <T> SetMultimap<Set<T>, Set<T>> createStrataDependencyGraph(
            Multimap<T, T> dependencyGraph) {
        SetMultimap<Set<T>, Set<T>> strataDependencyGraph = HashMultimap.create();
        for (Entry<T, T> entry : dependencyGraph.entries()) {
            strataDependencyGraph.put(ImmutableSet.of(entry.getKey()), ImmutableSet.of(entry.getValue()));
        }
        return strataDependencyGraph;
    }
}
