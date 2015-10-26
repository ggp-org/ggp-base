package org.ggp.base.util.gdl.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.concurrency.ConcurrencyUtils;
import org.ggp.base.util.gdl.GdlUtils;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SentenceFormsFinder {
    private final ImmutableList<Gdl> description;
    private final Map<NameAndArity, List<TermModel>> sentencesModel = Maps.newHashMap();
    private boolean haveCreatedModel = false;

    public SentenceFormsFinder(ImmutableList<Gdl> description) {
        this.description = description;
    }

    public ImmutableSet<SentenceForm> findSentenceForms() throws InterruptedException {
        createModel();

        return ImmutableSet.copyOf(getSentenceFormsFromModel());
    }

    public Map<SentenceForm, SentenceFormDomain> findCartesianDomains() throws InterruptedException {
        createModel();

        return getCartesianDomainsFromModel();
    }

    private void createModel() throws InterruptedException {
        synchronized (this) {
            if (!haveCreatedModel) {
                addTrueSentencesToModel();
                applyRulesToModel();
                haveCreatedModel = true;
            }
        }
    }

    private Map<SentenceForm, SentenceFormDomain> getCartesianDomainsFromModel() throws InterruptedException {
        Map<SentenceForm, SentenceFormDomain> results = Maps.newHashMap();
        for (Entry<NameAndArity, List<TermModel>> sentenceEntry : sentencesModel.entrySet()) {
            ConcurrencyUtils.checkForInterruption();
            NameAndArity nameAndArity = sentenceEntry.getKey();
            GdlConstant name = nameAndArity.getName();
            List<TermModel> bodyModels = sentenceEntry.getValue();
            // We'll end up taking the Cartesian product of the different
            // types of terms we have available
            if (nameAndArity.getArity() == 0) {
                GdlSentence sentence = GdlPool.getProposition(name);
                SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
                results.put(form, CartesianSentenceFormDomain.create(form, ImmutableList.<Set<GdlConstant>>of()));
            } else {
                List<Set<GdlTerm>> sampleTerms = toSampleTerms(bodyModels);
                for (List<GdlTerm> terms : Sets.cartesianProduct(sampleTerms)) {
                    ConcurrencyUtils.checkForInterruption();
                    GdlRelation sentence = GdlPool.getRelation(name, terms);
                    SimpleSentenceForm form = SimpleSentenceForm.create(sentence);
                    SentenceFormDomain domain = getDomain(form, sentence);
                    results.put(form, domain);
                }
            }
        }
        return results;
    }

    private SentenceFormDomain getDomain(SentenceForm form, GdlRelation sentence) {
        List<Set<GdlConstant>> domainContents = Lists.newArrayList();
        getDomainInternal(sentence.getBody(), sentencesModel.get(new NameAndArity(sentence)), domainContents);
        return CartesianSentenceFormDomain.create(form, domainContents);
    }

    //Appends to domainContents
    private void getDomainInternal(List<GdlTerm> body, List<TermModel> bodyModel,
            List<Set<GdlConstant>> domainContents) {
        if (body.size() != bodyModel.size()) {
            throw new IllegalStateException("Should have same arity in example as in model");
        }
        for (int i = 0; i < body.size(); i++) {
            GdlTerm term = body.get(i);
            TermModel termModel = bodyModel.get(i);
            if (term instanceof GdlConstant) {
                domainContents.add(termModel.getPossibleConstants());
            } else if (term instanceof GdlFunction) {
                GdlFunction function = (GdlFunction) term;
                List<TermModel> functionBodyModel = termModel.getFunctionBodyModel(function);
                getDomainInternal(function.getBody(), functionBodyModel, domainContents);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private Set<SentenceForm> getSentenceFormsFromModel() {
        Set<SentenceForm> results = Sets.newHashSet();
        for (Entry<NameAndArity, List<TermModel>> sentenceEntry : sentencesModel.entrySet()) {
            NameAndArity nameAndArity = sentenceEntry.getKey();
            GdlConstant name = nameAndArity.getName();
            List<TermModel> bodyModels = sentenceEntry.getValue();
            // We'll end up taking the Cartesian product of the different
            // types of terms we have available
            if (nameAndArity.getArity() == 0) {
                GdlSentence sentence = GdlPool.getProposition(name);
                results.add(SimpleSentenceForm.create(sentence));
            } else {
                List<Set<GdlTerm>> sampleTerms = toSampleTerms(bodyModels);
                for (List<GdlTerm> terms : Sets.cartesianProduct(sampleTerms)) {
                    GdlSentence sentence = GdlPool.getRelation(name, terms);
                    results.add(SimpleSentenceForm.create(sentence));
                }
            }
        }
        return results;
    }

    private List<Set<GdlTerm>> toSampleTerms(List<TermModel> bodyModels) {
        List<Set<GdlTerm>> results = Lists.newArrayList();
        for (TermModel termModel : bodyModels) {
            results.add(toSampleTerms(termModel));
        }
        return results;
    }

    private Set<GdlTerm> toSampleTerms(TermModel termModel) {
        Set<GdlTerm> results = Sets.newHashSet();
        if (!termModel.getPossibleConstants().isEmpty()) {
            results.add(termModel.getPossibleConstants().iterator().next());
        }
        for (NameAndArity nameAndArity : termModel.getPossibleFunctions().keySet()) {
            List<TermModel> bodyModel = termModel.getPossibleFunctions().get(nameAndArity);
            List<Set<GdlTerm>> functionSampleTerms = toSampleTerms(bodyModel);
            Set<List<GdlTerm>> functionBodies = Sets.cartesianProduct(functionSampleTerms);
            for (List<GdlTerm> functionBody : functionBodies) {
                GdlFunction function = GdlPool.getFunction(nameAndArity.getName(), functionBody);
                results.add(function);
            }
        }
        return results;
    }

    private void applyRulesToModel() throws InterruptedException {
        //Apply injections
        boolean changeMade = true;
        while (changeMade) {
            changeMade = false;
            for (Gdl gdl : description) {
                if (gdl instanceof GdlRule) {
                    changeMade |= addRule((GdlRule) gdl);
                }
            }
            changeMade |= applyLanguageRules();
        }
    }

    private boolean applyLanguageRules() throws InterruptedException {
        boolean changesMade = false;
        changesMade |= applyInjection(new NameAndArity(GdlPool.INIT, 1), new NameAndArity(GdlPool.TRUE, 1));
        changesMade |= applyInjection(new NameAndArity(GdlPool.NEXT, 1), new NameAndArity(GdlPool.TRUE, 1));
        changesMade |= applyInjection(new NameAndArity(GdlPool.LEGAL, 2), new NameAndArity(GdlPool.DOES, 2));
        return changesMade;
    }

    private boolean applyInjection(NameAndArity oldName,
            NameAndArity newName) throws InterruptedException {
        ConcurrencyUtils.checkForInterruption();
        Preconditions.checkArgument(oldName.getArity() == newName.getArity());
        boolean changesMade = false;
        if (sentencesModel.containsKey(oldName)) {
            List<TermModel> oldModel = sentencesModel.get(oldName);
            if (!sentencesModel.containsKey(newName)) {
                changesMade = true;
                sentencesModel.put(newName, getNTermModels(newName.arity));
            }
            List<TermModel> newModel = sentencesModel.get(newName);
            if (oldModel.size() != newModel.size()) {
                throw new IllegalStateException();
            }
            for (int i = 0; i < oldModel.size(); i++) {
                ConcurrencyUtils.checkForInterruption();
                changesMade |= newModel.get(i).mergeIn(oldModel.get(i));
            }
        }
        return changesMade;
    }

    private boolean addRule(GdlRule rule) throws InterruptedException {
        // Stuff can make it into the head sentence form either as part of
        // the head of the rule as presented or due to a variable connected
        // to the positive literals in the rule. (In the latter case, it
        // should be in the intersection of the models of all such positive
        // literals.) For each slot in the body, we want to set up everything
        // that will be injected into it.
        GdlSentence headSentence = rule.getHead();

        // We need to get the possible contents of variables beforehand, to
        // deal with the case of variables being inside functions.
        Map<GdlVariable, TermModel> varsToModelsMap = getVarsToModelsMap(rule);

        return addSentenceToModel(headSentence, varsToModelsMap);
    }

    private Map<GdlVariable, TermModel> getVarsToModelsMap(GdlRule rule) {
        Set<GdlVariable> varsToUse = Sets.newHashSet(GdlUtils.getVariables(rule.getHead()));
        Map<GdlVariable, TermModel> varsToModelsMap = Maps.newHashMap();
        for (GdlVariable var : varsToUse) {
            varsToModelsMap.put(var, new TermModel());
        }

        for (GdlLiteral literal : rule.getBody()) {
            if (literal instanceof GdlRelation) {
                List<GdlTerm> literalBody = ((GdlRelation) literal).getBody();
                NameAndArity nameAndArity = new NameAndArity((GdlSentence) literal);
                if (!sentencesModel.containsKey(nameAndArity)) {
                    sentencesModel.put(nameAndArity, getNTermModels(nameAndArity.getArity()));
                }
                List<TermModel> literalModel = sentencesModel.get(nameAndArity);
                addVariablesToMap(literalBody, literalModel, varsToModelsMap);
            }
        }
        return varsToModelsMap;
    }

    private void addVariablesToMap(List<GdlTerm> body,
            List<TermModel> model,
            Map<GdlVariable, TermModel> varsToModelsMap) {
        if (body.size() != model.size()) {
            throw new IllegalArgumentException("The term model and body sizes don't match: model is " + model + ", body is: " + body);
        }
        for (int i = 0; i < body.size(); i++) {
            GdlTerm term = body.get(i);
            TermModel termModel = model.get(i);
            if (term instanceof GdlVariable) {
                GdlVariable var = (GdlVariable) term;
                if (varsToModelsMap.containsKey(var)) {
                    varsToModelsMap.get(var).mergeIn(termModel);
                }
            } else if (term instanceof GdlFunction) {
                GdlFunction function = (GdlFunction) term;
                List<TermModel> functionBodyModel = termModel.getFunctionBodyModel(function);
                if (functionBodyModel != null) {
                    addVariablesToMap(function.getBody(), functionBodyModel, varsToModelsMap);
                }
            }
        }
    }

    private void addTrueSentencesToModel() throws InterruptedException {
        for (Gdl gdl : description) {
            ConcurrencyUtils.checkForInterruption();
            if (gdl instanceof GdlSentence) {
                addSentenceToModel((GdlSentence) gdl, ImmutableMap.<GdlVariable, TermModel>of());
            }
        }
    }

    private boolean addSentenceToModel(GdlSentence sentence, Map<GdlVariable, TermModel> varsToModelsMap) throws InterruptedException {
        ConcurrencyUtils.checkForInterruption();
        boolean changesMade = false;
        NameAndArity sentenceName = new NameAndArity(sentence);
        if (!sentencesModel.containsKey(sentenceName)) {
            changesMade = true;
            sentencesModel.put(sentenceName, getNTermModels(sentence.arity()));
        }
        changesMade |= addBodyToModel(sentencesModel.get(sentenceName), sentence.getBody(), varsToModelsMap);
        return changesMade;
    }

    private static List<TermModel> getNTermModels(int arity) {
        List<TermModel> result = Lists.newArrayListWithCapacity(arity);
        for (int i = 0; i < arity; i++) {
            result.add(new TermModel());
        }
        return result;
    }

    private static boolean addBodyToModel(List<TermModel> model, List<GdlTerm> body, Map<GdlVariable, TermModel> varsToModelsMap) {
        boolean changesMade = false;
        if (model.size() != body.size()) {
            throw new IllegalArgumentException("The term model and body sizes don't match: model is " + model + ", body is: " + body);
        }
        for (int i = 0; i < model.size(); i++) {
            TermModel termModel = model.get(i);
            GdlTerm term = body.get(i);
            changesMade |= termModel.addTerm(term, varsToModelsMap);
        }
        return changesMade;
    }

    private static class TermModel {
        private final Set<GdlConstant> possibleConstants = Sets.newHashSet();
        private final Map<NameAndArity, List<TermModel>> possibleFunctions = Maps.newHashMap();

        public List<TermModel> getFunctionBodyModel(GdlFunction function) {
            return possibleFunctions.get(new NameAndArity(function));
        }

        public Set<GdlConstant> getPossibleConstants() {
            return possibleConstants;
        }

        public Map<NameAndArity, List<TermModel>> getPossibleFunctions() {
            return possibleFunctions;
        }

        public boolean mergeIn(TermModel other) {
            boolean changesMade = false;
            changesMade |= possibleConstants.addAll(other.possibleConstants);
            for (NameAndArity key : other.possibleFunctions.keySet()) {
                List<TermModel> theirFunctionBodies = other.possibleFunctions.get(key);
                if (!possibleFunctions.containsKey(key)) {
                    possibleFunctions.put(key, deepCopyOf(theirFunctionBodies));
                    changesMade = true;
                } else {
                    List<TermModel> ourFunctionBodies = possibleFunctions.get(key);
                    if (ourFunctionBodies.size() != theirFunctionBodies.size()) {
                        throw new IllegalStateException();
                    }
                    for (int i = 0; i < ourFunctionBodies.size(); i++) {
                        changesMade |= ourFunctionBodies.get(i).mergeIn(theirFunctionBodies.get(i));
                    }
                }
            }
            return changesMade;
        }

        public TermModel() {
        }

        public boolean addTerm(GdlTerm term, Map<GdlVariable, TermModel> varsToModelsMap) {
            boolean changesMade = false;
            if (term instanceof GdlConstant) {
                changesMade = possibleConstants.add((GdlConstant) term);
            } else if (term instanceof GdlFunction) {
                GdlFunction function = (GdlFunction) term;
                NameAndArity sentenceName = new NameAndArity(function);
                if (!possibleFunctions.containsKey(sentenceName)) {
                    changesMade = true;
                    possibleFunctions.put(sentenceName, getNTermModels(function.arity()));
                }
                changesMade |= addBodyToModel(possibleFunctions.get(sentenceName), function.getBody(), varsToModelsMap);
            } else if (term instanceof GdlVariable) {
                changesMade = mergeIn(varsToModelsMap.get(term));
            } else {
                throw new RuntimeException("Unrecognized term type " + term.getClass() + " for term " + term);
            }
            return changesMade;
        }

        @Override
        public String toString() {
            return "NewTermModel [possibleConstants=" + possibleConstants
                    + ", possibleFunctions=" + possibleFunctions + "]";
        }

        public static TermModel copyOf(TermModel originalTermModel) {
            TermModel termModel = new TermModel();
            termModel.mergeIn(originalTermModel);
            return termModel;
        }
    }

    private static class NameAndArity {
        private final GdlConstant name;
        private final int arity;

        public NameAndArity(GdlSentence sentence) {
            this.name = sentence.getName();
            this.arity = sentence.arity();
        }

        public NameAndArity(GdlFunction function) {
            this.name = function.getName();
            this.arity = function.arity();
        }

        public NameAndArity(GdlConstant name, int arity) {
            this.name = name;
            this.arity = arity;
        }

        public GdlConstant getName() {
            return name;
        }

        public int getArity() {
            return arity;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + arity;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            NameAndArity other = (NameAndArity) obj;
            if (arity != other.arity)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "NameAndArity [name=" + name + ", arity=" + arity + "]";
        }
    }

    public static List<TermModel> deepCopyOf(List<TermModel> original) {
        List<TermModel> copy = Lists.newArrayListWithCapacity(original.size());
        for (TermModel originalTermModel : original) {
            copy.add(TermModel.copyOf(originalTermModel));
        }
        return copy;
    }
}
