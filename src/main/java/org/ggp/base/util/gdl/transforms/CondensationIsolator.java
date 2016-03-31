package org.ggp.base.util.gdl.transforms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.CartesianSentenceFormDomain;
import org.ggp.base.util.gdl.model.SentenceDomainModel;
import org.ggp.base.util.gdl.model.SentenceDomainModelFactory;
import org.ggp.base.util.gdl.model.SentenceDomainModelOptimizer;
import org.ggp.base.util.gdl.model.SentenceDomainModels;
import org.ggp.base.util.gdl.model.SentenceDomainModels.VarDomainOpts;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormDomain;
import org.ggp.base.util.gdl.model.SentenceFormModel;
import org.ggp.base.util.gdl.model.SentenceForms;
import org.ggp.base.util.gdl.model.SentenceModelUtils;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;
import org.ggp.base.util.gdl.model.assignments.AssignmentsImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


/**
 * The CondensationIsolator is a GDL transformation designed to split up
 * rules in a way that results in smaller propnets. For example, we may
 * have a rule as follows:
 *
 * <pre>
 * (&lt;= (foo ?x ?y)
 *     (bar ?x ?y)
 *     (baz ?y ?z))
 * </pre>
 *
 * <p>In the propnet, this will result in one AND node for each combination
 * of ?x, ?y, and ?z. The CondensationIsolator would split it up as follows:
 *
 * <pre>
 * (&lt;= (foo ?x ?y)
 *     (bar ?x ?y)
 *     (baz_tmp0 ?y))
 * (&lt;= (baz_tmp0 ?y)
 *     (baz ?y ?z))
 * </pre>
 *
 * <p>In the propnet, there will now be one AND node for each combination of
 * ?x and ?y and one new link for each combination of ?y and ?z, but there
 * will not be a cross-product of the domains of all three.
 *
 * <p>"Condensation" refers to the type of rule generated, in which we simply
 * ignore certain variables.
 *
 * @author Alex Landau
 *
 */
public class CondensationIsolator {
    private CondensationIsolator() {
    }

    public static List<Gdl> run(List<Gdl> description) throws InterruptedException {
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
        //      (r4 ?a)
        //      (r5 ?c))
        //If we wanted to factor out ?a, we'd normally have to do
        /*  (<= (r6 ?b ?c)
         *      (r2 ?a ?b ?c)
         *      (r4 ?a))
         *  (<= (r1 ?b)
         *      (r6 ?b ?c)
         *      (r3 ?b ?c)
         *      (r5 ?c))
         * But if we know r2 is a mutex, instead we can do (notice r2 splitting):
         *  (<= (r6 ?b)
         *      (r2 ?a ?b ?c)
         *      (r4 ?a))
         *  (<= (r1 ?b)
         *      (r2 ?a ?b ?c)
         *      (r6 ?b)
         *      (r3 ?b ?c)
         *      (r5 ?c))
         * Which in turn becomes:
         *  (<= (r6 ?b)
         *      (r2 ?a ?b ?c)
         *      (r4 ?a))
         *  (<= (r7 ?b)
         *      (r2 ?a ?b ?c)
         *      (r3 ?b ?c)
         *      (r5 ?c))
         *  (<= (r1 ?b)
         *      (r6 ?b)
         *      (r7 ?b))
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

        for(Gdl gdl : description) {
            if(gdl instanceof GdlRule)
                rulesToAdd.add((GdlRule) gdl);
            else
                newDescription.add(gdl);
        }

        //Don't use the model indiscriminately; it reflects the old description,
        //not necessarily the new one
        SentenceDomainModel model = SentenceDomainModelFactory.createWithCartesianDomains(description);
        model = SentenceDomainModelOptimizer.restrictDomainsToUsefulValues(model);
        UnusedSentenceNameSource sentenceNameSource = UnusedSentenceNameSource.create(model);
        ConstantChecker constantChecker = ConstantCheckerFactory.createWithForwardChaining(model);

        Set<SentenceForm> constantForms = model.getConstantSentenceForms();

        ConcurrencyUtils.checkForInterruption();

        List<Gdl> curDescription = Lists.newArrayList(description);
        while(!rulesToAdd.isEmpty()) {
            GdlRule curRule = rulesToAdd.remove();
            if(isRecursive(curRule)) {
                //Don't mess with it!
                newDescription.add(curRule);
                continue;
            }
            GdlSentence curRuleHead = curRule.getHead();
            if(SentenceModelUtils.inSentenceFormGroup(curRuleHead, constantForms)) {
                newDescription.add(curRule);
                continue;
            }
            Set<GdlLiteral> condensationSet = getCondensationSet(curRule, model, constantChecker, sentenceNameSource);
            ConcurrencyUtils.checkForInterruption();
            if(condensationSet != null) {
                List<GdlRule> newRules = applyCondensation(condensationSet, curRule, sentenceNameSource);
                rulesToAdd.addAll(newRules);
                //Since we're making only small changes, we can readjust
                //the model as we go, instead of recomputing it
                List<GdlRule> oldRules = Collections.singletonList(curRule);
                List<Gdl> replacementDescription = Lists.newArrayList(curDescription);
                replacementDescription.removeAll(oldRules);
                replacementDescription.addAll(newRules);
                curDescription = replacementDescription;
                model = augmentModelWithNewForm(model, newRules);
            } else {
                newDescription.add(curRule);
            }
        }
        return newDescription;
    }

    @SuppressWarnings("unused")
    private static void saveKif(List<Gdl> description) {
        //Save the description in a new file
        //Useful for debugging chains of condensations to see
        //which cause decreased performance
        String filename = "ci0.kif";
        int filenum = 0;
        File file = null;
        while(file == null || file.exists()) {
            filenum++;
            filename = "ci" + filenum + ".kif";
            file = new File(filename);
            file = new File("games/rulesheets", filename);
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            for(Gdl gdl : description) {
                out.append(gdl.toString() + "\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {}
        }
    }

    private static boolean isRecursive(GdlRule rule) {
        for(GdlLiteral literal : rule.getBody())
            if(literal instanceof GdlSentence)
                if(((GdlSentence) literal).getName().equals(rule.getHead().getName()))
                    //A good approximation
                    return true;
        return false;
    }

    private static class UnusedSentenceNameSource {
        private final Set<String> allNamesSoFar;

        public UnusedSentenceNameSource(Collection<String> initialNames) {
            allNamesSoFar = Sets.newHashSet(initialNames);
        }

        public static UnusedSentenceNameSource create(SentenceFormModel model) {
            Set<String> sentenceFormNames = SentenceForms.getNames(model.getSentenceForms());
            return new UnusedSentenceNameSource(sentenceFormNames);
        }

        public GdlConstant getNameWithPrefix(GdlConstant prefix) {
            for(int i = 0; ; i++) {
                String candidateName = prefix + "_tmp" + i;
                if(!allNamesSoFar.contains(candidateName)) {
                    allNamesSoFar.add(candidateName);
                    return GdlPool.getConstant(candidateName);
                }
            }
        }
    }

    private static List<GdlRule> applyCondensation(
            Set<GdlLiteral> condensationSet, GdlRule rule,
            UnusedSentenceNameSource sentenceNameSource) {

        Set<GdlVariable> varsInCondensationSet = new HashSet<GdlVariable>();
        for(GdlLiteral literal : condensationSet)
            varsInCondensationSet.addAll(GdlUtils.getVariables(literal));
        Set<GdlVariable> varsToKeep = new HashSet<GdlVariable>();
        //Which vars do we "keep" (put in our new condensed literal)?
        //Vars that are both:
        //1) In the condensation set, in a non-mutex literal
        //2) Either in the head or somewhere else outside the condensation set
        for(GdlLiteral literal : condensationSet)
            varsToKeep.addAll(GdlUtils.getVariables(literal));
        Set<GdlVariable> varsToKeep2 = new HashSet<GdlVariable>();
        varsToKeep2.addAll(GdlUtils.getVariables(rule.getHead()));
        for(GdlLiteral literal : rule.getBody())
            if(!condensationSet.contains(literal))
                varsToKeep2.addAll(GdlUtils.getVariables(literal));
        varsToKeep.retainAll(varsToKeep2);

        //Now we're ready to split it apart
        //Let's make the new rule
        List<GdlTerm> orderedVars = new ArrayList<GdlTerm>(varsToKeep);
        GdlConstant condenserName = sentenceNameSource.getNameWithPrefix(rule.getHead().getName());
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

        List<GdlLiteral> remainingLiterals = new ArrayList<GdlLiteral>();
        for(GdlLiteral literal : rule.getBody())
            if(!condensationSet.contains(literal))
                remainingLiterals.add(literal);

        remainingLiterals.add(condenserHead);
        GdlRule modifiedRule = GdlPool.getRule(rule.getHead(), remainingLiterals);

        List<GdlRule> newRules = new ArrayList<GdlRule>(2);
        newRules.add(condenserRule);
        newRules.add(modifiedRule);
        return newRules;
    }

    private static Set<GdlLiteral> getCondensationSet(GdlRule rule,
            SentenceDomainModel model,
            ConstantChecker checker,
            UnusedSentenceNameSource sentenceNameSource) throws InterruptedException {
        //We use each variable as a starting point
        List<GdlVariable> varsInRule = GdlUtils.getVariables(rule);
        List<GdlVariable> varsInHead = GdlUtils.getVariables(rule.getHead());
        List<GdlVariable> varsNotInHead = new ArrayList<GdlVariable>(varsInRule);
        varsNotInHead.removeAll(varsInHead);

        for(GdlVariable var : varsNotInHead) {
            ConcurrencyUtils.checkForInterruption();

            Set<GdlLiteral> minSet = new HashSet<GdlLiteral>();
            for(GdlLiteral literal : rule.getBody())
                if(GdlUtils.getVariables(literal).contains(var))
                    minSet.add(literal);

            //#1 is already done
            //Now we try #2
            Set<GdlVariable> varsNeeded = new HashSet<GdlVariable>();
            Set<GdlVariable> varsSupplied = new HashSet<GdlVariable>();
            for(GdlLiteral literal : minSet)
                if(literal instanceof GdlRelation)
                    varsSupplied.addAll(GdlUtils.getVariables(literal));
                else if(literal instanceof GdlDistinct || literal instanceof GdlNot)
                    varsNeeded.addAll(GdlUtils.getVariables(literal));
            varsNeeded.removeAll(varsSupplied);
            if(!varsNeeded.isEmpty())
                continue;

            List<Set<GdlLiteral>> candidateSuppliersList = new ArrayList<Set<GdlLiteral>>();
            for(GdlVariable varNeeded : varsNeeded) {
                Set<GdlLiteral> suppliers = new HashSet<GdlLiteral>();
                for(GdlLiteral literal : rule.getBody())
                    if(literal instanceof GdlRelation)
                        if(GdlUtils.getVariables(literal).contains(varNeeded))
                            suppliers.add(literal);
                candidateSuppliersList.add(suppliers);
            }

            //TODO: Now... I'm not sure if we want to minimize the number of
            //literals added, or the number of variables added
            //Right now, I don't have time to worry about optimization
            //Currently, we pick one at random
            //TODO: Optimize this
            Set<GdlLiteral> literalsToAdd = new HashSet<GdlLiteral>();
            for(Set<GdlLiteral> suppliers : candidateSuppliersList)
                if(Collections.disjoint(suppliers, literalsToAdd))
                    literalsToAdd.add(suppliers.iterator().next());
            minSet.addAll(literalsToAdd);

            if(goodCondensationSetByHeuristic(minSet, rule, model, checker, sentenceNameSource))
                return minSet;

        }
        return null;
    }

    private static boolean goodCondensationSetByHeuristic(
            Set<GdlLiteral> minSet, GdlRule rule, SentenceDomainModel model,
            ConstantChecker checker,
            UnusedSentenceNameSource sentenceNameSource) throws InterruptedException {
        //We actually want the sentence model here so we can see the domains
        //also, if it's a constant, ...
        //Anyway... we want to compare the heuristic for the number of assignments
        //and/or links that will be generated with or without the condensation set
        //Heuristic for a rule is A*(L+1), where A is the number of assignments and
        //L is the number of literals, unless L = 1, in which case the heuristic is
        //just A. This roughly captures the number of links that would be generated
        //if this rule were to be generated.
        //Obviously, there are differing degrees of accuracy with which we can
        //represent A.
        //One way is taking the product of all the variables in all the domains.
        //However, we can do better by actually asking the Assignments class for
        //its own heuristic of how it would implement the rule as-is.
        //The only tricky aspect here is that we need an up-to-date SentenceModel,
        //and in some cases this could be expensive to compute. Might as well try
        //it, though...

        //Heuristic for the rule as-is:

        long assignments = AssignmentsImpl.getNumAssignmentsEstimate(rule,
                SentenceDomainModels.getVarDomains(rule, model, VarDomainOpts.INCLUDE_HEAD),
                checker);
        int literals = rule.arity();
        if(literals > 1)
            literals++; //We have to "and" the literals together
        //Note that even though constants will be factored out, we're concerned here
        //with getting through them in a reasonable amount of time, so we do want to
        //count them. TODO: Not sure if they should be counted in L, though...
        long curRuleHeuristic = assignments * literals;
        //And if we split them up...
        List<GdlRule> newRules = applyCondensation(minSet, rule, sentenceNameSource);
        GdlRule r1 = newRules.get(0), r2 = newRules.get(1);

        //Augment the model
        SentenceDomainModel newModel = augmentModelWithNewForm(model, newRules);

        long a1 = AssignmentsImpl.getNumAssignmentsEstimate(r1,
                SentenceDomainModels.getVarDomains(r1, newModel, VarDomainOpts.INCLUDE_HEAD), checker);
        long a2 = AssignmentsImpl.getNumAssignmentsEstimate(r2,
                SentenceDomainModels.getVarDomains(r2, newModel, VarDomainOpts.INCLUDE_HEAD), checker);
        int l1 = r1.arity(); if(l1 > 1) l1++;
        int l2 = r2.arity(); if(l2 > 1) l2++;

        //Whether we split or not depends on what the two heuristics say
        long newRulesHeuristic = a1 * l1 + a2 * l2;
        return newRulesHeuristic < curRuleHeuristic;
    }

    private static SentenceDomainModel augmentModelWithNewForm(
            final SentenceDomainModel oldModel, List<GdlRule> newRules) {
        final SentenceForm newForm = SimpleSentenceForm.create(newRules.get(0).getHead());
        final SentenceFormDomain newFormDomain = getNewFormDomain(newRules.get(0), oldModel, newForm);
        return new SentenceDomainModel() {
            @Override
            public SentenceFormDomain getDomain(SentenceForm form) {
                if (form.equals(newForm)) {
                    return newFormDomain;
                }
                return oldModel.getDomain(form);
            }

            @Override
            public Set<SentenceForm> getIndependentSentenceForms() {
                throw new UnsupportedOperationException();
            }
            @Override
            public Set<SentenceForm> getConstantSentenceForms() {
                throw new UnsupportedOperationException();
            }
            @Override
            public Multimap<SentenceForm, SentenceForm> getDependencyGraph() {
                throw new UnsupportedOperationException();
            }
            @Override
            public Set<GdlSentence> getSentencesListedAsTrue(SentenceForm form) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Set<GdlRule> getRules(SentenceForm form) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Set<SentenceForm> getSentenceForms() {
                throw new UnsupportedOperationException();
            }
            @Override
            public List<Gdl> getDescription() {
                throw new UnsupportedOperationException();
            }
            @Override
            public SentenceForm getSentenceForm(GdlSentence sentence) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static SentenceFormDomain getNewFormDomain(GdlRule condensingRule,
            SentenceDomainModel oldModel, SentenceForm newForm) {
        Map<GdlVariable, Set<GdlConstant>> varDomains = SentenceDomainModels.getVarDomains(
                condensingRule, oldModel, VarDomainOpts.BODY_ONLY);

        List<Set<GdlConstant>> domainsForSlots = Lists.newArrayList();
        for (GdlTerm term : GdlUtils.getTupleFromSentence(condensingRule.getHead())) {
            if (!(term instanceof GdlVariable)) {
                throw new RuntimeException("Expected all slots in the head of a condensing rule to be variables, but the rule was: " + condensingRule);
            }
            domainsForSlots.add(varDomains.get(term));
        }
        return CartesianSentenceFormDomain.create(newForm, domainsForSlots);
    }
}
