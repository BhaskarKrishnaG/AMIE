package edu.rit.gdb.Utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Rule {

    Atom headAtom;
    Set<Atom> bodyAtoms;
    Double headCoverage;
    Double confPCA;
    int support;
    int joiningAtom;

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
        this.headCoverage = 0.0;
        this.confPCA = 0.0;
        this.joiningAtom = -1;
    }

    public Rule(Atom headAtom, Set<Atom> bodyAtoms, int joiningAtom) {
        this.headAtom = headAtom;
        this.bodyAtoms = bodyAtoms;
        this.headCoverage = 0.0;
        this.confPCA = 0.0;
        this.joiningAtom = joiningAtom;
    }

    public Rule() {
        bodyAtoms = new HashSet<>();
    }

    public Atom getHeadAtom() {
        return headAtom;
    }

    public void setHeadAtom(Atom headAtom) {
        this.headAtom = headAtom;
    }

    public int getJoiningAtom() {
        return joiningAtom;
    }

    public void setJoiningAtom(int joiningAtom) {
        this.joiningAtom = joiningAtom;
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

        return new Rule(newHead, newBodyAtoms, getJoiningAtom());
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


    /**
     * This method will evaluate if two bodies are same(Similar).
     * Since we are trying out combination of things while generating rules it's possible that we have arrived
     * at the same rule while taking multiple paths. They might look different but be the same.
     * Example: p1(x, y) ^ p2(y, z) => p3(x, z) and
     *          p2(z, x) ^ p1(y, z) => p3(y, x) are the semantically same! but with different ordering of things
     *          We want to be able to know this semantic equality.
     *
     * @param a body1
     * @param b body2
     * @return true - if semantically same.
     *         false - otherwise.
     */
    public boolean areTwoBodiesSame(Set<Atom> a, Set<Atom> b){

        HashMap<Long, Integer> aPredicates = new HashMap<>();
        HashMap<Long, Integer> bPredicates = new HashMap<>();
        if (a.size() != b.size()) {
            return false;
        }

        // THINK: If I use AtomicInteger then we can use IncrementAndGet method to make things simpler,
        // THINK: but this makes things complicated to compare two hashmaps as the AtomicInteger class does not
        // THINK: implement the equals() and hashCode() methods from the Object class.
        else {
            for (Atom atom: a){
                Long p = atom.getPredicateId();
                if(aPredicates.containsKey(p)){
                    int currentVal = aPredicates.get(p);
                    aPredicates.put(p, currentVal + 1);
                }
                else {
                    aPredicates.put(p, 1);
                }
            }
            for (Atom atom: b){
                Long p = atom.getPredicateId();
                if(bPredicates.containsKey(p)){
                    int currentVal = bPredicates.get(p);
                    bPredicates.put(p, currentVal + 1);
                }
                else {
                    bPredicates.put(atom.getPredicateId(), 1);
                }
            }

            // If the predicates are different or of different numbers then the rules are different.
            return aPredicates.size() == bPredicates.size() && aPredicates.equals(bPredicates);
        }

        // TODO: ? SubGraphMatching
        // With the way I have designed the initial facts we only need check if number of 1's and 0's are same
        // Even if we leak certain NOT-THE-SAME rules other checks will rule them out to be equal.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(getHeadAtom(), rule.getHeadAtom())
                && isClosed() == rule.isClosed()
                && areTwoBodiesSame(getBodyAtoms(), rule.getBodyAtoms())
                && Objects.equals(getHeadCoverage(), rule.getHeadCoverage())
                && Objects.equals(getConfPCA(), rule.getConfPCA());
    }


    @Override
    public int hashCode() {
        return Objects.hash(getHeadAtom(), getBodyAtoms(), getHeadCoverage(), getConfPCA());
    }


    @Override
    public String toString() {
        return bodyAtoms.toString() + " =>"+ headAtom.toString();
    }
}