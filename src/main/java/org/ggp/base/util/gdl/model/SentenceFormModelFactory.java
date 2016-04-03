package org.ggp.base.util.gdl.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.GdlVisitor;
import org.ggp.base.util.gdl.GdlVisitors;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.transforms.GdlCleaner;
import org.ggp.base.util.gdl.transforms.VariableConstrainer;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class SentenceFormModelFactory {
    private SentenceFormModelFactory() {
    }

    /**
     * Creates a SentenceFormModel for the given game description.
     *
     * It is recommended to use the {@link GdlCleaner} on the game
     * description before constructing this model, to prevent some
     * common problems with slightly invalid game descriptions.
     *
     * It is also recommended to use the {@link VariableConstrainer}
     * on the description before using this. If the description allows
     * for function-valued variables, some aspects of the model,
     * including the dependency graph, may be incorrect.
     */
    public static ImmutableSentenceFormModel create(List<Gdl> description) throws InterruptedException {
        ImmutableList<Gdl> gameRules = ImmutableList.copyOf(description);
        ImmutableSet<SentenceForm> sentenceForms = getSentenceForms(gameRules);
        ImmutableSetMultimap<SentenceForm, GdlRule> rulesByForm = getRulesByForm(gameRules, sentenceForms);
        ImmutableSetMultimap<SentenceForm, GdlSentence> trueSentencesByForm = getTrueSentencesByForm(gameRules, sentenceForms);
        ImmutableSetMultimap<SentenceForm, SentenceForm> dependencyGraph = getDependencyGraph(sentenceForms, rulesByForm);
        ImmutableSet<SentenceForm> constantSentenceForms = getConstantSentenceForms(sentenceForms, dependencyGraph);
        ImmutableSet<SentenceForm> independentSentenceForms = getIndependentSentenceForms(sentenceForms, dependencyGraph);

        return new ImmutableSentenceFormModel(
                gameRules,
                sentenceForms,
                constantSentenceForms,
                independentSentenceForms,
                dependencyGraph,
                rulesByForm,
                trueSentencesByForm);
    }

    private static ImmutableSet<SentenceForm> getIndependentSentenceForms(
            ImmutableSet<SentenceForm> sentenceForms,
            ImmutableSetMultimap<SentenceForm, SentenceForm> dependencyGraph) {
        SetMultimap<SentenceForm, SentenceForm> augmentedGraph = augmentGraphWithLanguageRules(dependencyGraph, sentenceForms);
        ImmutableSet<SentenceForm> moveDependentSentenceForms =
                DependencyGraphs.getMatchingAndDownstream(sentenceForms, augmentedGraph,
                        SentenceForms.DOES_PRED);
        return ImmutableSet.copyOf(Sets.difference(sentenceForms, moveDependentSentenceForms));
    }

    private static ImmutableSet<SentenceForm> getConstantSentenceForms(
            ImmutableSet<SentenceForm> sentenceForms,
            ImmutableSetMultimap<SentenceForm, SentenceForm> dependencyGraph) {
        SetMultimap<SentenceForm, SentenceForm> augmentedGraph = augmentGraphWithLanguageRules(dependencyGraph, sentenceForms);
        ImmutableSet<SentenceForm> changingSentenceForms =
                DependencyGraphs.getMatchingAndDownstream(sentenceForms, augmentedGraph,
                        Predicates.or(SentenceForms.TRUE_PRED, SentenceForms.DOES_PRED));
        return ImmutableSet.copyOf(Sets.difference(sentenceForms, changingSentenceForms));
    }

    /**
     * Modifies the graph by adding dependencies corresponding to language rules
     * that apply in a looser sense: TRUE forms depend on NEXT forms and DOES
     * forms depend on LEGAL forms.
     */
    private static SetMultimap<SentenceForm, SentenceForm> augmentGraphWithLanguageRules(
            ImmutableSetMultimap<SentenceForm, SentenceForm> dependencyGraph, ImmutableSet<SentenceForm> sentenceForms) {
        SetMultimap<SentenceForm, SentenceForm> newGraph = HashMultimap.create();
        newGraph.putAll(dependencyGraph);
        for (SentenceForm form : sentenceForms) {
            if (form.getName() == GdlPool.TRUE) {
                SentenceForm nextForm = form.withName(GdlPool.NEXT);
                if (sentenceForms.contains(nextForm)) {
                    newGraph.put(form, nextForm);
                }
            } else if (form.getName() == GdlPool.DOES) {
                SentenceForm legalForm = form.withName(GdlPool.LEGAL);
                if (sentenceForms.contains(legalForm)) {
                    newGraph.put(form, legalForm);
                }
            }
        }
        return newGraph;
    }

    private static ImmutableSetMultimap<SentenceForm, SentenceForm> getDependencyGraph(
            ImmutableSet<SentenceForm> sentenceForms,
            ImmutableSetMultimap<SentenceForm, GdlRule> rulesByForm) {
        SetMultimap<SentenceForm, SentenceForm> dependencyGraph = HashMultimap.create();
        for(Entry<SentenceForm, GdlRule> entry : rulesByForm.entries()) {
            SentenceForm head = entry.getKey();
            GdlRule rule = entry.getValue();
            for(GdlLiteral bodyLiteral : rule.getBody()) {
                dependencyGraph.putAll(head, getSentenceFormsInBody(bodyLiteral, sentenceForms));
            }
        }
        return ImmutableSetMultimap.copyOf(dependencyGraph);
    }

    private static Set<SentenceForm> getSentenceFormsInBody(
            GdlLiteral bodyLiteral, final ImmutableSet<SentenceForm> sentenceForms) {
        final Set<SentenceForm> forms = new HashSet<SentenceForm>();
        GdlVisitors.visitAll(bodyLiteral, new GdlVisitor() {
            @Override
            public void visitSentence(GdlSentence sentence) {
                for (SentenceForm form : sentenceForms) {
                    if (form.matches(sentence)) {
                        forms.add(form);
                    }
                }
            }
        });
        return forms;
    }

    private static ImmutableSetMultimap<SentenceForm, GdlSentence> getTrueSentencesByForm(
            ImmutableList<Gdl> gameRules,
            ImmutableSet<SentenceForm> sentenceForms) {
        ImmutableSetMultimap.Builder<SentenceForm, GdlSentence> builder =
                ImmutableSetMultimap.builder();

        for(Gdl gdl : gameRules) {
            if(gdl instanceof GdlSentence) {
                GdlSentence sentence = (GdlSentence) gdl;
                for (SentenceForm form : sentenceForms) {
                    if(form.matches(sentence)) {
                        builder.put(form, sentence);
                        break;
                    }
                }
            }
        }

        return builder.build();
    }

    private static ImmutableSetMultimap<SentenceForm, GdlRule> getRulesByForm(
            ImmutableList<Gdl> gameRules,
            ImmutableSet<SentenceForm> sentenceForms) {
        ImmutableSetMultimap.Builder<SentenceForm, GdlRule> builder =
                ImmutableSetMultimap.builder();

        for(Gdl gdl : gameRules) {
            if(gdl instanceof GdlRule) {
                GdlRule rule = (GdlRule) gdl;
                for (SentenceForm form : sentenceForms) {
                    if(form.matches(rule.getHead())) {
                        builder.put(form, rule);
                        break;
                    }
                }
            }
        }

        return builder.build();
    }

    private static ImmutableSet<SentenceForm> getSentenceForms(
            ImmutableList<Gdl> gameRules) throws InterruptedException {
        return new SentenceFormsFinder(gameRules).findSentenceForms();
    }
}
