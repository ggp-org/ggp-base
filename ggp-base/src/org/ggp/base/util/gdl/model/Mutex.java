package org.ggp.base.util.gdl.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;


/**
 * A Mutex represents a set of sentences that will be mutually
 * exclusive during any given turn. For example, the set of sentences
 * represented by (does white (move ?x ?y)) form a mutex due to
 * GDL's inherent rules. These mutexes can be important in creating
 * efficient state machines as well as in evaluating states and
 * reasoning about which moves to make.
 *  
 * @author Alex Landau
 */
public class Mutex {
	private SentenceForm form;
	//The tuple uses a GdlConstant 
	private List<GdlConstant> tuple;
	
	public Mutex(SentenceForm form2) {
		//All variables
		this.form = form2;
		tuple = Collections.nCopies(form.getTupleSize(), null);
		
	}

	public boolean matches(GdlSentence sentence) {
		if(!form.matches(sentence))
			return false;
		if(sentence instanceof GdlRelation) {
			Iterator<GdlConstant> tupleItr = tuple.iterator();
			if(!bodyMatches(sentence.getBody(), tupleItr))
				return false;
		}
		return true;
	}

	private boolean bodyMatches(List<GdlTerm> body, Iterator<GdlConstant> tupleItr) {
		for(GdlTerm term : body) {
			if(term instanceof GdlVariable) {
				if(tupleItr.next() != null)
					return false;
			} else if(term instanceof GdlConstant) {
				GdlConstant ourConstant = tupleItr.next();
				if(ourConstant != null && ourConstant != term)
					return false;
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<GdlTerm> functionBody = function.getBody();
				if(!bodyMatches(functionBody, tupleItr))
					return false;
			} else {
				throw new RuntimeException("New GDL term type added, error in Mutex");
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return form + ": " + tuple;
	}
}
