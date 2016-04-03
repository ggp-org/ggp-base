package org.ggp.base.util.gdl.model.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceDomainModels;
import org.ggp.base.util.gdl.model.SentenceDomainModels.VarDomainOpts;
import org.ggp.base.util.gdl.model.SentenceForm;


public class AssignmentsFactory {

    private AssignmentsFactory() {
    }

    public static Assignments getAssignmentsForRule(GdlRule rule,
                                                    SentenceDomainModel model, Map<SentenceForm, FunctionInfo> functionInfoMap,
                                                    Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
        return new AssignmentsImpl(rule,
                SentenceDomainModels.getVarDomains(rule, model, VarDomainOpts.INCLUDE_HEAD),
                functionInfoMap,
                completedSentenceFormValues);
    }

    public static Assignments getAssignmentsForRule(GdlRule rule,
            Map<GdlVariable, Set<GdlConstant>> varDomains,
            Map<SentenceForm, FunctionInfo> functionInfoMap,
            Map<SentenceForm, ? extends Collection<GdlSentence>> completedSentenceFormValues) {
        return new AssignmentsImpl(rule,
                varDomains,
                functionInfoMap,
                completedSentenceFormValues);
    }

    public static Assignments getAssignmentsWithRecursiveInput(GdlRule rule,
            SentenceDomainModel model, SentenceForm form, GdlSentence input,
            Map<SentenceForm, FunctionInfo> functionInfoMap,
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
                        //TODO: This one getVarDomains call is why a lot of
                        //SentenceModel/DomainModel stuff is required. Can
                        //this be better factored somehow?
                        SentenceDomainModels.getVarDomains(rule, model, VarDomainOpts.INCLUDE_HEAD),
                        functionInfoMap,
                        completedSentenceFormValues);
                assignmentsList.add(assignments);
            }
        }

        if(assignmentsList.isEmpty())
            return new AssignmentsImpl();
        if(assignmentsList.size() == 1)
            return assignmentsList.get(0);
        throw new RuntimeException("Not yet implemented: assignments for recursive functions with multiple recursive conjuncts");
        //TODO: Plan to implement by subclassing Assignments into something
        //that contains and iterates over multiple Assignments
    }

    //TODO: Put the constructor that uses the SentenceModel here


}
