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
}
