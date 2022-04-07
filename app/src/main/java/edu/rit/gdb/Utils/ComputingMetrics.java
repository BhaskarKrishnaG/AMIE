package edu.rit.gdb.Utils;

import edu.rit.gdb.AMIE;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

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

//        System.out.println("Head coverage: " + hc);
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
    public double computePCAConfidence(Rule r, GraphDatabaseService gdb) {

        Transaction tx = gdb.beginTx();
        Long s = r.getHeadAtom().getSubject();
        Long o = r.getHeadAtom().getObject();
        StringBuilder query = new StringBuilder();
        queryBuilderForPCA(r, query);
        query.append(" WITH DISTINCT n");

        if (!r.isPartOfBody(o)) {
            query.append(s).append(" RETURN COUNT(*) as cnt");
        }
        else {
            query.append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");
        }

        int count = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
        double conPCA = r.getSupport()*1.0/(count);

//        if (r.isClosed()) {
//            System.out.println("\nPCA Query: " + query);
//            System.out.println("support: " + r.getSupport());
//            System.out.println("count: " + count);
//        }
        tx.close();

        r.setConfPCA(conPCA);
        return conPCA;
    }


    /**
     * This method computes confidence under the PCA world.
     *
     *  confPCA (B => r(x,y)):=         supp( B => r(x, y))
     *                          -------------------------------
     *                          #(x,y):∃z1,...,zm,x′ :B ∧ r(x',y)
     * @param r rule.
     * @return confidence score.
     */
    public double computePCAConfidenceFixObject(Rule r, GraphDatabaseService gdb) {

        Transaction tx = gdb.beginTx();
        StringBuilder query = new StringBuilder();
        queryBuilder(r, query);
        query.append(" WHERE id(s) <> ").append(r.getHeadAtom().getSubject()).append(" RETURN COUNT(*) as cnt");

        System.out.println("PCA Query: " + query + "\n\n");
        int conPCA = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
        tx.close();

        System.out.println("PCA Confidence our change: " + conPCA);
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
     * This is a generic query builder method.
     *
     * @param r rule.
     * @param query string builder.
     */
    public void queryBuilderForPCA(Rule r, StringBuilder query){
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

        // Now add the head section of the rule.
        query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("()");

        // Just to test b being functional variable
//        query.append("()-[r:`").append(p).append("`]->").append("(n").append(o).append(")");

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
