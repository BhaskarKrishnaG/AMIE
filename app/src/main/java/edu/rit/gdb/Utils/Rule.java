package edu.rit.gdb.Utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Rule implements Comparable<Rule>{

    Atom headAtom;
    List<Atom> bodyAtoms;
    Double headCoverage;
    Double confPCA;
    int support;
    int functionalVariable;

    // Setting it to null cause the initial set of rules will not have parents.
    // TODO: Is there a better option?
    Set<Rule> parents = new HashSet<>();

    public Set<Rule> getParent() {
        return parents;
    }

    public void setParent(Rule parent) {
        parents.add(parent);
    }

    public Double getHeadCoverage() {
        return headCoverage;
    }

    public void setHeadCoverage(Double headCoverage) {
        this.headCoverage = headCoverage;
    }

    public int getSupport() {
        return support;
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public Double getConfPCA() {
        return confPCA;
    }

    public void setConfPCA(Double confPCA) {
        this.confPCA = confPCA;
    }

    public int getFunctionalVariable() {
        return functionalVariable;
    }

    public void setFunctionalVariable(int functionalVariable) {
        this.functionalVariable = functionalVariable;
    }

    /**
     * This method returns the size of the rule.
     *
     * @return length.
     */
    public int getLength() {
        return bodyAtoms.size() + 1;
    }


    // THINK: Do we want a variable that indicates the closeness? Or as a property?

    public Rule(Atom headAtom, List<Atom> bodyAtoms) {
        this.headAtom = headAtom;
        this.bodyAtoms = bodyAtoms;
        this.headCoverage = 0.0;
        this.confPCA = 0.0;
        this.functionalVariable = -1;
    }

    public Rule(Atom headAtom, List<Atom> bodyAtoms, int functionalVariable) {
        this.headAtom = headAtom;
        this.bodyAtoms = bodyAtoms;
        this.headCoverage = 0.0;
        this.confPCA = 0.0;
        this.functionalVariable = functionalVariable;
    }

    public Rule() {
        bodyAtoms = new ArrayList<>();
        this.headCoverage = 0.0;
        this.confPCA = 0.0;
        this.functionalVariable = -1;
    }

    public Atom getHeadAtom() {
        return headAtom;
    }

    public void setHeadAtom(Atom headAtom) {
        this.headAtom = headAtom;
    }


    public List<Atom> getBodyAtoms() {
        return bodyAtoms;
    }

    public void setBodyAtoms(List<Atom> bodyAtoms) {
        this.bodyAtoms = bodyAtoms;
    }


    /**
     * This method creates a new rules which is a deep copy of the existing rule.
     *
     * @return new rule with same signature.
     */
    public Rule deepCopyRule(){
        Atom newHead = getHeadAtom().deepCopyAtom();
        List<Atom> newBodyAtoms = new ArrayList<>();
        for (Atom a: getBodyAtoms()){
            newBodyAtoms.add(a.deepCopyAtom());
        }

        return new Rule(newHead, newBodyAtoms, functionalVariable);
    }


    /**
     * This method will return all the variables that occur ONCE within the rule.
     * If rule is NOT closed then we only need to join with non-closed variables so this method will
     * help up get those variables
     *
     * @return Set of variables.
     */
    public Set<Long> getOpenVariables(){
        List<Long> allVariablesList = new ArrayList<>();
        Set<Long> allVariablesSet;
        allVariablesList.add(headAtom.getSubject());
        allVariablesList.add(headAtom.getObject());

        for (Atom a: bodyAtoms){
            allVariablesList.add(a.getSubject());
            allVariablesList.add(a.getObject());
        }

        allVariablesSet = allVariablesList.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()))
                .entrySet()
                .stream()
                .filter(x -> x.getValue() == 1L)
                .map(Map.Entry::getKey).collect(Collectors.toSet());

        return allVariablesSet;
    }


    /**
     * This method returns all the variables that occur in the rule.
     *
     * @return Set of variables.
     */
    public Set<Long> getALLVariables(){
        Set<Long> allVariablesSet = new HashSet<>();
        allVariablesSet.add(headAtom.getSubject());
        allVariablesSet.add(headAtom.getObject());

        for (Atom a: bodyAtoms){
            allVariablesSet.add(a.getSubject());
            allVariablesSet.add(a.getObject());
        }
        return allVariablesSet;
    }


    /**
     * This method will check of it's closed, meaning is there two occurrences of every subject/object.
     *
     * @return true - if it's close.
     *         false - otherwise.
     */
    public boolean isClosed(){

        HashMap<Long, AtomicInteger> countVariables = new HashMap<>();
        for (Atom eachAtom: getBodyAtoms()){
            if (!countVariables.containsKey(eachAtom.getSubject())){
                countVariables.put(eachAtom.getSubject(), new AtomicInteger(1));
            }
            else {
                countVariables.get(eachAtom.getSubject()).incrementAndGet();
            }
            if (!countVariables.containsKey(eachAtom.getObject())){
                countVariables.put(eachAtom.getObject(), new AtomicInteger(1));
            }
            else {
                countVariables.get(eachAtom.getObject()).incrementAndGet();
            }
        }

        // Both the head atom variables must be present in the map.
        long s = getHeadAtom().getSubject();
        long o = getHeadAtom().getObject();
        if (!countVariables.containsKey(s)
                || !countVariables.containsKey(o)){
            return false;
        }

        for (Long var: countVariables.keySet()){
            if( countVariables.get(var).intValue() < 2 && var != s && var != o) {
                return false;
            }
        }
        return true;
    }


    /**
     * This method checks if an Atom is present within the rule.
     *
     * @param a Atom.
     * @return True - If present.
     *         False - Otherwise.
     */
    public boolean containsAtom(Atom a){
        for (Atom atom: bodyAtoms){
            if (atom.equals(a)) {
                return true;
            }
        }

        return headAtom.equals(a);
    }


    /**
     * This method checks if a given variable part of any Atom that's in the body of the rule.
     *
     * @return - True if part.
     *         - False otherwise
     */
    public boolean isPartOfBody(Long var){

        for (Atom atom: getBodyAtoms()){
            if (var.equals(atom.getSubject()) || var.equals(atom.getObject())){
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return bodyAtoms.toString() + " =>"+ headAtom.toString();
    }

    @Override
    public int compareTo(Rule o) {
        return getConfPCA().compareTo(o.getConfPCA());
    }
}