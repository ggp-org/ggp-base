package org.ggp.base.util.propnet.factory.annotater;

import java.math.BigInteger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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


/**
 * Annotater generates ( base ?x ) annotations that explicitly specify the
 * domains of the propositions in a game. This only works on some relatively
 * simple games, unfortunately.
 * 
 * @author Ethan Dreyfuss
 */
public class PropNetAnnotater {
    private List<Gdl> description;
//  private List<GdlRelation> relations = new ArrayList<GdlRelation>();
    private Set<GdlRelation> baseRelations = new HashSet<GdlRelation>();
    private Set<GdlConstant> universe = new HashSet<GdlConstant>();
    private GdlFunction universalDom = null;
    
    private class Domain {
        public Domain(Location loc) {this.loc = loc;}
        public Set<GdlConstant> values = new HashSet<GdlConstant>();
        public Set<Set<Domain>> functionRefs = new HashSet<Set<Domain>>();
        public Location loc;
        
        @Override
        public String toString()
        {
            return "Name: "+loc.name+" index: "+loc.idx+"\nvalues: "+values+"\nfunctionRefs: "+functionRefs;
        }
    }
    
    private class Location {
        public GdlConstant name;
        public Integer idx;
        
        public Location()
        {}
        
        @SuppressWarnings("unused")
        public Location(Location other)
        {
            name = other.name;
            idx = other.idx;
        }
        
        @Override
        public boolean equals(Object other)
        {
            if(!(other instanceof Location))
                return false;
            Location rhs = (Location)other;
            return idx==rhs.idx && name.toString().equals(rhs.name.toString());
        }
        
        @Override
        public int hashCode()
        {
            byte[] bytes = name.toString().getBytes();
            BigInteger bigInt = new BigInteger(bytes);
            int val = bigInt.bitCount()+bigInt.intValue();
            return val+idx;
        }
        
        @Override
        public String toString()
        {
            return name.toString()+"("+idx+")";
        }
    }
    
    HashMap<Location,Domain> domains = new HashMap<Location, Domain>();
    
    public PropNetAnnotater(List<Gdl> description)
    {
        this.description = description;
    }
    
    public List<Gdl> getAnnotations()
    {
        //Find universe and initial domains
        for(Gdl gdl : description)
        {
            processGdl(gdl, null);
            processDomain(gdl);
        }
        
        //Compute the actual domains of everything
        updateDomains();
        
        //printDomains();
        //printDomainRefs();
        
        //Compute function corresponding to universal set for insertion in baseprops
        List<GdlTerm> body = new ArrayList<GdlTerm>();
        body.addAll(universe);
        universalDom = GdlPool.getFunction(GdlPool.getConstant("thing"), body);
        
        //Find next/init things and use them to instantiate base props
        for(Gdl gdl : description)
        {
            findAndInstantiateBaseProps(gdl);
        }
        
        baseRelations = mergeBaseRelations(baseRelations);
        
        //Return the results
        List<Gdl> rval = new ArrayList<Gdl>();
        rval.addAll(baseRelations);
        return rval;
    }
    
    private Set<GdlRelation> mergeBaseRelations(Set<GdlRelation> rels) {
        HashMap<GdlConstant,List<Set<GdlConstant>>> merges = new HashMap<GdlConstant, List<Set<GdlConstant>>>();
        for(GdlRelation rel : rels)
        {
            GdlConstant name = (GdlConstant)rel.get(0);
            if(!merges.containsKey(name))
                merges.put(name, new ArrayList<Set<GdlConstant>>());
            List<Set<GdlConstant>> merge = merges.get(name);
            addRelToMerge(rel, merge);
        }       
        
        Set<GdlRelation> rval = new HashSet<GdlRelation>();
        
        GdlConstant valConst = GdlPool.getConstant("val");
        for(GdlConstant c : merges.keySet())
        {
            List<Set<GdlConstant>> merge = merges.get(c);
            List<GdlTerm> body = new ArrayList<GdlTerm>();
            body.add(c);
            for(Set<GdlConstant> mergeSet : merge)
            {
                List<GdlTerm> ms2 = new ArrayList<GdlTerm>(mergeSet);
                Collections.sort(ms2, new SortTerms());
                body.add(GdlPool.getFunction(valConst, ms2));
            }
            GdlRelation toAdd = GdlPool.getRelation(baseConstant, body);
            rval.add(toAdd);
        }
        
        return rval;
    }
    
    private class SortTerms implements Comparator<GdlTerm>
    {
        public int compare(GdlTerm arg0, GdlTerm arg1) {
            GdlConstant a1 = (GdlConstant)arg0;
            GdlConstant a2 = (GdlConstant)arg1;
            String s1 = a1.toString();
            String s2 = a2.toString();
            
            int num1 = -1;
            int num2 = -1;
            
            try {num1 = Integer.parseInt(s1);} catch(Exception ex) {}
            try {num2 = Integer.parseInt(s2);} catch(Exception ex) {}
            
            if(num1 == -1 && num2 == -1)
                return Collator.getInstance().compare(s1, s2);
            
            if(num1 == -1)
                return 1;
            
            if(num2 == -1)
                return -1;
            
            return num1-num2;
        }       
    }

    private void addRelToMerge(GdlRelation rel, List<Set<GdlConstant>> merge) {
        for(int i=1; i<rel.arity(); i++)
        {   
            GdlTerm t = rel.get(i);
            if(!(t instanceof GdlFunction))
                throw new RuntimeException("Incorrectly constructed base props");
            
            if(merge.size()<i)
                merge.add(new HashSet<GdlConstant>());
                        
            GdlFunction f = (GdlFunction)t;
            Set<GdlConstant> dom = merge.get(i-1);
            for(GdlTerm t2 : f.getBody())
            {
                if(!(t2 instanceof GdlConstant))
                    throw new RuntimeException("Incorrectly constructed base props: something other than a constant");
                dom.add((GdlConstant)t2);
            }
        }
    }

    void printDomains()
    {
        System.out.println("Domains: ");
        for(Location loc : domains.keySet())
        {
            Domain d = domains.get(loc);
            System.out.println("\t"+loc);
            for(GdlConstant val : d.values)
                System.out.println("\t\t"+val);
        }
    }
    
    void printDomainRefs()
    {
        System.out.println("Domains refs: ");
        for(Location loc : domains.keySet())
        {
            Domain d = domains.get(loc);
            System.out.println("\t"+loc);
            for(Set<Domain> domSet : d.functionRefs)
            {
                System.out.println("\t\t|");
                for(Domain d2 : domSet)
                    if(d2!=null)
                        System.out.println("\t\t+"+d2.loc);
            }
        }
    }
    
    void processGdl(Gdl gdl, GdlConstant parent)
    {
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            String name = relation.getName().toString();
            if(!name.equals("base"))
            {
                for(Gdl gdl2 : relation.getBody())
                    processGdl(gdl2, relation.getName());
            }
        }
        else if(gdl instanceof GdlRule)
        {
            GdlRule rule = (GdlRule) gdl;
            for(Gdl gdl2 : rule.getBody())
                processGdl(gdl2, null);
        }
        else if(gdl instanceof GdlConstant)
        {
            universe.add((GdlConstant)gdl);
        }
        else if(gdl instanceof GdlFunction)
        {
            GdlFunction func = (GdlFunction)gdl;
            for(Gdl gdl2 : func.getBody())
                processGdl(gdl2, func.getName());               
        }
        else if(gdl instanceof GdlDistinct)
        {
            GdlDistinct distinct = (GdlDistinct)gdl;
            processGdl(distinct.getArg1(), null);
            processGdl(distinct.getArg2(), null);
        }
        else if(gdl instanceof GdlNot)
        {
            GdlNot not = (GdlNot)gdl;
            processGdl(not.getBody(), null);
        }
        else if(gdl instanceof GdlOr)
        {
            GdlOr or = (GdlOr)gdl;
            for(int i=0; i<or.arity(); i++)
                processGdl(or.get(i), null);
        }
        else if(gdl instanceof GdlProposition)
        {
            //IGNORE
        }
        else if(gdl instanceof GdlVariable)
        {
            //IGNORE
        }
    }
    
    private GdlConstant baseConstant = GdlPool.getConstant("base");
    
    private void findAndInstantiateBaseProps(Gdl gdl)
    {
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            String name = relation.getName().toString();
            if(name.equals("init"))
            {
                if(relation.arity()!=1)
                    throw new RuntimeException("Can't init more than one thing as far as I know.");
                GdlTerm template = relation.get(0);
                if(template instanceof GdlConstant)
                {
                    List<GdlTerm> body = new ArrayList<GdlTerm>();
                    body.add(template);
                    GdlRelation toAdd = GdlPool.getRelation(baseConstant, body);
                    baseRelations.add(toAdd);
                    System.err.println("Weird init of constant");
                }               
                else if(template instanceof GdlVariable)
                {
                    System.err.println("Weird init of constant");
                    List<GdlTerm> body = new ArrayList<GdlTerm>();
                    body.add(universalDom);
                    GdlRelation toAdd = GdlPool.getRelation(baseConstant, body);
                    baseRelations.add(toAdd);
                    System.err.println("Weird init of variable");
                }
                else if(template instanceof GdlFunction)
                {
                    GdlFunction func = (GdlFunction)template;
                    instantiateBaseProps(func.toSentence());
                }
            }
        }
        else if(gdl instanceof GdlRule)
        {
            GdlRule rule = (GdlRule) gdl;
            String name = rule.getHead().getName().toString();
            if(name.equals("next"))
            {
                GdlSentence head = rule.getHead();
                if(head.arity()!=1)
                    throw new RuntimeException("Can't next more than one thing as far as I know.");
                if(head.get(0) instanceof GdlVariable)
                {   //weird case where you have rule like (next ?q)
                    Location l = new Location();
                    l.idx = 0;
                    l.name = head.getName();
                    Domain dom = domains.get(l);
                    for(GdlConstant c : dom.values)
                    {
                        List<GdlTerm> body = new ArrayList<GdlTerm>();
                        body.add(c);
                        baseRelations.add(GdlPool.getRelation(baseConstant, body));
                    }
                }
                else
                    instantiateBasePropsWithRHS(head.get(0).toSentence(), rule.getBody());
            }
        }
    }
    
    private void instantiateBaseProps(GdlSentence template)
    {
        List<GdlTerm> body = new ArrayList<GdlTerm>();
        body.add(template.getName());
        for(int i=0; i<template.arity(); i++)
        {
            GdlTerm arg = template.get(i);
            if(arg instanceof GdlConstant)
            {
                List<GdlTerm> domBody = new ArrayList<GdlTerm>();
                domBody.add(arg);
                GdlFunction dom = GdlPool.getFunction(GdlPool.getConstant("val"), domBody);
                body.add(dom);
            }               
            else if(arg instanceof GdlVariable)
            {
                List<GdlTerm> domBody = new ArrayList<GdlTerm>();
                Location loc = new Location();
                loc.idx = i;
                loc.name = template.getName();
                Domain varDom = domains.get(loc);
                if(varDom == null)
                    throw new RuntimeException("Unexpected domain: "+loc+" encountered.");
                domBody.addAll(varDom.values);
                GdlFunction dom = GdlPool.getFunction(GdlPool.getConstant("val"), domBody);
                body.add(dom);
            }
            else if(arg instanceof GdlFunction)
            {
                throw new RuntimeException("Don't know how to deal with functions within next/init.");
            }
        }
        GdlRelation toAdd = GdlPool.getRelation(baseConstant, body);
        baseRelations.add(toAdd);
    }
    
    void processDomain(Gdl gdl)
    {
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            String name = relation.getName().toString();
            if(!name.equals("base"))
            {
                addDomain(relation);
            }
        }
        else if(gdl instanceof GdlRule)
        {
            GdlRule rule = (GdlRule)gdl;
            GdlSentence head = rule.getHead();
            if(head instanceof GdlRelation)
            {
                GdlRelation rel = (GdlRelation)head;
                
                int i=0;
                for(GdlTerm term : rel.getBody())
                {
                    addDomain2(term, rel.getName(), i, rule.getBody());                 
                    i++;
                }
            }
            else if(head instanceof GdlProposition)
            {
//              GdlProposition prop = (GdlProposition)head;
                //addDomain2(prop.toTerm(), prop.getName(), 0, rule.getBody());
            }
            else
                throw new RuntimeException("Don't know how to deal with this.");
        }
    }
    
    
    
    void addDomain2(GdlTerm term, GdlConstant name, int idx, List<GdlLiteral> RHS)
    {
        Location loc = new Location();
        loc.name = name;
        loc.idx = idx;
        if(!domains.containsKey(loc))
            domains.put(loc, new Domain(loc));
        Domain dom = domains.get(loc);
        if(term instanceof GdlConstant)
        {
            GdlConstant constant = (GdlConstant)term;
            dom.values.add(constant);
        }
        else if(term instanceof GdlFunction)
        {
            GdlFunction func = (GdlFunction)term;
            int i=0;
            for(GdlTerm t2 : func.getBody())
            {
                addDomain2(t2, func.getName(), i, RHS);
                i++;
            }
        }
        else if(term instanceof GdlVariable)
        {
            GdlVariable var = (GdlVariable)term;
            Set<Domain> occuranceList = findAllInstancesOf(var, RHS);
            dom.functionRefs.add(occuranceList);
        }
    }

    private Set<Domain> findAllInstancesOf(GdlVariable var, List<GdlLiteral> RHS) {
        Set<Domain> rval = new HashSet<Domain>();
        
        for(GdlLiteral literal : RHS)
        {
            rval.addAll(findAllInstancesOf(var,literal));
        }
        
        return rval;
    }

    private Set<Domain> findAllInstancesOf(GdlVariable var, GdlLiteral literal) {
        return findAllInstancesOf(var,literal,null);
    }
    
    private Set<Domain> findAllInstancesOf(GdlVariable var, Gdl gdl, Location loc) {
        if(!domains.containsKey(loc))
            domains.put(loc, new Domain(loc));
        
        Set<Domain> rval = new HashSet<Domain>();
        
        if(gdl instanceof GdlRelation)
        {
            GdlRelation relation = (GdlRelation) gdl;
            for(int i=0; i<relation.arity(); i++)
            {
                Location parent = new Location();
                parent.name = relation.getName();
                parent.idx = i;
                rval.addAll(findAllInstancesOf(var, relation.get(i), parent));
            }
        }
        else if(gdl instanceof GdlDistinct)
        {
//          GdlDistinct distinct = (GdlDistinct)gdl;
            //Negative context, ignore it for now
        }
        else if(gdl instanceof GdlNot)
        {
//          GdlNot not = (GdlNot)gdl;
            //Negative context, ignore it for now
        }
        else if(gdl instanceof GdlOr) //TODO: check that this is right, I think it may not be
        {
            GdlOr or = (GdlOr)gdl;
            for(int i=0; i<or.arity(); i++)
                rval.addAll(findAllInstancesOf(var, or.get(i), null));
        }
        else if(gdl instanceof GdlProposition)
        {
//          GdlProposition prop = (GdlProposition)gdl;
            //I think these can safely be ignored, they have no body
        }
        else if(gdl instanceof GdlConstant)
        {
//          GdlConstant constant = (GdlConstant)gdl;
            //Just a constant
        }
        else if(gdl instanceof GdlFunction)
        {
            GdlFunction func = (GdlFunction)gdl;
            for(int i=0; i<func.arity(); i++)
            {
                Location parent = new Location();
                parent.name = func.getName();
                parent.idx = i;
                rval.addAll(findAllInstancesOf(var, func.get(i), parent));
            }
        }
        else if(gdl instanceof GdlVariable)
        {   //This is the interesting one
            GdlVariable variable = (GdlVariable)gdl;
            if(variable == var)
            {   //Found what we're looking for (base case of recursion)
                if(loc==null)
                    throw new RuntimeException("Parent missing for a variable.");
                rval.add(domains.get(loc));
            }
        }
        else if(gdl instanceof GdlRule)
        {
            throw new RuntimeException("Shouldn't nest rules.");
        }
        
        return rval;
    }

    void addDomain(GdlRelation relation)
    {
        int i = 0;
        for(GdlTerm term : relation.getBody())
        {
            Location loc = new Location();
            loc.idx = i;
            loc.name = relation.getName();
            addDomain(term, loc);
            i++;
        }
    }
    
    void addDomain(GdlTerm term, Location loc)
    {
        if(!domains.containsKey(loc))
            domains.put(loc, new Domain(loc));
        Domain doms = domains.get(loc);
        if(term instanceof GdlConstant)
        {
            doms.values.add((GdlConstant)term);
        }
        else if(term instanceof GdlFunction)
        {
            GdlFunction func = (GdlFunction)term;
            int j=0;
            for(GdlTerm newTerm : func.getBody())
            {
                Location loc2 = new Location();
                loc2.idx = j;
                loc2.name = func.getName();
                addDomain(newTerm, loc2);
                j++;
            }
        }
        else if(term instanceof GdlVariable)
        {
            throw new RuntimeException("Uh oh, unbound variable which I don't know how to deal with.");
        }
    }
    
    void updateDomains()
    {
        boolean changedSomething = true;
        while(changedSomething)
        {
            changedSomething = false;
            for(Domain d : domains.values())
            {
                int before = d.values.size();
                
                for(Set<Domain> intSet : d.functionRefs)
                {
                    Set<GdlConstant> domain = null;
                    for(Domain d2 : intSet)
                    {
                        if(d2 != null)
                        {
                            if(domain == null)
                                domain = new HashSet<GdlConstant>(d2.values);
                            else
                                domain.retainAll(d2.values);
                        }
                    }
                    if(domain!=null)                        
                        d.values.addAll(domain);
                }
                
                if(d.loc != null)
                {
                    String name = d.loc.name.toString();
                    if(name.equals("does"))
                    {
                        Location newLoc = new Location();
                        newLoc.name = GdlPool.getConstant("legal");
                        newLoc.idx = d.loc.idx;
                        Domain otherDom = domains.get(newLoc);
                        if(otherDom == null)
                            throw new RuntimeException("Uh oh, missed a legal");
                        d.values.addAll(otherDom.values);
                    }
                    else if(name.equals("true"))
                    {
                        Location newLoc = new Location();
                        newLoc.name = GdlPool.getConstant("next");
                        newLoc.idx = d.loc.idx;
                        Domain otherDom = domains.get(newLoc);
                        if(otherDom == null)
                            throw new RuntimeException("Uh oh, missed a next");
                        d.values.addAll(otherDom.values);
                    }
                }
                
                if(d.values.size()!=before)
                    changedSomething = true;
            }
        }
    }
    
    private void instantiateBasePropsWithRHS(GdlSentence template, List<GdlLiteral> RHS)
    {
        instantiateBaseProps(template);
    }
    
    public List<Gdl> getAugmentedDescription()
    {
        List<Gdl> rval = new ArrayList<Gdl>();
        for(Gdl gdl : description)
        {
            boolean notBase = true;
            if(gdl instanceof GdlRelation)
            {               
                GdlRelation rel = (GdlRelation)gdl;
                if(rel.getName().toString().equals("base"))
                    notBase = false;
            }
            if(notBase)
                rval.add(gdl);
        }
        rval.addAll(getAnnotations());
        return rval;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        List<Gdl> description = GameRepository.getDefaultRepository().getGame("conn4").getRules();;
        
        PropNetAnnotater aa = new PropNetAnnotater(description);
        System.out.println("Annotations for connect four are: \n"+aa.getAnnotations());
    }
}