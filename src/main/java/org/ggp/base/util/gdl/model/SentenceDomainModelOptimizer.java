package org.ggp.base.util.gdl.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.GdlVisitor;
import org.ggp.base.util.gdl.GdlVisitors;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.SentenceDomainModels.VarDomainOpts;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class SentenceDomainModelOptimizer {
    private SentenceDomainModelOptimizer() {
    }

    /**
     * Given a SentenceDomainModel, returns an ImmutableSentenceDomainModel
     * with Cartesian domains that tries to minimize the domains of sentence
     * forms without impacting the game rules. In particular, when sentences
     * are restricted to these domains, the answers to queries about terminal,
     * legal, goal, next, and init sentences will not change.
     *
     * Note that if a sentence form is not used in a meaningful way by the
     * game, it may end up with an empty domain.
     *
     * The description for the game must have had the {@link VariableConstrainer}
     * applied to it.
     */
    public static ImmutableSentenceDomainModel restrictDomainsToUsefulValues(SentenceDomainModel oldModel) throws InterruptedException {
        // Start with everything from the current domain model.
        Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededAndPossibleConstantsByForm = Maps.newHashMap();
        for (SentenceForm form : oldModel.getSentenceForms()) {
            neededAndPossibleConstantsByForm.put(form, HashMultimap.<Integer, GdlConstant>create());
            addDomain(neededAndPossibleConstantsByForm.get(form), oldModel.getDomain(form), form);
        }

        /*
         * To minimize the contents of the domains, we repeatedly go through two processes to reduce
         * the domain:
         *
         * 1) We remove unneeded constants from the domain. These are constants which (in their
         *    position) do not contribute to any sentences with a GDL keyword as its name; that
         *    is, it never matters whether a sentence with that constant in that position is
         *    true or false.
         * 2) We remove impossible constants from the domain. These are constants which cannot
         *    end up in their position via any rule or sentence in the game description, given
         *    the current domain.
         *
         * Constants removed because of one type of pass or the other may cause other constants
         * in other sentence forms to become unneeded or impossible, so we make multiple passes
         * until everything is stable.
         */
        boolean somethingChanged = true;
        while (somethingChanged) {
            somethingChanged = removeUnneededConstants(neededAndPossibleConstantsByForm, oldModel);
            somethingChanged |= removeImpossibleConstants(neededAndPossibleConstantsByForm, oldModel);
        }

        return toSentenceDomainModel(neededAndPossibleConstantsByForm, oldModel);
    }

    private static void addDomain(
            SetMultimap<Integer, GdlConstant> setMultimap,
            SentenceFormDomain domain,
            SentenceForm form) {
        for (int i = 0; i < form.getTupleSize(); i++) {
            setMultimap.putAll(i, domain.getDomainForSlot(i));
        }
    }

    private static boolean removeImpossibleConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm = Maps.newHashMap();
        for (SentenceForm form : curDomains.keySet()) {
            newPossibleConstantsByForm.put(form, HashMultimap.<Integer, GdlConstant>create());
        }
        populateInitialPossibleConstants(newPossibleConstantsByForm, curDomains, model);

        boolean somethingChanged = true;
        while (somethingChanged) {
            somethingChanged = propagatePossibleConstants(newPossibleConstantsByForm, curDomains, model);
        }

        return retainNewDomains(curDomains, newPossibleConstantsByForm);
    }

    private static void populateInitialPossibleConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        //Add anything in the head of a rule...
        for (GdlRule rule : getRules(model.getDescription())) {
            GdlSentence head = rule.getHead();

            addConstantsFromSentenceIfInOldDomain(newPossibleConstantsByForm, curDomains, model, head);
        }
        //... and any true sentences
        for (SentenceForm form : model.getSentenceForms()) {
            for (GdlSentence sentence : model.getSentencesListedAsTrue(form)) {
                addConstantsFromSentenceIfInOldDomain(newPossibleConstantsByForm, curDomains, model, sentence);
            }
        }
    }

    private static boolean propagatePossibleConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomain,
            SentenceFormModel model) throws InterruptedException {
        //Injection: Go from the intersections of variable values in rules to the
        //values in their heads
        boolean somethingChanged = false;

        for (GdlRule rule : getRules(model.getDescription())) {
            GdlSentence head = rule.getHead();

            Map<GdlVariable, Set<GdlConstant>> domainsOfHeadVars = Maps.newHashMap();
            for (GdlVariable varInHead : ImmutableSet.copyOf(GdlUtils.getVariables(rule.getHead()))) {
                Set<GdlConstant> domain = getVarDomainInRuleBody(varInHead, rule, newPossibleConstantsByForm, curDomain, model);
                domainsOfHeadVars.put(varInHead, domain);
                somethingChanged |= addPossibleValuesToSentence(domain, head, varInHead, newPossibleConstantsByForm, model);
            }
        }

        //Language-based injections
        somethingChanged |= applyLanguageBasedInjections(GdlPool.INIT, GdlPool.TRUE, newPossibleConstantsByForm);
        somethingChanged |= applyLanguageBasedInjections(GdlPool.NEXT, GdlPool.TRUE, newPossibleConstantsByForm);
        somethingChanged |= applyLanguageBasedInjections(GdlPool.LEGAL, GdlPool.DOES, newPossibleConstantsByForm);

        return somethingChanged;
    }

    private static boolean applyLanguageBasedInjections(
            GdlConstant curName,
            GdlConstant resultingName,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm) throws InterruptedException {
        boolean somethingChanged = false;
        for (SentenceForm form : newPossibleConstantsByForm.keySet()) {
            ConcurrencyUtils.checkForInterruption();
            if (form.getName() == curName) {
                SentenceForm resultingForm = form.withName(resultingName);

                SetMultimap<Integer, GdlConstant> curFormDomain = newPossibleConstantsByForm.get(form);
                SetMultimap<Integer, GdlConstant> resultingFormDomain = newPossibleConstantsByForm.get(resultingForm);

                somethingChanged |= resultingFormDomain.putAll(curFormDomain);
            }
        }
        return somethingChanged;
    }

    private static Set<GdlConstant> getVarDomainInRuleBody(
            GdlVariable varInHead,
            GdlRule rule,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomain,
            SentenceFormModel model) {
        try {
            List<Set<GdlConstant>> domains = Lists.newArrayList();
            for (GdlSentence conjunct : getPositiveConjuncts(rule.getBody())) {
                if (GdlUtils.getVariables(conjunct).contains(varInHead)) {
                    domains.add(getVarDomainInSentence(varInHead, conjunct, newPossibleConstantsByForm, curDomain, model));
                }
            }
            return getIntersection(domains);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in rule " + rule + " for variable " + varInHead, e);
        }
    }

    private static Set<GdlConstant> getVarDomainInSentence(
            GdlVariable var,
            GdlSentence conjunct,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newPossibleConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomain,
            SentenceFormModel model) {
        SentenceForm form = model.getSentenceForm(conjunct);
        List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(conjunct);

        List<Set<GdlConstant>> domains = Lists.newArrayList();
        for (int i = 0; i < tuple.size(); i++) {
            if (tuple.get(i) == var) {
                domains.add(newPossibleConstantsByForm.get(form).get(i));
                domains.add(curDomain.get(form).get(i));
            }
        }
        return getIntersection(domains);
    }

    private static Set<GdlConstant> getIntersection(
            List<Set<GdlConstant>> domains) {
        if (domains.isEmpty()) {
            throw new IllegalArgumentException("Unsafe rule has no positive conjuncts");
        }
        Set<GdlConstant> intersection = Sets.newHashSet(domains.get(0));
        for (int i = 1; i < domains.size(); i++) {
            Set<GdlConstant> curDomain = domains.get(i);
            intersection.retainAll(curDomain);
        }
        return intersection;
    }

    private static boolean removeUnneededConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newNeededConstantsByForm = Maps.newHashMap();
        for (SentenceForm form : curDomains.keySet()) {
            newNeededConstantsByForm.put(form, HashMultimap.<Integer, GdlConstant>create());
        }
        populateInitialNeededConstants(newNeededConstantsByForm, curDomains, model);

        boolean somethingChanged = true;
        while (somethingChanged) {
            somethingChanged = propagateNeededConstants(newNeededConstantsByForm, curDomains, model);
        }

        return retainNewDomains(curDomains, newNeededConstantsByForm);
    }

    private static boolean retainNewDomains(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newDomains) {
        boolean somethingChanged = false;
        for (SentenceForm form : curDomains.keySet()) {
            SetMultimap<Integer, GdlConstant> newDomain = newDomains.get(form);
            somethingChanged |= curDomains.get(form).entries().retainAll(newDomain.entries());
        }
        return somethingChanged;
    }

    private static boolean propagateNeededConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        boolean somethingChanged = false;

        somethingChanged |= applyRuleHeadPropagation(neededConstantsByForm, curDomains, model);
        somethingChanged |= applyRuleBodyOnlyPropagation(neededConstantsByForm, curDomains, model);

        return somethingChanged;
    }


    private static boolean applyRuleBodyOnlyPropagation(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        boolean somethingChanged = false;
        //If a variable does not appear in the head of a variable,
        //then all the values that are in the intersections of all the
        //domains from the positive conjuncts containing the variable
        //become needed.

        for (GdlRule rule : getRules(model.getDescription())) {
            GdlSentence head = rule.getHead();
            Set<GdlVariable> varsInHead = ImmutableSet.copyOf(GdlUtils.getVariables(head));

            Map<GdlVariable, Set<GdlConstant>> varDomains = getVarDomains(rule, curDomains, model);
            for (GdlVariable var : ImmutableSet.copyOf(GdlUtils.getVariables(rule))) {
                if (!varsInHead.contains(var)) {
                    Set<GdlConstant> neededConstants = varDomains.get(var);
                    if (neededConstants == null) {
                        throw new IllegalStateException("var is " + var + ";\nvarDomains key set is " + varDomains.keySet() + ";\nvarsInHead is " + varsInHead +
                                ";\nrule is " + rule);
                    }
                    for (GdlLiteral conjunct : rule.getBody()) {
                        somethingChanged |=
                                addPossibleValuesToConjunct(neededConstants, conjunct, var, neededConstantsByForm, model);
                    }
                }
            }
        }
        return somethingChanged;
    }

    private static Map<GdlVariable, Set<GdlConstant>> getVarDomains(
            GdlRule rule,
            final Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            final SentenceFormModel model) {
        return SentenceDomainModels.getVarDomains(rule, new AbstractSentenceDomainModel(model) {
            @Override
            public SentenceFormDomain getDomain(final SentenceForm form) {
                return new SentenceFormDomain() {
                    @Override
                    public SentenceForm getForm() {
                        return form;
                    }

                    @Override
                    public Iterator<GdlSentence> iterator() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Set<GdlConstant> getDomainForSlot(int slotIndex) {
                        if (!curDomains.containsKey(form)) {
                            return ImmutableSet.of();
                        }
                        return curDomains.get(form).get(slotIndex);
                    }
                };
            }}, VarDomainOpts.INCLUDE_HEAD);
    }

    private static boolean applyRuleHeadPropagation(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        boolean somethingChanged = false;
        //If a term that is a variable in the head of a rule needs a
        //particular value, AND that variable is possible (i.e. in the
        //current domain) in every appearance of the variable in
        //positive conjuncts in the rule's body, then the value is
        //needed in every appearance of the variable in the rule
        //(positive or negative).
        for (GdlRule rule : getRules(model.getDescription())) {
            GdlSentence head = rule.getHead();
            SentenceForm headForm = model.getSentenceForm(head);
            List<GdlTerm> headTuple = GdlUtils.getTupleFromSentence(head);

            Map<GdlVariable, Set<GdlConstant>> varDomains = getVarDomains(rule, curDomains, model);

            for (int i = 0; i < headTuple.size(); i++) {
                ConcurrencyUtils.checkForInterruption();
                if (headTuple.get(i) instanceof GdlVariable) {
                    GdlVariable curVar = (GdlVariable) headTuple.get(i);
                    Set<GdlConstant> neededConstants = neededConstantsByForm.get(headForm).get(i);

                    //Whittle these down based on what's possible throughout the rule
                    Set<GdlConstant> neededAndPossibleConstants = Sets.newHashSet(neededConstants);
                    neededAndPossibleConstants.retainAll(varDomains.get(curVar));
                    //Relay those values back to the conjuncts in the rule body
                    for (GdlLiteral conjunct : rule.getBody()) {
                        somethingChanged |= addPossibleValuesToConjunct(neededAndPossibleConstants, conjunct, curVar, neededConstantsByForm, model);
                    }
                }
            }
        }
        return somethingChanged;
    }

    private static boolean addPossibleValuesToConjunct(
            Set<GdlConstant> neededAndPossibleConstants,
            GdlLiteral conjunct,
            GdlVariable curVar,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededConstantsByForm,
            SentenceFormModel model) throws InterruptedException {
        if (conjunct instanceof GdlSentence) {
            return addPossibleValuesToSentence(neededAndPossibleConstants, (GdlSentence) conjunct, curVar, neededConstantsByForm, model);
        } else if (conjunct instanceof GdlNot) {
            GdlSentence innerSentence = (GdlSentence) ((GdlNot) conjunct).getBody();
            return addPossibleValuesToSentence(neededAndPossibleConstants, innerSentence, curVar, neededConstantsByForm, model);
        } else if (conjunct instanceof GdlOr) {
            throw new IllegalArgumentException("The SentenceDomainModelOptimizer is not designed for game descriptions with OR. Use the DeORer.");
        } else if (conjunct instanceof GdlDistinct) {
            return false;
        } else {
            throw new IllegalArgumentException("Unexpected literal type " + conjunct.getClass() + " for literal " + conjunct);
        }
    }

    private static boolean addPossibleValuesToSentence(
            Set<GdlConstant> neededAndPossibleConstants,
            GdlSentence sentence,
            GdlVariable curVar,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededConstantsByForm,
            SentenceFormModel model) throws InterruptedException {
        ConcurrencyUtils.checkForInterruption();
        boolean somethingChanged = false;

        SentenceForm form = model.getSentenceForm(sentence);
        List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
        Preconditions.checkArgument(form.getTupleSize() == tuple.size());

        for (int i = 0; i < tuple.size(); i++) {
            if (tuple.get(i) == curVar) {
                Preconditions.checkNotNull(neededConstantsByForm.get(form));
                Preconditions.checkNotNull(neededAndPossibleConstants);
                somethingChanged |= neededConstantsByForm.get(form).putAll(i, neededAndPossibleConstants);
            }
        }
        return somethingChanged;
    }

    private static Iterable<GdlSentence> getPositiveConjuncts(List<GdlLiteral> body) {
        return Iterables.transform(Iterables.filter(body, new Predicate<GdlLiteral>() {
            @Override
            public boolean apply(GdlLiteral input) {
                return input instanceof GdlSentence;
            }
        }), new Function<GdlLiteral, GdlSentence>() {
            @Override
            public GdlSentence apply(GdlLiteral input) {
                return (GdlSentence) input;
            }
        });
    }

    // Unlike getPositiveConjuncts, this also returns sentences inside NOT literals.
    private static List<GdlSentence> getAllSentencesInBody(List<GdlLiteral> body) {
        final List<GdlSentence> sentences = Lists.newArrayList();
        GdlVisitors.visitAll(body, new GdlVisitor() {
            @Override
            public void visitSentence(GdlSentence sentence) {
                sentences.add(sentence);
            }
        });
        return sentences;
    }

    private static Iterable<GdlRule> getRules(List<Gdl> description) {
        return Iterables.transform(Iterables.filter(description, new Predicate<Gdl>() {
            @Override
            public boolean apply(Gdl input) {
                return input instanceof GdlRule;
            }
        }), new Function<Gdl, GdlRule>() {
            @Override
            public GdlRule apply(Gdl input) {
                return (GdlRule) input;
            }
        });
    }

    private static final ImmutableSet<GdlConstant> ALWAYS_NEEDED_SENTENCE_NAMES = ImmutableSet.of(
            GdlPool.NEXT,
            GdlPool.GOAL,
            GdlPool.LEGAL,
            GdlPool.INIT,
            GdlPool.ROLE,
            GdlPool.BASE,
            GdlPool.INPUT,
            GdlPool.TRUE,
            GdlPool.DOES);
    private static void populateInitialNeededConstants(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newNeededConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> curDomains,
            SentenceFormModel model) throws InterruptedException {
        // If the term model is part of a keyword-named sentence,
        // then it is needed. This includes base and init.
        for (SentenceForm form : model.getSentenceForms()) {
            ConcurrencyUtils.checkForInterruption();

            GdlConstant name = form.getName();
            if (ALWAYS_NEEDED_SENTENCE_NAMES.contains(name)) {
                newNeededConstantsByForm.get(form).putAll(curDomains.get(form));
            }
        }

        // If the term has a constant value in some sentence in the
        // BODY of a rule, then it is needed.
        for (GdlRule rule : getRules(model.getDescription())) {
            for (GdlSentence sentence : getAllSentencesInBody(rule.getBody())) {
                addConstantsFromSentenceIfInOldDomain(newNeededConstantsByForm, curDomains, model, sentence);
            }
        }
    }

    private static void addConstantsFromSentenceIfInOldDomain(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> newConstantsByForm,
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> oldDomain,
            SentenceFormModel model, GdlSentence sentence) throws InterruptedException {
        SentenceForm form = model.getSentenceForm(sentence);
        List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
        if (tuple.size() != form.getTupleSize()) {
            throw new IllegalStateException();
        }

        for (int i = 0; i < form.getTupleSize(); i++) {
            ConcurrencyUtils.checkForInterruption();

            GdlTerm term = tuple.get(i);
            if (term instanceof GdlConstant) {
                Set<GdlConstant> oldDomainForTerm = oldDomain.get(form).get(i);
                if (oldDomainForTerm.contains(term)) {
                    newConstantsByForm.get(form).put(i, (GdlConstant) term);
                }
            }
        }
    }

    private static ImmutableSentenceDomainModel toSentenceDomainModel(
            Map<SentenceForm, SetMultimap<Integer, GdlConstant>> neededAndPossibleConstantsByForm,
            SentenceFormModel formModel) throws InterruptedException {
        Map<SentenceForm, SentenceFormDomain> domains = Maps.newHashMap();
        for (SentenceForm form : formModel.getSentenceForms()) {
            ConcurrencyUtils.checkForInterruption();
            domains.put(form, CartesianSentenceFormDomain.create(form,
                    neededAndPossibleConstantsByForm.get(form)));
        }

        return ImmutableSentenceDomainModel.create(formModel, domains);
    }
}
