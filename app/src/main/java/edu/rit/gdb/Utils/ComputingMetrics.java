package edu.rit.gdb.Utils;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputingMetrics {

    /**
     * This method computes the headcount for a relation.
     *
     * hc(B => r(x, y)) := supp(B => r(x, y))/size(r).
     *
     * @return headCount.
     */
    public double getHeadCoverage(Rule r, GraphDatabaseService gdb){

        Transaction tx = gdb.beginTx();
        // Cypher query to get the relationship type and count #(x',y') with this type.
        int count = ((Number)gdb.execute("MATCH ()-[:`" +r.getHeadAtom().getPredicateId() + "`]->() " +
                        " RETURN COUNT(*) as cnt").next().get("cnt")).intValue();

        double hc = computeSupport(r, gdb)*1.0/count;
        tx.close();

        System.out.println("Head coverage: " + hc);
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
    //TODO: execute the cypher query and return the result
    public int computeSupport(Rule r, GraphDatabaseService gdb){

        Transaction tx = gdb.beginTx();
        StringBuilder query = new StringBuilder();
        queryBuilder(r, query);
//        query.append(" RETURN COUNT(*) as cnt");

//        query.append(" WITH DISTINCT (n").append(r.getHeadAtom().getSubject()).append("), (n")
//            .append(r.getHeadAtom().getObject()).append(") RETURN COUNT(*) as cnt");

        query.append(" RETURN COUNT(DISTINCT id(r)) as cnt");

//        System.out.println("Support Query: " + query + "\n\n");
        int supp = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
        tx.close();

        System.out.println("Support: " + supp);
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

        // Changing the object o to oPrime
//        query.deleteCharAt(query.length()-1);
//        query.append("Prime) WHERE id(n").append(o).append(") <> id(n").append(o).append("Prime) WITH DISTINCT n")

        // WITH DISTINCT a, b RETURN COUNT(*)
        query.append(" WITH DISTINCT n")
        .append(s).append(", n").append(o).append(" RETURN COUNT(*) as cnt");

        System.out.println("\nPCA Query: " + query);
        int count = ((Number)gdb.execute(query.toString()).next().get("cnt")).intValue();
        int supp = computeSupport(r, gdb);
//        double conPCA = supp*1.0/(supp + count);
        double conPCA = supp*1.0/(count);

        tx.close();

        System.out.println("PCA Confidence: " + conPCA);
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
//        query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("(n").append(o).append(")");
        query.append("(n").append(s).append(")-[r:`").append(p).append("`]->").append("( )");
    }

    public void appendNode(StringBuilder query, Long node){
        query.append("(n").append(node).append(")");

    }

    /*------------------------------------------------------------------------------------------
    /**
     * This is a helper method to generate the QUERY string-builder.
     * Reason: We want to preserve the locations of the entities from the head of a rule.
     * example: R: r1(x,z1) ^ r2(y, z2) ^ r3(z1, z2) => r(x,y)
     *             we want x and y to occur in the right positions in our generic query.
     *
     * @param query string builder.
     * @param s subject of the head.
     * @param o object of the head.
     * @param node current atom.
     * @param i placeHolder incrementer.

    public void appendNode(StringBuilder query, Long s, Long o, Long node, AtomicInteger i){
        String placeHolder = "z";

        if (node.equals(s)){
            query.append("(s)");
        }
        else if (node.equals(o)){
            query.append("(o)");
        }
        else{
            query.append("(").append(placeHolder).append(i.incrementAndGet()).append(")");
        }
    }
    /*------------------------------------------------------------------------------------------

    /**
     * This method will check of it's closed, meaning is there two occurrences of every subject/object.
     *
     * @return true - if it's close.
     *         false - otherwise.
     */
    public boolean checkIfClosed(Rule r){

        HashMap<Long, AtomicInteger> countVariables = new HashMap<>();
        for (Atom eachAtom: r.getBodyAtoms()){
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
        if (!r.getHeadAtom().getSubject().equals(r.getHeadAtom().getObject())
            && (!countVariables.containsKey(r.getHeadAtom().getSubject()) || !countVariables.containsKey(r.getHeadAtom().getObject()))){
            return false;
        }

        for (AtomicInteger count: countVariables.values()){
            if( count.intValue() < 2) {
                return false;
            }
        }
        return true;
    }
}
