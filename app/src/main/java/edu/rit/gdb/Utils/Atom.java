package edu.rit.gdb.Utils;

public class Atom {
    public Long predicateId;
    public String relationshipName;
    public Long subject;
    public Long object;

    public Atom(Long predicateId, Long subject, Long object) {
        this.predicateId = predicateId;
        this.subject = subject;
        this.object = object;
    }

    public Atom(Long predicateId, Long subject, Long object, String name) {
        this.predicateId = predicateId;
        this.subject = subject;
        this.object = object;
        this.relationshipName = name;
    }

    public Atom() {
        // Do nothing.
    }

    public Long getPredicateId() {
        return predicateId;
    }

    public void setPredicateId(Long predicateId) {
        this.predicateId = predicateId;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public Long getSubject() {
        return subject;
    }

    public void setSubject(Long subject) {
        this.subject = subject;
    }

    public Long getObject() {
        return object;
    }

    public void setObject(Long y) {
        this.object = y;
    }

    /**
     * This method creates a new Atom with same signature.
     *
     * @return new Atom.
     */
    public Atom deepCopyAtom(){
        Atom newAtom = new Atom();
        newAtom.setSubject(getSubject());
        newAtom.setPredicateId(getPredicateId());
        newAtom.setObject(getObject());
        newAtom.setRelationshipName(getRelationshipName());

        return newAtom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atom atom = (Atom) o;

        if (this.predicateId == null || atom.predicateId == null)
            return relationshipName.equals(atom.getRelationshipName()) && subject.equals(atom.subject) && object.equals(atom.object);
        return predicateId.equals(atom.predicateId) && subject.equals(atom.subject) && object.equals(atom.object);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return " (" + subject + ")-[" + relationshipName + "]->(" + object + ")";
    }
}