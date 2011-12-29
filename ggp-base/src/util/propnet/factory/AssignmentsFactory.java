package util.propnet.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.GdlUtils;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlVariable;
import util.gdl.model.SentenceForm;
import util.gdl.model.SentenceFormSource;
import util.gdl.model.SentenceModel;
import util.propnet.factory.AssignmentsImpl.ConstantForm;

public class AssignmentsFactory {

	public static Assignments getAssignmentsForRule(GdlRule rule,
			SentenceModel model, Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
		return new AssignmentsImpl(rule,
				model.getVarDomains(rule),
				constantForms,
				completedSentenceFormValues,
				model);
	}
	
	public static Assignments getAssignmentsForRule(GdlRule rule,
			Map<GdlVariable, Set<GdlConstant>> varDomains,
			Map<SentenceForm, ConstantForm> constantForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues,
			SentenceFormSource sentenceFormSource) {
		return new AssignmentsImpl(rule,
				varDomains,
				constantForms,
				completedSentenceFormValues,
				sentenceFormSource);
	}
	
	public static Assignments getAssignmentsWithRecursiveInput(GdlRule rule,
			SentenceModel model, SentenceForm form, GdlSentence input,
			Map<SentenceForm, ConstantForm> constantForms, boolean useConstForms,
			Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
		//Look for the literal(s) in the rule with the sentence form of the
		//recursive input. This can be tricky if there are multiple matching
		//literals.
		List<GdlSentence> matchingLiterals = new ArrayList<GdlSentence>();
		for(GdlLiteral literal : rule.getBody())
			if(literal instanceof GdlSentence)
				if(form.matches((GdlSentence) literal))
					matchingLiterals.add((GdlSentence) literal);

		List<Assignments> assignmentsList = new ArrayList<Assignments>();
		for(GdlSentence matchingLiteral : matchingLiterals) {
			//left has the variables, right has the constants
			Map<GdlVariable, GdlConstant> preassignment = GdlUtils.getAssignmentMakingLeftIntoRight(matchingLiteral, input);
			if(preassignment != null) {
				Assignments assignments = new AssignmentsImpl(
						preassignment,
						rule,
						model.getVarDomains(rule),
						constantForms,
						completedSentenceFormValues,
						model);
				assignmentsList.add(assignments);
			}
		}

		if(assignmentsList.size() == 0)
			return new AssignmentsImpl();
		if(assignmentsList.size() == 1)
			return assignmentsList.get(0);
		throw new RuntimeException("Not yet implemented: assignments for recursive functions with multiple recursive conjuncts");
		//TODO: Plan to implement by subclassing Assignments into something
		//that contains and iterates over multiple Assignments
	}
	
	//TODO: Put the constructor that uses the SentenceModel here
	

}
