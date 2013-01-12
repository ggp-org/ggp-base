package org.ggp.base.util.gdl.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.MoveMutexFinder;
import org.ggp.base.util.gdl.model.Mutex;


public class CrudeSplitter {
	public static List<Gdl> run(List<Gdl> description) throws InterruptedException {
		//Recommend running CI, then this, then CI again
		
		Set<Mutex> mutexes = MoveMutexFinder.findMutexes(description);
		
		//Go through the rules looking for mutex body literals
		List<Gdl> newDescription = new ArrayList<Gdl>();
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				//Does it contain a mutex?
				if(containsMutex((GdlRule)gdl, mutexes))
					newDescription.add(splittableRule((GdlRule)gdl, mutexes));
				else
					newDescription.add(gdl);
			} else {
				newDescription.add(gdl);
			}
		}
		return newDescription;
	}

	private static GdlRule splittableRule(GdlRule rule, Set<Mutex> mutexes) {
		//System.out.println("Trying to get splittable rule for " + rule);
		//Find the mutex (assume there's only one)
		//Split it based on how its variables are used in the rest of the rule
		//We're basically looking for connected components
		GdlLiteral mutexComponent = null;
		for(GdlLiteral literal : rule.getBody()) {
			if(literal instanceof GdlSentence) {
				for(Mutex mutex : mutexes) {
					if(mutex.matches((GdlSentence)literal))
						mutexComponent = literal;
				}
			}
		}
		if(mutexComponent == null) throw new RuntimeException(":" + rule);
		//Want to form connected components from variables in the rule...
		List<GdlVariable> varsInMutex = GdlUtils.getVariables(mutexComponent);
		Map<GdlVariable, Set<GdlVariable>> map = new HashMap<GdlVariable, Set<GdlVariable>>();
		//Also do this for head, not just body literals
		{
			List<GdlVariable> vars = GdlUtils.getVariables(rule.getHead());
			for(GdlVariable var : vars) {
				if(!map.containsKey(var))
					map.put(var, new HashSet<GdlVariable>());
				map.get(var).addAll(vars);
			}
		}
		for(GdlLiteral literal : rule.getBody()) {
			if(literal == mutexComponent)
				continue;
			List<GdlVariable> vars = GdlUtils.getVariables(literal);
			for(GdlVariable var : vars) {
				if(!map.containsKey(var))
					map.put(var, new HashSet<GdlVariable>());
				map.get(var).addAll(vars);
			}
		}
		//TODO: Can't explain why this is right right now
		for(GdlVariable key : map.keySet()) {
			if(!varsInMutex.contains(key))
				map.get(key).clear();
			else
				map.get(key).retainAll(varsInMutex);
		}
		List<Set<GdlVariable>> connectedComponents;
		connectedComponents = getConnectedComponents(map);
		//So now what?
		//Find ccs overlapping the mutex's variables
		//Where that happens, make a copy of the mutex sharing only those
		//variables with the rest of the rule
		//The remainder become new variables
		List<String> variableNames = GdlUtils.getVariableNames(rule);
		int varName = 0;
		List<GdlLiteral> mutexCopies = new ArrayList<GdlLiteral>();
		for(Set<GdlVariable> cc : connectedComponents) {
			if(!Collections.disjoint(cc, varsInMutex)) {
				Set<GdlVariable> varsToReplace = new HashSet<GdlVariable>(varsInMutex);
				varsToReplace.removeAll(cc);
				//Replace all these vars with new variables
				Map<GdlVariable, GdlVariable> renaming = new HashMap<GdlVariable, GdlVariable>();
				for(GdlVariable toReplace : varsToReplace) {
					String newCandidateName = "?a" + (varName++);
					while(variableNames.contains(newCandidateName))
						newCandidateName = "?a" + (varName++);
					renaming.put(toReplace, GdlPool.getVariable(newCandidateName));
					variableNames.add(newCandidateName);
				}
				mutexCopies.add(CommonTransforms.replaceVariables(mutexComponent, renaming));
			}
		}
		//We also want to make sure whatever parts are
		if(mutexCopies.size() <= 1)
			return rule; //No benefit to the renaming
		
		List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();
		for(GdlLiteral literal : rule.getBody()) {
			if(literal != mutexComponent)
				newBody.add(literal);
		}
		newBody.addAll(mutexCopies);
		GdlRule newRule = GdlPool.getRule(rule.getHead(), newBody);
		//System.out.println("Split " + rule + " into " + newRule);
		return newRule;
	}
	private static List<Set<GdlVariable>> getConnectedComponents(
			Map<GdlVariable, Set<GdlVariable>> graph) {
		List<Set<GdlVariable>> components = new ArrayList<Set<GdlVariable>>();
		Set<GdlVariable> varsAdded = new HashSet<GdlVariable>();
		
		for(GdlVariable key : graph.keySet()) {
			Set<GdlVariable> component = new HashSet<GdlVariable>();
			Queue<GdlVariable> varsToAdd = new LinkedList<GdlVariable>();
			if(!varsAdded.contains(key))
				varsToAdd.add(key);
			
			while(!varsToAdd.isEmpty()) {
				GdlVariable curVar = varsToAdd.remove();
				if(varsAdded.contains(curVar))
					continue;
				//Find the children
				Set<GdlVariable> children = graph.get(curVar);
				//Add those children that have not been handled
				for(GdlVariable child : children) {
					if(!varsAdded.contains(child))
						varsToAdd.add(child);
				}

				component.add(curVar);
				varsAdded.add(curVar);
			}
			if(!component.isEmpty())
				components.add(component);
		}
		
		return components;
	}

	private static boolean containsMutex(GdlRule rule, Set<Mutex> mutexes) {
		for(GdlLiteral literal : rule.getBody()) {
			if(literal instanceof GdlSentence) {
				for(Mutex mutex : mutexes) {
					if(mutex.matches((GdlSentence)literal))
						return true;
				}
			}
		}
		return false;
	}
}
