package org.ggp.base.util.gdl.transforms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.GdlVisitor;
import org.ggp.base.util.gdl.GdlVisitors;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.gdl.model.ImmutableSentenceFormModel;
import org.ggp.base.util.gdl.model.SentenceForm;
import org.ggp.base.util.gdl.model.SentenceFormModel;
import org.ggp.base.util.gdl.model.SentenceFormModelFactory;
import org.ggp.base.util.gdl.model.SimpleSentenceForm;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class VariableConstrainer {
    private VariableConstrainer() {
    }

    /**
     * Modifies a GDL description by replacing all rules in which variables could be bound to
     * functions, so that the new rules will only bind constants to variables. Also automatically
     * removes GdlOrs from the rules using the DeORer.
     *
     * Not guaranteed to work if the GDL is written strangely, such as when they include rules
     * in which certain conjuncts are never or always true. Not guaranteed to work when rules
     * are unsafe, i.e., they contain variables only appearing in the head, a negated literal,
     * and/or a distinct literal. (In fact, this can be a good way to test for GDL errors, which
     * often result in exceptions.)
     *
     * Not guaranteed to finish in a reasonable amount of time in pathological cases, where the
     * number of possible functional structures is prohibitively large.
     *
     * @param description A GDL game description.
     * @return A modified version of the same game.
     */
    public static List<Gdl> replaceFunctionValuedVariables(List<Gdl> description) throws InterruptedException {
        description = GdlCleaner.run(description);
        description = DeORer.run(description);
        SentenceFormModel model = SentenceFormModelFactory.create(description);

        // Find "ambiguities" between sentence rules: "If we have sentence form X
        // with variables in slots [...], it could be aliased to sentence form Y instead"
        ListMultimap<SentenceForm, Ambiguity> ambiguitiesByOriginalForm = getAmbiguitiesByOriginalForm(model);
        if (ambiguitiesByOriginalForm.isEmpty()) {
            return description;
        }

        List<Gdl> expandedRules = applyAmbiguitiesToRules(description, ambiguitiesByOriginalForm, model);
        return removeDuplicates(cleanUpIrrelevantRules(expandedRules));
    }

    private static List<Gdl> removeDuplicates(List<Gdl> rules) {
        Set<Gdl> alreadyInRules = Sets.newHashSet();
        List<Gdl> newRules = Lists.newArrayList();
        for (Gdl rule : rules) {
            if (alreadyInRules.contains(rule)) {
                continue;
            }
            newRules.add(rule);
            alreadyInRules.add(rule);
        }
        return newRules;
    }

    /**
     * An ambiguity represents a particular relationship between two
     * sentence forms. It says that if sentence form "original" appears
     * in a rule and has GdlVariables in particular slots, it could be
     * equivalent to the sentence form "replacement" if functions are
     * assigned to its variables.
     *
     * The goal of this transformation is to make it possible for users
     * of the game description to treat it as if functions could not be
     * assigned to variables. This requires adding or modifying rules to
     * account for the extra cases.
     */
    private static class Ambiguity {
        private final SentenceForm original;
        private final ImmutableMap<Integer, GdlFunction> replacementsByOriginalTupleIndex;
        private final SentenceForm replacement;

        private Ambiguity(SentenceForm original,
                ImmutableMap<Integer, GdlFunction> replacementsByOriginalTupleIndex,
                SentenceForm replacement) {
            Preconditions.checkNotNull(original);
            Preconditions.checkNotNull(replacementsByOriginalTupleIndex);
            Preconditions.checkArgument(!replacementsByOriginalTupleIndex.isEmpty());
            Preconditions.checkNotNull(replacement);
            for (int varIndex : replacementsByOriginalTupleIndex.keySet()) {
                Preconditions.checkElementIndex(varIndex, original.getTupleSize());
            }
            this.original = original;
            this.replacementsByOriginalTupleIndex = replacementsByOriginalTupleIndex;
            this.replacement = replacement;
        }

        public static Ambiguity create(SentenceForm original,
                Map<Integer, GdlFunction> replacementsByOriginalTupleIndex,
                SentenceForm replacement) {
            return new Ambiguity(original,
                    ImmutableMap.copyOf(replacementsByOriginalTupleIndex),
                    replacement);
        }

        public SentenceForm getOriginal() {
            return original;
        }

        public SentenceForm getReplacement() {
            return replacement;
        }

        @Override
        public String toString() {
            return "Ambiguity [original=" + original
                    + ", replacementsByOriginalTupleIndex="
                    + replacementsByOriginalTupleIndex + ", replacement="
                    + replacement + "]";
        }

        /**
         * Returns true iff the given sentence could correspond to a
         * sentence of the replacement form, for some variable assignment.
         */
        public boolean applies(GdlSentence sentence) {
            if (!original.matches(sentence)) {
                return false;
            }

            List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
            for (int varIndex : replacementsByOriginalTupleIndex.keySet()) {
                if (!(tuple.get(varIndex) instanceof GdlVariable)) {
                    return false;
                }
            }
            return true;
        }

        public Map<GdlVariable, GdlTerm> getReplacementAssignment(GdlSentence sentence, UnusedVariableGenerator varGen) {
            Preconditions.checkArgument(applies(sentence));

            Map<GdlVariable, GdlTerm> assignment = Maps.newHashMap();
            List<GdlTerm> tuple = GdlUtils.getTupleFromSentence(sentence);
            for (int varIndex : replacementsByOriginalTupleIndex.keySet()) {
                GdlFunction function = replacementsByOriginalTupleIndex.get(varIndex);

                GdlFunction replacementFunction = varGen.replaceVariablesAndConstants(function);
                assignment.put((GdlVariable) tuple.get(varIndex), replacementFunction);
            }
            return assignment;
        }
    }

    private static ListMultimap<SentenceForm, Ambiguity> getAmbiguitiesByOriginalForm(
            SentenceFormModel model) throws InterruptedException {
        ListMultimap<SentenceForm, Ambiguity> result = ArrayListMultimap.create();
        ListMultimap<GdlConstant, SentenceForm> formsByName = getFormsByName(model);

        for (GdlConstant name : formsByName.keySet()) {
            List<SentenceForm> forms = formsByName.get(name);
            for (SentenceForm form : forms) {
                result.putAll(form, getAmbiguities(form, forms));
            }
        }

        Set<SentenceForm> allForms = ImmutableSet.copyOf(formsByName.values());
        for (Ambiguity ambiguity : result.values()) {
            Preconditions.checkState(allForms.contains(ambiguity.getOriginal()));
            Preconditions.checkState(allForms.contains(ambiguity.getReplacement()));
        }

        return result;
    }

    private static ListMultimap<GdlConstant, SentenceForm> getFormsByName(
            SentenceFormModel model) {
        return Multimaps.index(getAllSentenceForms(model), new Function<SentenceForm, GdlConstant>() {
            @Override
            public GdlConstant apply(SentenceForm input) {
                return input.getName();
            }
        });
    }

    private static Set<SentenceForm> getAllSentenceForms(SentenceFormModel model) {
        //The model may only have sentence forms for sentences that can actually be
        //true. It may be missing sentence forms that are used in the rules only,
        //with no actual corresponding sentences. We want to make sure these are
        //included.
        final Set<SentenceForm> forms = Sets.newHashSet(model.getSentenceForms());
        GdlVisitors.visitAll(model.getDescription(), new GdlVisitor() {
            @Override
            public void visitSentence(GdlSentence sentence) {
                forms.add(SimpleSentenceForm.create(sentence));
            }
        });
        return forms;
    }

    private static List<Ambiguity> getAmbiguities(
            SentenceForm original, List<SentenceForm> forms) throws InterruptedException {
        List<Ambiguity> result = Lists.newArrayList();
        for (SentenceForm form : forms) {
            if (form == original) {
                continue;
            }

            Optional<Ambiguity> ambiguity = findAmbiguity(original, form);
            if (ambiguity.isPresent()) {
                result.add(ambiguity.get());
            }
        }
        return result;
    }

    private static Optional<Ambiguity> findAmbiguity(SentenceForm original,
            SentenceForm replacement) throws InterruptedException {
        Preconditions.checkArgument(original.getName() == replacement.getName());
        Preconditions.checkArgument(!original.equals(replacement));
        ConcurrencyUtils.checkForInterruption();

        Map<Integer, GdlFunction> replacementsByOriginalTupleIndex = Maps.newHashMap();
        //Make the arguments ?v0, ?v1, ?v2, ... so we can find the tuple indices easily
        GdlSentence originalSentence =
                original.getSentenceFromTuple(getNumberedTuple(original.getTupleSize()));
        GdlSentence replacementSentence =
                replacement.getSentenceFromTuple(getNumberedTuple(replacement.getTupleSize()));

        boolean success = findAmbiguity(originalSentence.getBody(), replacementSentence.getBody(),
                replacementsByOriginalTupleIndex);
        if (success) {
            return Optional.of(Ambiguity.create(original, replacementsByOriginalTupleIndex, replacement));
        } else {
            return Optional.absent();
        }
    }

    private static boolean findAmbiguity(List<GdlTerm> originalBody,
            List<GdlTerm> replacementBody, Map<Integer, GdlFunction> replacementsByOriginalTupleIndex) throws InterruptedException {
        if (originalBody.size() != replacementBody.size()) {
            return false;
        }
        for (int i = 0; i < originalBody.size(); i++) {
            ConcurrencyUtils.checkForInterruption();

            GdlTerm originalTerm = originalBody.get(i);
            GdlTerm replacementTerm = replacementBody.get(i);
            if (replacementTerm instanceof GdlVariable) {
                if (!(originalTerm instanceof GdlVariable)) {
                    return false;
                }
            } else if (replacementTerm instanceof GdlFunction) {
                if (originalTerm instanceof GdlVariable) {
                    int varIndex = Integer.valueOf(originalTerm.toString().replace("?v", ""));
                    replacementsByOriginalTupleIndex.put(varIndex, (GdlFunction) replacementTerm);
                } else if (originalTerm instanceof GdlFunction) {
                    GdlFunction originalFunction = (GdlFunction) originalTerm;
                    GdlFunction replacementFunction = (GdlFunction) replacementTerm;
                    if (originalFunction.getName() != replacementFunction.getName()) {
                        return false;
                    }

                    boolean successSoFar = findAmbiguity(originalFunction.getBody(),
                            replacementFunction.getBody(),
                            replacementsByOriginalTupleIndex);
                    if (!successSoFar) {
                        return false;
                    }
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        }
        return true;
    }

    private static List<GdlVariable> getNumberedTuple(int tupleSize) {
        List<GdlVariable> result = Lists.newArrayList();
        for (int i = 0; i < tupleSize; i++) {
            result.add(GdlPool.getVariable("?v" + Integer.toString(i)));
        }
        return result;
    }

    private static List<Gdl> applyAmbiguitiesToRules(List<Gdl> description,
            ListMultimap<SentenceForm, Ambiguity> ambiguitiesByOriginalForm,
            SentenceFormModel model) throws InterruptedException {
        ImmutableList.Builder<Gdl> result = ImmutableList.builder();

        for (Gdl gdl : description) {
            if (gdl instanceof GdlRule) {
                result.addAll(applyAmbiguities((GdlRule) gdl, ambiguitiesByOriginalForm, model));
            } else {
                result.add(gdl);
            }
        }

        return result.build();
    }

    private static List<GdlRule> applyAmbiguities(GdlRule originalRule,
            ListMultimap<SentenceForm, Ambiguity> ambiguitiesByOriginalForm,
            SentenceFormModel model) throws InterruptedException {
        List<GdlRule> rules = Lists.newArrayList(originalRule);
        //Each literal can potentially multiply the number of rules we have, so
        //we apply each literal separately to the entire list of rules so far.
        for (GdlLiteral literal : Iterables.concat(ImmutableSet.of(originalRule.getHead()),
                originalRule.getBody())) {
            List<GdlRule> newRules = Lists.newArrayList();
            for (GdlRule rule : rules) {
                Preconditions.checkArgument(originalRule.arity() == rule.arity());
                newRules.addAll(applyAmbiguitiesForLiteral(literal, rule, ambiguitiesByOriginalForm, model));
            }
            rules = newRules;
        }
        return rules;
    }

    private static List<GdlRule> applyAmbiguitiesForLiteral(
            GdlLiteral literal, GdlRule rule,
            ListMultimap<SentenceForm, Ambiguity> ambiguitiesByOriginalForm,
            SentenceFormModel model) throws InterruptedException {
        ConcurrencyUtils.checkForInterruption();
        List<GdlRule> results = Lists.newArrayList(rule);
        UnusedVariableGenerator varGen = getVariableGenerator(rule);

        if (literal instanceof GdlSentence) {
            GdlSentence sentence = (GdlSentence) literal;
            SentenceForm form = model.getSentenceForm(sentence);
            for (Ambiguity ambiguity : ambiguitiesByOriginalForm.get(form)) {
                ConcurrencyUtils.checkForInterruption();
                if (ambiguity.applies(sentence)) {
                    Map<GdlVariable, GdlTerm> replacementAssignment = ambiguity.getReplacementAssignment(sentence, varGen);
                    GdlRule newRule = CommonTransforms.replaceVariables(rule, replacementAssignment);
                    results.add(newRule);
                }
            }
        } else if (literal instanceof GdlNot) {
            // Do nothing. Variables must appear in a positive literal in the
            // rule, and will be handled there.
        } else if (literal instanceof GdlOr) {
            throw new RuntimeException("ORs should have been removed");
        } else if (literal instanceof GdlDistinct) {
            // Do nothing
        }

        return results;
    }

    private abstract static class UnusedVariableGenerator {
        public GdlFunction replaceVariablesAndConstants(GdlFunction function) {
            Map<GdlVariable, GdlVariable> assignment = Maps.newHashMap();

            final Set<GdlTerm> termsToReplace = Sets.newHashSet();
            GdlVisitors.visitAll(function, new GdlVisitor() {
                @Override
                public void visitConstant(GdlConstant constant) {
                    termsToReplace.add(constant);
                }
                @Override
                public void visitVariable(GdlVariable variable) {
                    termsToReplace.add(variable);
                }
            });

            for (GdlVariable var : GdlUtils.getVariables(function)) {
                assignment.put(var, getUnusedVariable());
            }
            return (GdlFunction) CommonTransforms.replaceVariables(function, assignment);
        }

        protected abstract GdlVariable getUnusedVariable();
    }

    private static UnusedVariableGenerator getVariableGenerator(final GdlRule rule) {
        //Not thread-safe
        return new UnusedVariableGenerator() {
            private int count = 1;
            private final Set<GdlVariable> originalVarsFromRule =
                    ImmutableSet.copyOf(GdlUtils.getVariables(rule));
            @Override
            public GdlVariable getUnusedVariable() {
                GdlVariable curVar = GdlPool.getVariable("?a" + count);
                count++;
                while (originalVarsFromRule.contains(curVar)) {
                    curVar = GdlPool.getVariable("?a" + count);
                    count++;
                }
                return curVar;
            }
        };
    }

    /**
     * Removes rules with sentences with empty domains. These simply won't have
     * sentence forms in the generated sentence model, so this is fairly easy.
     * @throws InterruptedException
     */
    private static List<Gdl> cleanUpIrrelevantRules(List<Gdl> expandedRules) throws InterruptedException {
        final ImmutableSentenceFormModel model = SentenceFormModelFactory.create(expandedRules);
        return ImmutableList.copyOf(Collections2.filter(expandedRules, new Predicate<Gdl>() {
            @Override
            public boolean apply(Gdl input) {
                if (!(input instanceof GdlRule)) {
                    // If it's not a rule, leave it in
                    return true;
                }
                GdlRule rule = (GdlRule) input;
                // Used just as a boolean we can change from the inner class
                final AtomicBoolean shouldRemove = new AtomicBoolean(false);
                GdlVisitors.visitAll(rule, new GdlVisitor() {
                    @Override
                    public void visitSentence(GdlSentence sentence) {
                        SentenceForm form = model.getSentenceForm(sentence);
                        if (!model.getSentenceForms().contains(form)) {
                            shouldRemove.set(true);
                        }
                    }
                });
                return !shouldRemove.get();
            }
        }));
    }
}
