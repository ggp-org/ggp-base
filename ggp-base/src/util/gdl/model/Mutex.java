package util.gdl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.SentenceModel.SentenceForm;

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
			List<Integer> index = new ArrayList<Integer>(1);
			index.add(0);
			if(!bodyMatches(sentence.getBody(), index))
				return false;
		}
		return true;
	}

	private boolean bodyMatches(List<GdlTerm> body, List<Integer> index) {
		for(GdlTerm term : body) {
			GdlConstant ourConstant = tuple.get(index.get(0));
			if(term instanceof GdlVariable) {
				if(ourConstant != null)
					return false;
				
				index.set(0, 1 + index.get(0));
			} else if(term instanceof GdlConstant) {
				if(ourConstant != null && ourConstant != term)
					return false;
				
				index.set(0, 1 + index.get(0));
			} else if(term instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) term;
				List<GdlTerm> functionBody = function.getBody();
				if(!bodyMatches(functionBody, index))
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
