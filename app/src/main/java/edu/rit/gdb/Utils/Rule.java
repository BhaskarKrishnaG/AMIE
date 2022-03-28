package edu.rit.gdb.Utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Rule {

    Atom headAtom;
    Set<Atom> bodyAtoms;
    Double headCount;
    Double confPCA;
    boolean closed;

    // Setting it to null cause the initial set of rules will not have parents.
    // TODO: Is there a better option?
    Rule parent = null;

    public Rule getParent() {
        return parent;
    }

    public void setParent(Rule parent) {
        this.parent = parent;
    }

    public Double getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Double headCount) {
        this.headCount = headCount;
    }

    public Double getConfPCA() {
        return confPCA;
    }

    public void setConfPCA(Double confPCA) {
        this.confPCA = confPCA;
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

    public Rule(Atom headAtom, Set<Atom> bodyAtoms) {
        this.headAtom = headAtom;
        this.bodyAtoms = bodyAtoms;
        this.headCount = 0.0;
        this.confPCA = 0.0;
    }

    public Rule() {
        bodyAtoms = new HashSet<>();
        closed = false;
    }

    public Atom getHeadAtom() {
        return headAtom;
    }

    public void setHeadAtom(Atom headAtom) {
        this.headAtom = headAtom;
    }

    public Set<Atom> getBodyAtoms() {
        return bodyAtoms;
    }

    public void setBodyAtoms(Set<Atom> bodyAtoms) {
        this.bodyAtoms = bodyAtoms;
    }

    /**
     * This method creates a new rules which is a deep copy of the existing rule.
     *
     * @return new rule with same signature.
     */
    public Rule deepCopyRule(){
        Atom newHead = getHeadAtom().deepCopyAtom();
        Set<Atom> newBodyAtoms = new HashSet<>();
        for (Atom a: getBodyAtoms()){
            newBodyAtoms.add(a.deepCopyAtom());
        }

        return new Rule(newHead, newBodyAtoms);
    }


    /**
     * This method will return all the variables that occur less than twice within the rule.
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
    public boolean checkIfClosed(){

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

        // If none of the head variables are present in the map and they are not the same variables,
        // then it's definitely not a closed rules.
        if (!getHeadAtom().getSubject().equals(getHeadAtom().getObject())
                && (!countVariables.containsKey(getHeadAtom().getSubject()) || !countVariables.containsKey(getHeadAtom().getObject()))){
            return false;
        }

        for (AtomicInteger count: countVariables.values()){
            if( count.intValue() < 2) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return bodyAtoms.toString() + headAtom.toString();
    }
}
