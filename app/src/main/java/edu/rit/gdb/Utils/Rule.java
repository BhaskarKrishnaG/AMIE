package edu.rit.gdb.Utils;

import java.util.HashSet;
import java.util.Set;

public class Rule {

    Atom headAtom;
    // not modifyiable set
    Set<Atom> bodyAtoms;

    // THINK: Do we want a variable that indicates the closeness? Or as a property?

    public Rule(Atom headAtom, Set<Atom> bodyAtoms) {
        this.headAtom = headAtom;
        this.bodyAtoms = bodyAtoms;
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

    public Set<Atom> getBodyAtoms() {
        return bodyAtoms;
    }

    public void setBodyAtoms(Set<Atom> bodyAtoms) {
        this.bodyAtoms = bodyAtoms;
    }
}
