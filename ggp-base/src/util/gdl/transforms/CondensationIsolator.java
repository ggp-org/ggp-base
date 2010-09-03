package util.gdl.transforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.MoveMutexFinder;
import util.gdl.model.Mutex;
import util.gdl.model.SentenceModel;

public class CondensationIsolator {

	public static List<Gdl> run(List<Gdl> description, boolean restrained, boolean moreRestrained) {
		//This class is not put together in any "optimal" way, so it's left in
		//an unpolished state for now. A better version would use estimates of
		//the impact of breaking apart rules. (It also needs to stop itself from
		//making multiple new relations with the same meaning.)
		
		//This version will be rather advanced.
		//In particular, it will try to incorporate
		//1) More thorough scanning for condensations;
		//2) Condensations that are only safe to perform because of mutexes.
		
		//TODO: Don't perform condensations on stuff like (add _ _ _)...
		//In general, don't perform condensations where the headroom is huge?
		//Better yet... DON'T perform condensations on recursive functions!
		//As for headroom... maybe make sure that # of vars eliminated > # "kept"
		//Or make sure none are kept? Use directional connected components?
		
		description = GdlCleaner.run(description);
		description = DeORer.run(description);
		description = VariableConstrainer.replaceFunctionValuedVariables(description);
		
		//How do we define a condensation, and what needs to be true in it?
		//Definition: A condensation set is a set of conjuncts of a
		//sentence.
		//Restrictions:
		//1) There must be some variable not in the head of the sentence that
		//   appears exclusively in the condensation set. (This means we can
		//   easily find sets one of which must be a condensation set.)
		//2) For any variable appearing in a distinct or not conjunct in the set,
		//   there must be a positive conjunct in the set also containing that
		//   variable. This does apply to variables found in the head.
		//3) There must be at least one non-distinct literal outside the
		//   condensation set.
		
		//How mutexes work:
		//Say we have a rule
		//  (<= (r1 ?b)
		//      (r2 ?a ?b ?c)
		//      (r3 ?b ?c)
		//		(r4 ?a)
		//		(r5 ?c))
		//If we wanted to factor out ?a, we'd normally have to do
		/*  (<= (r6 ?b ?c)
		 * 		(r2 ?a ?b ?c)
		 * 		(r4 ?a))
		 *  (<= (r1 ?b)
		 * 		(r6 ?b ?c)
		 * 		(r3 ?b ?c)
		 * 		(r5 ?c))
		 * But if we know r2 is a mutex, instead we can do (notice r2 splitting):
		 *  (<= (r6 ?b)
		 * 		(r2 ?a ?b ?c)
		 * 		(r4 ?a))
		 *  (<= (r1 ?b)
		 *  	(r2 ?a ?b ?c)
		 *  	(r6 ?b)
		 *  	(r3 ?b ?c)
		 *  	(r5 ?c))
		 * Which in turn becomes:
		 *  (<= (r6 ?b)
		 * 		(r2 ?a ?b ?c)
		 * 		(r4 ?a))
		 *  (<= (r7 ?b)
		 *  	(r2 ?a ?b ?c)
		 *  	(r3 ?b ?c)
		 *  	(r5 ?c))
		 *  (<= (r1 ?b)
		 *  	(r6 ?b)
		 *		(r7 ?b))
		 * Both r6 and r7 can be further condensed to ignore ?c and ?a,
		 * respectively. What just happened?
		 * 1) The condensation set for ?a included the mutex r2.
		 * 2) r2 (by itself) would have required ?c to be included as an
		 *    argument passed back to the original rule, which is undesirable.
		 *    Instead, as it's a mutex, we leave a copy in the original rule
		 *    and don't include the ?c.
		 *    
		 * So, what kind of algorithm can we find to solve this task?
		 */
		List<Gdl> newDescription = new ArrayList<Gdl>();
		Queue<GdlRule> rulesToAdd = new LinkedList<GdlRule>();
		
		Set<Mutex> mutexes = MoveMutexFinder.findMutexes(description);
		if(restrained)
			mutexes = Collections.emptySet();
		
		for(Gdl gdl : description) {
			if(gdl instanceof GdlRule)
				rulesToAdd.add((GdlRule) gdl);
			else
				newDescription.add(gdl);
		}
		
		Set<String> sentenceNames = new HashSet<String>(new SentenceModel(description).getSentenceNames());
		
		while(!rulesToAdd.isEmpty()) {
			GdlRule curRule = rulesToAdd.remove();
			if(isRecursive(curRule)) {
				//Don't mess with it!
				newDescription.add(curRule);
				continue;
			}
			Set<GdlLiteral> condensationSet = getCondensationSet(curRule, mutexes, restrained, moreRestrained);
			if(condensationSet != null) {
				rulesToAdd.addAll(applyCondensation(condensationSet, curRule, mutexes, newDescription, rulesToAdd, sentenceNames));
			} else {
				newDescription.add(curRule);
			}
		}
		return newDescription;
	}

	private static boolean isRecursive(GdlRule rule) {
		for(GdlLiteral literal : rule.getBody())
			if(literal instanceof GdlSentence)
				if(((GdlSentence) literal).getName().equals(rule.getHead().getName()))
					//A good approximation
					return true;
		return false;
	}

	private static List<GdlRule> applyCondensation(
			Set<GdlLiteral> condensationSet, GdlRule rule,
			Set<Mutex> mutexes, List<Gdl> newDescription,
			Queue<GdlRule> rulesToAdd, Set<String> sentenceNames) {
		Set<GdlRule> existingRules = new HashSet<GdlRule>();
		for(Gdl gdl : newDescription)
			if(gdl instanceof GdlRule)
				existingRules.add((GdlRule) gdl);
		existingRules.addAll(rulesToAdd);

		Set<GdlVariable> varsInCondensationSet = new HashSet<GdlVariable>();
		for(GdlLiteral literal : condensationSet)
			varsInCondensationSet.addAll(SentenceModel.getVariables(literal));
		Set<GdlVariable> varsToKeep = new HashSet<GdlVariable>();
		//Which vars do we "keep" (put in our new condensed literal)?
		//Vars that are both:
		//1) In the condensation set, in a non-mutex literal
		//2) Either in the head or somewhere else outside the condensation set
		for(GdlLiteral literal : condensationSet)
			if(!isMutex(literal, mutexes))
				varsToKeep.addAll(SentenceModel.getVariables(literal));
		Set<GdlVariable> varsToKeep2 = new HashSet<GdlVariable>();
		varsToKeep2.addAll(SentenceModel.getVariables(rule.getHead()));
		for(GdlLiteral literal : rule.getBody())
			if(!condensationSet.contains(literal))
				varsToKeep2.addAll(SentenceModel.getVariables(literal));
		varsToKeep.retainAll(varsToKeep2);
		
		//Now we're ready to split it apart
		//Let's make the new rule
		List<GdlTerm> orderedVars = new ArrayList<GdlTerm>(varsToKeep);
		GdlConstant condenserName;
		for(int i = 0; ; i++) {
			String candidateName = rule.getHead().getName().getValue() + "_tmp" + i;
			if(!sentenceNames.contains(candidateName)) {
				condenserName = GdlPool.getConstant(candidateName);
				sentenceNames.add(candidateName);
				//Success!
				break;
			}
		}
		//Make the rule head
		GdlSentence condenserHead;
		if(orderedVars.isEmpty()) {
			condenserHead = GdlPool.getProposition(condenserName);
		} else {
			condenserHead = GdlPool.getRelation(condenserName, orderedVars);
		}
		List<GdlLiteral> condenserBody = new ArrayList<GdlLiteral>(condensationSet);
		GdlRule condenserRule = GdlPool.getRule(condenserHead, condenserBody);
		//TODO: Look for existing rules matching the new one
		
		//Replace the non-mutex elements with ___
		//Though... sometimes we do want to take out the mutex
		//Namely, when all the non-head vars in the mutex are being removed
		List<GdlLiteral> remainingLiterals = new ArrayList<GdlLiteral>();
		List<GdlLiteral> mutexLiterals = new ArrayList<GdlLiteral>();
		for(GdlLiteral literal : rule.getBody())
			if(!condensationSet.contains(literal))
				remainingLiterals.add(literal);
			else if(isMutex(literal, mutexes))
				mutexLiterals.add(literal);
		//Now we go through the mutexes and see if they're worth adding
		for(GdlLiteral mutexLiteral : mutexLiterals) {
			List<GdlVariable> mutexVars = SentenceModel.getVariables(mutexLiteral);
			mutexVars.removeAll(SentenceModel.getVariables(rule.getHead()));
			//vars in head
			//are any present outside the mutex?
			boolean presentOutside = false;
			for(GdlLiteral literal : remainingLiterals) {
				if(!Collections.disjoint(mutexVars, SentenceModel.getVariables(literal)))
					presentOutside = true;
			}
			if(presentOutside)
				//TODO: Maybe this should be added to something other than remainingLiterals
				//TODO: Might end up with conflicts involving other mutexes?
				remainingLiterals.add(mutexLiteral);
			//TODO: VariableNames
		}
		remainingLiterals.add(condenserHead);
		GdlRule modifiedRule = GdlPool.getRule(rule.getHead(), remainingLiterals);
		
		List<GdlRule> newRules = new ArrayList<GdlRule>(2);
		newRules.add(condenserRule);
		newRules.add(modifiedRule);
		return newRules;
	}

	private static boolean isMutex(GdlLiteral literal, Set<Mutex> mutexes) {
		if(!(literal instanceof GdlSentence))
			return false;
		for(Mutex mutex : mutexes) {
			if(mutex.matches((GdlSentence) literal))
				return true;
		}
		return false;
	}

	private static Set<GdlLiteral> getCondensationSet(GdlRule rule,
			Set<Mutex> mutexes, boolean restrained, boolean moreRestrained) {
		//We use each variable as a starting point
		List<GdlVariable> varsInRule = SentenceModel.getVariables(rule);
		List<GdlVariable> varsInHead = SentenceModel.getVariables(rule.getHead());
		List<GdlVariable> varsNotInHead = new ArrayList<GdlVariable>(varsInRule);
		varsNotInHead.removeAll(varsInHead);
		//System.out.println(rule);
		for(GdlVariable var : varsNotInHead) {
			//System.out.println(var);
			Set<GdlLiteral> minSet = new HashSet<GdlLiteral>();
			for(GdlLiteral literal : rule.getBody())
				if(SentenceModel.getVariables(literal).contains(var))
					minSet.add(literal);
			
			//TODO: New condition to worry about
			//What if we keep removing the same mutex, over and over?
			//Go through the requirements, fixing them as we check them
			//#1 is already done
			//Now we try #2
			Set<GdlVariable> varsNeeded = new HashSet<GdlVariable>();
			Set<GdlVariable> varsSupplied = new HashSet<GdlVariable>();
			for(GdlLiteral literal : minSet)
				if(literal instanceof GdlRelation)
					varsSupplied.addAll(SentenceModel.getVariables(literal));
				else if(literal instanceof GdlDistinct || literal instanceof GdlNot)
					varsNeeded.addAll(SentenceModel.getVariables(literal));
			varsNeeded.removeAll(varsSupplied);
			if(restrained && !varsNeeded.isEmpty())
				continue;
			//System.out.println("varsNeeded: " + varsNeeded + "; varsSupplied: " + varsSupplied);
			List<Set<GdlLiteral>> candidateSuppliersList = new ArrayList<Set<GdlLiteral>>();
			for(GdlVariable varNeeded : varsNeeded) {
				Set<GdlLiteral> suppliers = new HashSet<GdlLiteral>();
				for(GdlLiteral literal : rule.getBody())
					if(literal instanceof GdlRelation)
						if(SentenceModel.getVariables(literal).contains(varNeeded))
							suppliers.add(literal);
				candidateSuppliersList.add(suppliers);
			}
			//System.out.println("candidateSuppliersList: " + candidateSuppliersList);
			//TODO: Now... I'm not sure if we want to minimize the number of
			//literals added, or the number of variables added
			//Right now, I don't have time to worry about optimization
			Set<GdlLiteral> literalsToAdd = new HashSet<GdlLiteral>();
			for(Set<GdlLiteral> suppliers : candidateSuppliersList)
				if(Collections.disjoint(suppliers, literalsToAdd))
					literalsToAdd.add(suppliers.iterator().next());
			//System.out.println("literalsToAdd: " + literalsToAdd);
			minSet.addAll(literalsToAdd);
			
			if(moreRestrained) {
				//How many non-head variables will we need to "keep"?
				//If fewer than the number we're eliminating, don't bother
				//Alternative: If any, don't bother
				Set<GdlVariable> varsToKeep = new HashSet<GdlVariable>();
				Set<GdlVariable> varsEliminated = new HashSet<GdlVariable>();
				for(GdlLiteral literal : rule.getBody())
					if(!minSet.contains(literal))
						varsToKeep.addAll(SentenceModel.getVariables(literal));
					else
						varsEliminated.addAll(SentenceModel.getVariables(literal));
				varsEliminated.removeAll(varsInHead);
				varsToKeep.retainAll(varsEliminated);
				varsEliminated.removeAll(varsToKeep);
				if(varsToKeep.size() >= varsEliminated.size())
					return null;
			}
			
			boolean containsNonMutexes = false;
			for(GdlLiteral literal : minSet)
				if(!isMutex(literal, mutexes))
					containsNonMutexes = true;
			if(!containsNonMutexes)
				continue; //to the next variable
			//TODO: But what if a mutex gets left here because of that?
			//Should be legal if it's going to actually get removed
			
			//System.out.println(minSet);
			//Finally, check #3
			for(GdlLiteral literal : rule.getBody()) {
				if(literal instanceof GdlRelation || literal instanceof GdlNot)
					if(!minSet.contains(literal))
						return minSet;
			}
			//return nothing
		}
		return null;
	}

}
