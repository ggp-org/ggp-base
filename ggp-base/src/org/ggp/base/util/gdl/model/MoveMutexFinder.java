package org.ggp.base.util.gdl.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.transforms.DeORer;
import org.ggp.base.util.gdl.transforms.LegalSplitter;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;
import org.ggp.base.util.statemachine.Role;


public class MoveMutexFinder {
	private static final GdlConstant LEGAL = GdlPool.getConstant("legal");
	private static final GdlConstant DOES = GdlPool.getConstant("does");

	public static Set<Mutex> findMutexes(List<Gdl> description) throws InterruptedException {
		//The kind of logic we're using is as follows:
		
		//First, we need to know the flow of the game, as in
		//what happens regardless of the players' actions.
		
		//Then we find sentence forms for moves that only one player
		//can make at a time. We can automatically add all the mutexes
		//involving a specific player. However, the really interesting
		//mutexes are those that have a variable player; i.e., only one
		//player can make this move type at any given time. This can be
		//very useful to know. For example, with a little extra
		//information, we can often figure out whether this is
		//an alternating game, solely from analysis of the GDL.
		
		//For each player, we record on which turns they can make
		//moves of a given sentence type. This comes from examining
		//all the rules that could make moves of that type legal.
		//For now, we assume that some sentence defined by the flow
		//of the game (or its constants) is directly present in each
		//rule defining the move's sentence type.
		
		//These transformations help...
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		description = LegalSplitter.run(description);
		
		SentenceModel model = new SentenceModelImpl(description);
		
		GameFlow flow = new GameFlow(description);
		
		//What operations would I use on it?
		Map<SentenceForm, Map<Role, Set<Integer>>> moveTurnsByForm = new HashMap<SentenceForm, Map<Role, Set<Integer>>>();
		
		for(SentenceForm form : model.getSentenceForms()) {
			if(form.getName().equals(LEGAL)) {
				//Look for rules with this head
				Set<GdlRule> generatingRules = model.getRules(form);
				
				if(!moveTurnsByForm.containsKey(form))
					moveTurnsByForm.put(form, new HashMap<Role, Set<Integer>>());
				Map<Role, Set<Integer>> moveTurnsByRole = moveTurnsByForm.get(form);

				for(GdlRule rule : generatingRules) {
					//We have some rule defining some move as legal.
					//First we get the appropriate set of turns.
					GdlTerm playerName = rule.getHead().get(0);
					if(!(playerName instanceof GdlConstant))
						throw new RuntimeException("LegalSplitter failed on rule " + rule + " (reported by MoveMutexFinder)");
					Role role = new Role((GdlConstant) playerName);
					
					if(!moveTurnsByRole.containsKey(role))
						moveTurnsByRole.put(role, new HashSet<Integer>());
					Set<Integer> moveTurns = moveTurnsByRole.get(role);
					
					Set<Integer> curMoveTurns = flow.getTurnsConjunctsArePossible(rule.getBody());
					moveTurns.addAll(curMoveTurns);
				}
				
				//Handle relations
				Set<GdlRelation> legalRelations = model.getRelations(form);
				for(GdlRelation relation : legalRelations) {
					GdlTerm playerName = relation.get(0);
					Role role = new Role((GdlConstant) playerName);
					
					if(!moveTurnsByRole.containsKey(role))
						moveTurnsByRole.put(role, new HashSet<Integer>());
					Set<Integer> moveTurns = moveTurnsByRole.get(role);
					
					//The move is always possible
					Set<Integer> curMoveTurns = flow.getCompleteTurnSet();
					moveTurns.addAll(curMoveTurns);
				}
			}
		}
		
		//Now we have moveTurnsByForm filled, but we need to process
		//it to figure out what the actual mutexes are
		Set<Mutex> mutexes = new HashSet<Mutex>();
		for(Entry<SentenceForm, Map<Role, Set<Integer>>> entry : moveTurnsByForm.entrySet()) {
			SentenceForm form = entry.getKey();
			Map<Role, Set<Integer>> moveTurnsByRole = entry.getValue();
			
			//What does it take to add the sentence form with a
			//variable player? All the sets of move IDs of all the
			//players must be disjoint.
			boolean allDisjoint = true;
			Set<Integer> allTurnsSoFar = new HashSet<Integer>();
			for(Set<Integer> moveTurns : moveTurnsByRole.values()) {
				if(!Collections.disjoint(moveTurns, allTurnsSoFar)) {
					allDisjoint = false;
					break;
				}
				allTurnsSoFar.addAll(moveTurns);
			}
			
			if(allDisjoint) {
				//Make the mutex
				Mutex mutex = new Mutex(form.getCopyWithName(DOES)); //all variables
				mutexes.add(mutex);
			}
		}
		
		return mutexes;
	}
	
}
