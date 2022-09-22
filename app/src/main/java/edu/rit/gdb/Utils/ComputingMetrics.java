package edu.rit.gdb.Utils;

import edu.rit.gdb.AMIE;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.Map;

public class ComputingMetrics {

    /**
     * This method computes the headcount for a relation.
     *
     * hc(B => r(x, y)) := supp(B => r(x, y))/size(r).
     *
     * @return headCount.
     */
    public double getHeadCoverage(Rule r, Session gdb){

        double hc = computeSupport(r, gdb)*1.0/ AMIE.predicateCount.get(r.getHeadAtom().getPredicateId());

        r.setHeadCoverage(hc);
        return hc;
    }


    /**
     * This method computes the support for the rule.
     * i.e, the number of distinct pairs subject and object.
     *
     * supp(B => r(x,y)) := #(x,y):∃z1,...,zm : B ∧ r(x,y).
     *
     * @param r rules.
     * @return support value.
     */
    public int computeSupport(Rule r, Session gdb){

        StringBuilder query = new StringBuilder();
        queryBuilder(r, query);

        query.append(" RETURN COUNT(DISTINCT id(r)) as cnt");

        int supp = gdb.run(query.toString()).next().get("cnt").asInt();

        r.setSupport(supp);
        return supp;
    }


    /**
     * This method computes confidence under the PCA world.
     *
     *  confPCA (B => r(x,y)):=         supp( B => r(x, y))
     *                          -------------------------------
     *                          #(x,y):∃z1,...,zm,y′ :B ∧ r(x,y′)
     * @param r rule.
     * @return confidence score.
     */
    public double computePCAConfidence(Rule r, Session gdb, boolean graphSemantics) {

        Long s = r.getHeadAtom().getSubject();
        Long o = r.getHeadAtom().getObject();
        StringBuilder query = new StringBuilder();

        double conPCA = 0;

        queryBuilderForPCA(r, query, r.getFunctionalVariable(), graphSemantics);
        query.append(" WITH DISTINCT n");

        // If the rule is not closed then we have two choices, only one is right
        // (s)-[]->() || ()-[]->(o)
        // pick the right one.
        if (!r.isClosed()){
            if (r.isPartOfBody(s)){
                query.append(s).append(" RETURN COUNT(*) as cnt");
            }
            else {
                query.append(o).append(" RETURN COUNT(*) as cnt");
            }

        }

        // If the rule is closed will can have both subject and object in the query.
        else {
            query.append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");
        }

        int count = gdb.run(query.toString()).next().get("cnt").asInt();
        // Can count be 0?
        if (count != 0) {
            conPCA = r.getSupport() * 1.0 / (count);
        }
        r.setConfPCA(conPCA);

        return conPCA;

    }


    /**
     * This is a generic query builder method.
     *
     * @param r rule.
     * @param query string builder.
     */
    public void queryBuilder(Rule r, StringBuilder query){
        Long s = r.getHeadAtom().getSubject();
        Long o = r.getHeadAtom().getObject();
        Long p = r.getHeadAtom().getPredicateId();

        query.append("MATCH ");

        for (Atom body: r.getBodyAtoms()){
            Long sPrime = body.getSubject();
            Long oPrime = body.getObject();

            appendNode(query,sPrime);
            query.append("-[:`").append(body.getPredicateId()).append("`]->");
            appendNode(query,oPrime);
            query.append(" MATCH ");
        }

        // Now add the head section of the rule.
        query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("(n").append(o).append(")");
    }


    /**
     * This is a PCA query builder method.
     *
     * @param r rule.
     * @param query string builder.
     * @param functionalVariable which variable to set blank.
     */
    public void queryBuilderForPCA(Rule r, StringBuilder query, int functionalVariable, boolean graphSemantics){
        Long s = r.getHeadAtom().getSubject();
        Long p = r.getHeadAtom().getPredicateId();
        Long o = r.getHeadAtom().getObject();

        query.append("MATCH ");

        for (Atom body: r.getBodyAtoms()){
            Long sPrime = body.getSubject();
            Long oPrime = body.getObject();

            appendNode(query,sPrime);
            query.append("-[:`").append(body.getPredicateId()).append("`]->");
            appendNode(query,oPrime);

            if(graphSemantics){
                query.append(" , ");
            }
            else {
                query.append(" MATCH ");
            }
        }

        // Set Object as blank.
        if (functionalVariable == 0) {
            query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("()");
        }

        // Set Subject as blank.
        else {
            query.append("()-[r:`").append(p).append("`]->").append("(n").append(o).append(")");
        }

    }


    /**
     * This is a helper method to create a cypher query.
     *
     * @param query to append to.
     * @param node in the db.
     */
    public void appendNode(StringBuilder query, Long node){
        query.append("(n").append(node).append(")");
    }

    public int getFunctionality(Session gdb, Rule r) {
        Record row = gdb.run("  MATCH (s)-[r]->(o) " +
                        " WHERE type(r) = $predicate " +
                        " RETURN count(DISTINCT s) as domainCount, count(DISTINCT o) as coDomainCount",
                Map.of("predicate", String.valueOf(r.getHeadAtom().getPredicateId()))).next();

        return row.get("domainCount").asInt() >= row.get("coDomainCount").asInt() ?  0 :  1;
    }
}