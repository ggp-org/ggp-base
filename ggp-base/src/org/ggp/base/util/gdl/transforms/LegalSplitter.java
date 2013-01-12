package org.ggp.base.util.gdl.transforms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.statemachine.Role;


public class LegalSplitter {
	private static final GdlConstant LEGAL = GdlPool.getConstant("legal");

	/**
	 * Modifies the rules for legal moves in a GDL description so that
	 * they use constants for each player in place of variables; e.g.
	 * rules with the head (does ?player (move ?x ?y)) are replaced
	 * with rules with heads (does white (move ?x ?y)),
	 * (does black (move ?x ?y)), etc. This may be useful when trying
	 * to identify which types of moves are mutually exclusive, i.e.,
	 * only one player can play such a move at a time. (See
	 * MoveMutexFinder.) 
	 */
	public static List<Gdl> run(List<Gdl> description) {
		List<Gdl> newDescription = new ArrayList<Gdl>();
		List<Role> roles = Role.computeRoles(description);
		
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				GdlSentence head = rule.getHead();
				if(head.getName().equals(LEGAL)) {
					if(head instanceof GdlProposition || head.arity() != 2)
						throw new RuntimeException("Head of rule is improper 'legal' sentence: " + rule);
					//Is the player name given by a variable?
					GdlTerm playerTerm = head.get(0);
					if(playerTerm instanceof GdlVariable) {
						
						//Here we split the rule up
						for(Role role : roles) {
							GdlConstant playerName = role.getName();
							//Substitute into the rule...
							GdlRule newRule = CommonTransforms.replaceVariable(rule, (GdlVariable) playerTerm, playerName);
							newDescription.add(newRule);
						}
						
					} else {
						newDescription.add(rule);
					}
				} else {
					newDescription.add(rule);
				}
			} else {
				newDescription.add(gdl);
			}
		}
		
		removeImpossibleRules(newDescription);
		
		return newDescription;
	}

	//This won't get all the rules that can't possibly be true,
	//but it will get those that can't be true based on a
	//"distinct" literal between two constants.
	private static void removeImpossibleRules(List<Gdl> newDescription) {
		Iterator<Gdl> itr = newDescription.iterator();
		while(itr.hasNext()) {
			Gdl gdl = itr.next();
			
			if(gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				
				//Look for a bad GdlDistinct
				for(GdlLiteral literal : rule.getBody()) {
					if(literal instanceof GdlDistinct) {
						GdlDistinct distinct = (GdlDistinct) literal;
						//Is it (distinct c c) for some constant c?
						if(distinct.getArg1() instanceof GdlConstant
								&& distinct.getArg1() == distinct.getArg2()) {
							itr.remove();
							System.out.println("Removed impossible rule " + rule);
							break; //out of the rule
						}
					}
				}
			}
		}
	}
}
