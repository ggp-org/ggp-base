package org.ggp.base.util.propnet.factory.flattener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.logging.GamerLogger;


/**
 * PropNetFlattener is an implementation of a GDL flattener using fixed-point
 * analysis of the rules. This flattener works on many small and medium-sized
 * games, but can fail on very large games.
 * 
 * To use this class:
 *      PropNetFlattener PF = new PropNetFlattener(description);
 *      List<GdlRule> flatDescription = PF.flatten();
 *      return converter.convert(flatDescription);
 *
 * @author Ethan Dreyfuss
 * @author Sam Schreiber (comments)
 */
public class PropNetFlattener {

    private List<Gdl> description;
    
    private class Assignment extends ArrayList<GdlConstant> {
        private static final long serialVersionUID = 1L;        
    }
    
    private class Assignments extends HashSet<Assignment> {
        private static final long serialVersionUID = 1L;        
    }
    
    private class Index extends HashMap<GdlConstant, Assignments> {
        private static final long serialVersionUID = 1L;        
    }
    
    private class Condition {
        public Condition(GdlTerm template)
        {           
            this.template = getConstantAndVariableList(template);
            key = findGenericForm(template);
            updateDom();
        }
        
        public void updateDom()
        {
            if(!domains.containsKey(key))
                dom = null;
            else
                dom = domains.get(key);
        }
        
        public List<GdlTerm> template;
        public Domain dom;
        GdlTerm key;
        
        @Override
        public String toString()
        {
            return template.toString();
        }
    }
    
    private class RuleReference {
        public List<GdlTerm> productionTemplate; //The template from the rule head, contains only variables and constants
        public List<Condition> conditions = new ArrayList<Condition>(); //the conditions (right hand side of the rule)
        public Gdl originalRule;
        
        public RuleReference(GdlRule originalRule)
        {
            this.originalRule = originalRule;
        }
        
        @Override
        public String toString()
        {
            return "\n\tProduction: "+(productionTemplate!=null ? productionTemplate.toString() : "null")+" conditions: "+(conditions!=null ? conditions.toString() : "null");
        }
        
        @Override
        public boolean equals(Object other)
        {
            if(!(other instanceof RuleReference))
                return false;
            RuleReference rhs = (RuleReference)other;
            return rhs.productionTemplate==this.productionTemplate && rhs.conditions.equals(this.conditions);
        }
        
        @Override
        public int hashCode()
        {
            return productionTemplate.hashCode()+conditions.hashCode();
        }
    }
    
    private class Domain {
        public Domain(GdlTerm name, GdlTerm name2) {this.name = name; this.name2 = name2;}
        
        public Assignments assignments = new Assignments();
        public List<Index> indices = new ArrayList<Index>(); 
        public Set<RuleReference> ruleRefs = new HashSet<RuleReference>();
        
        @SuppressWarnings("unused")
        public GdlTerm name, name2;
        
        @Override
        public String toString()
        {
            return "\nName: "+name+"\nvalues: "+assignments;//+"\nruleRefs: "+ruleRefs;
        }

        public void buildIndices() {
            for(Assignment assignment : assignments)
            {
                addAssignmentToIndex(assignment);
            }
            for(RuleReference ruleRef : ruleRefs)
            {
                List<Condition> newConditions = new ArrayList<Condition>();
                for(Condition c : ruleRef.conditions)
                {
                    if(c.dom == null)
                        c.updateDom();
                    
                    if(c.dom != null)
                        newConditions.add(c);
                }
                if(newConditions.size() != ruleRef.conditions.size()) //Remove reference to constant terms
                    ruleRef.conditions = newConditions;
            }
        }
        
        public void addAssignmentToIndex(Assignment assignment) {
            for(int i=0; i<assignment.size(); i++)
            {
                GdlConstant c = assignment.get(i);
                if(indices.size() <= i)
                    indices.add(new Index());                   
                Index index = indices.get(i);
                
                if(!index.containsKey(c))
                    index.put(c, new Assignments());
                Assignments val = index.get(c);
                val.add(assignment);
            }
        }
    }
        
    private GdlVariable fillerVar = GdlPool.getVariable("?#*#");
    
    HashMap<GdlTerm,Domain> domains = new HashMap<GdlTerm, Domain>();
    
    private List<RuleReference> extraRefs = new ArrayList<RuleReference>();
    
    public PropNetFlattener(List<Gdl> description)
    {
        this.description = description;
    }
    
    public List<GdlRule> flatten()
    {
        //Find universe and initial domains
        for(Gdl gdl : description)
        {
            initializeDomains(gdl);
        }
        
        for(Domain d : domains.values())
            d.buildIndices();
        
        //Compute the actual domains of everything
        updateDomains();
        
        //printDomains();
        //printDomainRefs();
                                
        return getAllInstantiations();
    }

    private List<GdlRule> getAllInstantiations() {
        List<GdlRule> rval = new ArrayList<GdlRule>();
        
        for(Gdl gdl : description)
        {
            if(gdl instanceof GdlRelation)
            {
                GdlRelation relation = (GdlRelation) gdl;
                String name = relation.getName().toString();
                if(name.equals("base"))
                    continue;
                
                rval.add(GdlPool.getRule(relation));
            }
        }
        
        for(Domain d : domains.values())
        {
            for(RuleReference r : d.ruleRefs)
            {
                Set<Map<GdlVariable,GdlConstant>> varInstantiations = findSatisfyingInstantiations(r);
                
                for(Map<GdlVariable,GdlConstant> varInstantiation : varInstantiations){
                    if(varInstantiation.containsValue(null))
                        throw new RuntimeException("Shouldn't instantiate anything to null.");
                    rval.add(getInstantiation(r.originalRule, varInstantiation));
                    if(rval.get(rval.size()-1).toString().contains("null"))
                        throw new RuntimeException("Shouldn't instantiate anything to null: "+rval.get(rval.size()-1).toString());
                }
            }
        }
        
        
        for(RuleReference ruleRef : extraRefs)
        {
            List<Condition> newConditions = new ArrayList<Condition>();
            for(Condition c : ruleRef.conditions)
            {
                if(c.dom == null)
                    c.updateDom();
                
                if(c.dom != null)
                    newConditions.add(c);
            }
            if(newConditions.size() != ruleRef.conditions.size()) //Remove reference to constant terms
                ruleRef.conditions = newConditions;
        }
        for(RuleReference r : extraRefs)
        {
            Set<Map<GdlVariable,GdlConstant>> varInstantiations = findSatisfyingInstantiations(r);
            
            for(Map<GdlVariable,GdlConstant> varInstantiation : varInstantiations){
                if(varInstantiation.containsValue(null))
                    throw new RuntimeException("Shouldn't instantiate anything to null.");
                rval.add(getInstantiation(r.originalRule, varInstantiation));

                if(rval.get(rval.size()-1).toString().contains("null"))
                    throw new RuntimeException("Shouldn't instantiate anything to null.");
            }
            
            if(varInstantiations.size() == 0)
                rval.add(getInstantiation(r.originalRule, new HashMap<GdlVariable,GdlConstant>()));
        }
        
        return rval;
    }
    
    private GdlRule getInstantiation(Gdl gdl, Map<GdlVariable, GdlConstant> varInstantiation) {
        Gdl instant = getInstantiationAux(gdl, varInstantiation);
        return (GdlRule)instant;                
    }
    
    private Gdl getInstantiationAux(Gdl gdl, Map<GdlVariable, GdlConstant> varInstantiation) {
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            
            List<GdlTerm> body = new ArrayList<GdlTerm>();
            for(int i=0; i<relation.arity(); i++)
            {
                body.add((GdlTerm)getInstantiationAux(relation.get(i), varInstantiation));
            }
            return GdlPool.getRelation(relation.getName(), body);
        }
        else if(gdl instanceof GdlRule)
        {
            GdlRule rule = (GdlRule)gdl;
            GdlSentence head = (GdlSentence)getInstantiationAux(rule.getHead(), varInstantiation);
            
            List<GdlLiteral> body = new ArrayList<GdlLiteral>();
            for(int i=0; i<rule.arity(); i++)
            {
                body.add((GdlLiteral)getInstantiationAux(rule.get(i), varInstantiation));               
            }
            return GdlPool.getRule(head, body);
        }
        else if(gdl instanceof GdlDistinct)
        {
            GdlDistinct distinct = (GdlDistinct)gdl;
            GdlTerm arg1 = (GdlTerm)getInstantiationAux(distinct.getArg1(), varInstantiation);
            GdlTerm arg2 = (GdlTerm)getInstantiationAux(distinct.getArg2(), varInstantiation);
            return GdlPool.getDistinct(arg1, arg2);
        }
        else if(gdl instanceof GdlNot)
        {
            GdlNot not = (GdlNot)gdl;   
            GdlLiteral body = (GdlLiteral)getInstantiationAux(not.getBody(), varInstantiation);
            return GdlPool.getNot(body);
        }
        else if(gdl instanceof GdlOr)
        {
            GdlOr or = (GdlOr)gdl;
            List<GdlLiteral> body = new ArrayList<GdlLiteral>();
            for(int i=0; i<or.arity(); i++)
            {
                body.add((GdlLiteral)getInstantiationAux(or.get(i), varInstantiation));
            }
            return GdlPool.getOr(body);
        }
        else if(gdl instanceof GdlProposition)
        {
            return gdl;
        }
        else if(gdl instanceof GdlConstant)
        {
            return gdl;
        }
        else if(gdl instanceof GdlFunction)
        {
            GdlFunction func = (GdlFunction)gdl;
            List<GdlTerm> body = new ArrayList<GdlTerm>();
            for(int i=0; i<func.arity(); i++)
            {               
                body.add((GdlTerm)getInstantiationAux(func.get(i), varInstantiation));
            }
            return GdlPool.getFunction(func.getName(), body);           
        }
        else if(gdl instanceof GdlVariable)
        {   
            GdlVariable variable = (GdlVariable)gdl;
            return varInstantiation.get(variable);
        }
        else
            throw new RuntimeException("Someone went and extended the GDL hierarchy without updating this code.");
    }
        
    void initializeDomains(Gdl gdl)
    {
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            String name = relation.getName().toString();            
            if(!name.equals("base"))
            {
                GdlTerm term = relation.toTerm();
                GdlTerm generified = findGenericForm(term);
                Assignment instantiation = getConstantList(term);
                if(!domains.containsKey(generified))
                    domains.put(generified, new Domain(generified, term));
                Domain dom = domains.get(generified);
                dom.assignments.add(instantiation);
            }
        }
        else if(gdl instanceof GdlRule)
        {
            GdlRule rule = (GdlRule)gdl;
            GdlSentence head = rule.getHead();
            if(head instanceof GdlRelation)
            {
                GdlRelation rel = (GdlRelation)head;
                GdlTerm term = rel.toTerm();
                
                GdlTerm generified = findGenericForm(term);
                if(!domains.containsKey(generified))
                    domains.put(generified, new Domain(generified, term));
                Domain dom = domains.get(generified);
                
                List<GdlTerm> productionTemplate = getConstantAndVariableList(term);
                
                List<List<GdlLiteral>> newRHSs = deOr(rule.getBody());
                for(List<GdlLiteral> RHS : newRHSs)
                {
                    RuleReference ruleRef = new RuleReference(GdlPool.getRule(head, RHS));
                    ruleRef.productionTemplate = productionTemplate;
                    for(GdlLiteral lit : RHS)
                    {
                        if(lit instanceof GdlSentence)
                        {
                            GdlTerm t = ((GdlSentence)lit).toTerm();
                            Condition cond = new Condition(t);
                            ruleRef.conditions.add(cond);
                        }
                    }
                    dom.ruleRefs.add(ruleRef);
                }
                
            }
            else
            {
                List<List<GdlLiteral>> newRHSs = deOr(rule.getBody());
                for(List<GdlLiteral> RHS : newRHSs)
                {
                    RuleReference ruleRef = new RuleReference(GdlPool.getRule(head, RHS));
                    for(GdlLiteral lit : RHS)
                    {
                        if(lit instanceof GdlSentence)
                        {
                            GdlTerm t = ((GdlSentence)lit).toTerm();
                            Condition cond = new Condition(t);
                            ruleRef.conditions.add(cond);
                        }
                    }
                    extraRefs.add(ruleRef);
                }
            }
        }
    }   
    
    private Assignment getConstantList(GdlTerm term) {
        Assignment rval = new Assignment();
        if(term instanceof GdlConstant)
        {
            rval.add((GdlConstant)term);
            return rval;
        }
        else if(term instanceof GdlVariable)
            throw new RuntimeException("Called getConstantList on something containing a variable.");
        
        GdlFunction func = (GdlFunction)term;
        for(GdlTerm t : func.getBody())
            rval.addAll(getConstantList(t));
        
        return rval;
    }
    
    private List<GdlTerm> getConstantAndVariableList(GdlTerm term) {
        List<GdlTerm> rval = new ArrayList<GdlTerm>();
        if(term instanceof GdlConstant)
        {
            rval.add(term);
            return rval;
        }
        else if(term instanceof GdlVariable)
        {
            rval.add(term);
            return rval;
        }
        
        GdlFunction func = (GdlFunction)term;
        for(GdlTerm t : func.getBody())
            rval.addAll(getConstantAndVariableList(t));
        
        return rval;
    }

    GdlConstant legalConst = GdlPool.getConstant("legal");
    GdlConstant trueConst = GdlPool.getConstant("true");
    GdlConstant doesConst = GdlPool.getConstant("does");
    GdlConstant nextConst = GdlPool.getConstant("next");
    GdlConstant initConst = GdlPool.getConstant("init");
    private GdlTerm findGenericForm(GdlTerm term) {
        if(term instanceof GdlConstant)
            return fillerVar;
        else if(term instanceof GdlVariable)
            return fillerVar;
        
        GdlFunction func = (GdlFunction)term;
        List<GdlTerm> newBody = new ArrayList<GdlTerm>();
        for(GdlTerm t : func.getBody())
            newBody.add(findGenericForm(t));
        GdlConstant name = func.getName();
        if(name==legalConst)
            name=doesConst;
        else if(name==nextConst)
            name=trueConst;
        else if(name==initConst)
            name=trueConst;
        return GdlPool.getFunction(name, newBody);
    }

    private List<List<GdlLiteral>> deOr(List<GdlLiteral> rhs) {
        List<List<GdlLiteral>> wrapped = new ArrayList<List<GdlLiteral>>();
        wrapped.add(rhs);
        return deOr2(wrapped);
    }

    private List<List<GdlLiteral>> deOr2(List<List<GdlLiteral>> rhsList) {
        List<List<GdlLiteral>> rval = new ArrayList<List<GdlLiteral>>();
        boolean expandedSomething = false;
        for(List<GdlLiteral> rhs : rhsList)
        {
            int i=0;
            if(!expandedSomething)
            {
                for(GdlLiteral lit : rhs)
                {
                    if(!expandedSomething)
                    {
                        List<Gdl> expandedList = expandFirstOr(lit);
                        
                        if(expandedList.size() > 1)
                        {
                            for(Gdl replacement : expandedList)
                            {                   
                                List<GdlLiteral> newRhs = new ArrayList<GdlLiteral>(rhs);
                                if(!(replacement instanceof GdlLiteral)) throw new RuntimeException("Top level return value is different type of gdl.");
                                GdlLiteral newLit = (GdlLiteral)replacement;
                                newRhs.set(i, newLit);
                                rval.add(newRhs);
                            }
                            expandedSomething = true;
                            break;                      
                        }
                    }
                    
                    i++;
                }
                if(!expandedSomething) //If I didn't find anything to expand
                    rval.add(rhs);
            }
            else
                rval.add(rhs); //If I've already expanded this function call
            
        }
        
        if(!expandedSomething)      
            return rhsList;
        else
            return deOr2(rval);
    }

    private List<Gdl> expandFirstOr(Gdl gdl) {
        List<Gdl> rval;
        List<Gdl> expandedChild;
        if(gdl instanceof GdlDistinct)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlNot)
        {
            GdlNot not = (GdlNot)gdl;
            expandedChild = expandFirstOr(not.getBody());
            rval = new ArrayList<Gdl>();
            for(Gdl g : expandedChild)
            {
                if(!(g instanceof GdlLiteral)) throw new RuntimeException("Not must have literal child.");
                GdlLiteral lit = (GdlLiteral)g;
                rval.add(GdlPool.getNot(lit));
            }
            return rval;
        }
        else if(gdl instanceof GdlOr)
        {
            GdlOr or = (GdlOr)gdl;
            rval = new ArrayList<Gdl>();
            for(int i=0; i<or.arity(); i++)
            {
                rval.add(or.get(i));                
            }
            return rval;
        }
        else if(gdl instanceof GdlProposition)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlRelation)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlRule)
        {
            throw new RuntimeException("This should be used to remove 'or's from the body of a rule, and rules can't be nested");
        }
        else if(gdl instanceof GdlConstant)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlFunction)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else if(gdl instanceof GdlVariable)
        {
            //Can safely be ignored, won't contain 'or'
            rval = new ArrayList<Gdl>();
            rval.add(gdl);
            return rval;
        }
        else
        {
            throw new RuntimeException("Uh oh, gdl hierarchy must have been extended without updating this code.");
        }
    }

    void updateDomains()
    {
        boolean changedSomething = true;
        int itrNum = 0;
        Set<Domain> lastUpdatedDomains = new HashSet<Domain>(domains.values());
        while(changedSomething)
        {
            GamerLogger.log("StateMachine", "Beginning domain finding iteration: "+itrNum);
            Set<Domain> currUpdatedDomains = new HashSet<Domain>();
            changedSomething = false;
            int rulesConsidered = 0;
            for(Domain d : domains.values())
            {
                
                for(RuleReference ruleRef : d.ruleRefs)
                {
                    boolean containsUpdatedDomain = false;
                    for(Condition c : ruleRef.conditions)
                        if(lastUpdatedDomains.contains(c.dom))
                        {
                            containsUpdatedDomain = true;
                            break;
                        }
                    if(!containsUpdatedDomain)
                        continue;
                    
                    rulesConsidered++;
                    
                    Set<Map<GdlVariable,GdlConstant>> instantiations = findSatisfyingInstantiations(ruleRef);
                    for(Map<GdlVariable,GdlConstant> instantiation : instantiations)
                    {
                        Assignment a = new Assignment();
                        for(GdlTerm t : ruleRef.productionTemplate)
                        {
                            if(t instanceof GdlConstant)
                                a.add((GdlConstant)t);
                            else
                            {
                                GdlVariable var = (GdlVariable)t;
                                a.add(instantiation.get(var));
                            }                               
                        }
                        if(!d.assignments.contains(a))
                        {
                            currUpdatedDomains.add(d);
                            d.assignments.add(a);
                            changedSomething = true;
                            d.addAssignmentToIndex(a);
                        }
                    }
                    if(instantiations.size() == 0)
                    { //There might just be no variables in the rule
                        Assignment a = new Assignment();
                        findSatisfyingInstantiations(ruleRef); //just for debugging
                        boolean isVar = false;
                        for(GdlTerm t : ruleRef.productionTemplate)
                        {
                            if(t instanceof GdlConstant)
                                a.add((GdlConstant)t);
                            else
                            {
                                //There's a variable and we didn't find an instantiation
                                isVar = true;
                                break;
                            }                               
                        }
                        if(!isVar && !d.assignments.contains(a))
                        {
                            currUpdatedDomains.add(d);
                            d.assignments.add(a);
                            changedSomething = true;
                            d.addAssignmentToIndex(a);
                        }
                    }
                }
            }
            itrNum++;
            lastUpdatedDomains = currUpdatedDomains;
            GamerLogger.log("StateMachine", "\tDone with iteration.  Considered "+rulesConsidered+" rules.");
        }
    }

    
    private Set<Map<GdlVariable, GdlConstant>> findSatisfyingInstantiations(RuleReference ruleRef) 
    {
        Map<GdlVariable, GdlConstant> emptyInstantiation = new HashMap<GdlVariable, GdlConstant>();
                
        return findSatisfyingInstantiations(ruleRef.conditions, 0, emptyInstantiation);
    }

    //Coolest method in this whole thing, does the real work of the JOIN stuff
    private Set<Map<GdlVariable, GdlConstant>> findSatisfyingInstantiations(
            List<Condition> conditions, int idx,
            Map<GdlVariable, GdlConstant> instantiation) 
    {
        Set<Map<GdlVariable, GdlConstant>> rval = new HashSet<Map<GdlVariable,GdlConstant>>();
        if(idx==conditions.size())
        {
            rval.add(instantiation);
            return rval;
        }           
        
        Condition cond = conditions.get(idx);
        Domain dom = cond.dom;
        Assignments assignments = null;
        for(int i=0; i<cond.template.size(); i++)
        {
            GdlTerm t = cond.template.get(i);
            GdlConstant c = null;
            if(t instanceof GdlVariable)
            {
                GdlVariable v = (GdlVariable)t;
                if(instantiation.containsKey(v))
                    c = instantiation.get(v);
            }
            else if(t instanceof GdlConstant)
                c = (GdlConstant)t;
                
            if(c != null)
            {
                if(assignments == null)
                {
                    assignments = new Assignments();
                    if(dom.indices.size() > i)  //if this doesn't hold it is because there are no assignments and the indices haven't been set up yet
                    {
                        Index index = dom.indices.get(i);
                        if(index.containsKey(c))  //Might be no assignment which satisfies this condition
                            assignments.addAll(index.get(c));
                    }
                }
                else
                {
                    if(dom.indices.size()>i)
                    {
                        Index index = dom.indices.get(i);
                        if(index.containsKey(c))  //Might be no assignment which satisfies this condition
                            assignments.retainAll(index.get(c));
                    }
                    else  //This is when we've tried to find an assignment for a form that doesn't have any assignments yet.  Pretend it returned an empty set
                        assignments.clear();
                }
            }
        }
        if(assignments == null) //case where there are no constants to be consistent with
        {
            assignments = dom.assignments;
        }
                
        for(Assignment a : assignments)
        {
            Map<GdlVariable, GdlConstant> newInstantiation = new HashMap<GdlVariable, GdlConstant>(instantiation);
            for(int i=0; i<a.size(); i++)
            {
                GdlTerm t = cond.template.get(i);
                if(t instanceof GdlVariable)
                {
                    GdlVariable var = (GdlVariable)t;
                    if(!instantiation.containsKey(var))
                        newInstantiation.put(var, a.get(i));
                }
            }
            rval.addAll(findSatisfyingInstantiations(conditions, idx+1, newInstantiation));
        }
        
        return rval;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        List<Gdl> description = GameRepository.getDefaultRepository().getGame("conn4").getRules();
        
        PropNetFlattener flattener = new PropNetFlattener(description);
        List<GdlRule> flattened = flattener.flatten();
        System.out.println("Flattened description for connect four contains: \n" + flattened.size() + "\n\n");
        
        List<String> strings = new ArrayList<String>();
        for(GdlRule rule : flattened)
            strings.add(rule.toString());
        Collections.sort(strings);
        
        for(String s : strings)
            System.out.println(s);        
    }
}