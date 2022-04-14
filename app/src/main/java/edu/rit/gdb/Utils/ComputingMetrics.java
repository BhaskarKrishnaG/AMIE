package edu.rit.gdb.Utils;

import edu.rit.gdb.AMIE;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.sql.Timestamp;

public class ComputingMetrics {

    /**
     * This method computes the headcount for a relation.
     *
     * hc(B => r(x, y)) := supp(B => r(x, y))/size(r).
     *
     * @return headCount.
     */
    public double getHeadCoverage(Rule r, GraphDatabaseService gdb){

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
    public int computeSupport(Rule r, GraphDatabaseService gdb){

        Transaction tx = gdb.beginTx();
        StringBuilder query = new StringBuilder();
        queryBuilder(r, query);

        query.append(" RETURN COUNT(DISTINCT id(r)) as cnt");

        int supp = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
        tx.close();

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
    public double computePCAConfidence(Rule r, GraphDatabaseService gdb, boolean graphSemantics) {

        Transaction tx = gdb.beginTx();
        Long s = r.getHeadAtom().getSubject();
        Long o = r.getHeadAtom().getObject();
        StringBuilder query = new StringBuilder();

        // If the rule is closed and size 2 then it's PCA is computed for the first time.
        // Try both subject and object as the joining variables and keep greater PCA.
        if (r.getLength() == 2 && r.isClosed()){
            double subjectPCA = 0;
            double objectPCA = 0;

            queryBuilderForPCA(r, query, 1, graphSemantics);
            query.append(" WITH DISTINCT n");
            query.append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");
            System.out.println(new Timestamp(System.currentTimeMillis()).getTime() + "\t" + query);

            int subjectBlankCount = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();

            // Can count be 0?
            if (subjectBlankCount != 0){
                subjectPCA = r.getSupport()*1.0/(subjectBlankCount);
            }

            query = new StringBuilder();
            queryBuilderForPCA(r, query, 2, graphSemantics);
            query.append(" WITH DISTINCT n");
            query.append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");
            System.out.println(new Timestamp(System.currentTimeMillis()).getTime() + "\t" + query);

            int objectBlankCount = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();

            // Can count be 0?
            if (subjectBlankCount != 0) {
                objectPCA = r.getSupport() * 1.0 / (objectBlankCount);
            }
            if (subjectPCA >= objectPCA){
                r.setJoiningAtom(1);
                r.setConfPCA(subjectPCA);

                tx.close();
                return subjectPCA;
            }
            else {
                r.setJoiningAtom(2);
                r.setConfPCA(objectPCA);

                tx.close();
                return objectPCA;
            }
        }

        // If the rules is not closed and we are computing PCA for the first time then get the joining variable/common
        // variable and use that for PCA.
        else if (r.getLength() == 2){
            double conPCA = 0;

            if (r.isPartOfBody(s)){
                r.setJoiningAtom(1);
                queryBuilderForPCA(r, query, 1, graphSemantics);
                query.append(" WITH DISTINCT n");
                query.append(s).append(" RETURN COUNT(*) as cnt");
            }
            else {
                r.setJoiningAtom(2);
                queryBuilderForPCA(r, query, 2, graphSemantics);
                query.append(" WITH DISTINCT n");
                query.append(o).append(" RETURN COUNT(*) as cnt");
            }

            System.out.println(new Timestamp(System.currentTimeMillis()).getTime() + "\t" + query);
            int count = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
            // Can count be 0?
            if (count != 0) {
                conPCA = r.getSupport() * 1.0 / (count);
            }
            r.setConfPCA(conPCA);

            tx.close();
            return conPCA;
        }

        // If we are here then the PCA for shorter version of this rule was already computed, use that joining variable.
        // At this point, since AMIE only mines closed rules and just length 3 we will have both subject and object in the
        // body of the rule so we will use both in the filter section.
        // Note: If we decide to mine longer rules in future this needs more work.
        //       We will have to check if subject and object are present and build the filter accordingly.
        else {
            double conPCA = 0;

            queryBuilderForPCA(r, query, r.getJoiningAtom(), graphSemantics);
            query.append(" WITH DISTINCT n");
            query.append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");
            System.out.println(new Timestamp(System.currentTimeMillis()).getTime() + "\t" + query);

            int count = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
            // Can count be 0?
            if (count != 0) {
                conPCA = r.getSupport() * 1.0 / (count);
            }
            r.setConfPCA(conPCA);

            tx.close();
            return conPCA;
        }
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
     * Decider method for which semantics to use for the query.
     *
     * @param r rule.
     * @param query string builder.
     * @param joiningVariable which variable to set blank.
     */
    public void queryBuilderForPCA(Rule r, StringBuilder query, int joiningVariable, boolean graphSemantics){
        if (graphSemantics) {
            queryBuilderForPCA_GraphSemantics(r, query, joiningVariable);
        } else {
            queryBuilderForPCA_PrologSemantics(r, query, joiningVariable);
        }
    }


    /**
     * This is a PCA query builder method.
     *
     * @param r rule.
     * @param query string builder.
     * @param joiningVariable which variable to set blank.
     */
    public void queryBuilderForPCA_PrologSemantics(Rule r, StringBuilder query, int joiningVariable){
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
            query.append(" MATCH ");
        }

        // Set subject as blank.
        if (joiningVariable == 1) {
            query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("()");
        }

        // Set Object as blank.
        else {
            query.append("()-[r:`").append(p).append("`]->").append("(n").append(o).append(")");
        }

    }


    /**
     * This is a PCA query builder method.
     *
     * @param r rule.
     * @param query string builder.
     * @param joiningVariable which variable to retain.
     */
    public void queryBuilderForPCA_GraphSemantics(Rule r, StringBuilder query, int joiningVariable){
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
            query.append(", ");
        }

        // Set subject as blank.
        if (joiningVariable == 1) {
            query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("(n").append(o).append("PRIME)")
                    .append(" WHERE id(n").append(o).append(") <> id(n").append(o).append("PRIME) ");
        }

        // Set Object as blank.
        else {
            query.append("(n").append(s).append("PRIME)-[r:`").append(p).append("`]->").append("(n").append(o)
                    .append(") WHERE id(n").append(s).append(") <> id(n").append(s).append("PRIME) ");
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

}