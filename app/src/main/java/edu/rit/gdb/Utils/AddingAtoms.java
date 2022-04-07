package edu.rit.gdb.Utils;

import edu.rit.gdb.AMIE;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddingAtoms {

    ComputingMetrics metricAssistant = new ComputingMetrics();

    /**
     * This method queries the KB to get a new relationship to extend the rule.
     *
     * @param gdb database.
     * @param r rule.
     * @param k minHC X support of the rule.
     * @return relations.
     */
    public Set<Long> getPossibleRelations(GraphDatabaseService gdb, Rule r, double k){

        Set<Long> result = new HashSet<>();

        //TODO: Add the threshold for length of the rule as an argument as well?

        StringBuilder query = buildCurrentRule(r);

        // THINK: Add minimum support as argument?
        query.append(" MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as rel, COUNT(r) as relCount");

        Result res = gdb.execute(query.toString());
        while (res.hasNext()){
            Map<String, Object> relation = res.next();
            if (((Number)relation.get("relCount")).intValue() >= k){
                result.add(Long.parseLong((String)relation.get("rel")));
            }
        }

        return result;
    }

    /**
     * This method adds dangling atoms.
     * This operator adds a new atom to a rule. The new atom uses a fresh variable for one of its two arguments.
     * The other argument is a variable that is shared with the rule, i.e., it occurs in some other atom of the rule.
     *
     * @param gdb database.
     * @param r rule.
     * @param k threshold.
     */
    public void addDanglingAtoms(GraphDatabaseService gdb, Rule r, double k, Set<Rule> danglingRules){

        StringBuilder query = buildCurrentRule(r);

        // If it's an intermediate rule then we only need open variables.
        Set<Long> variables = r.getOpenVariables();

        // If the rule is closed then we need to try all combinations.
        if (r.isClosed()){
            variables = r.getALLVariables();
        }
        for (Long v: variables) {
            executeDandling(gdb, r, k, query, v, true, danglingRules);
            executeDandling(gdb, r, k, query, v, false, danglingRules);
        }

    }

    /**
     * This method checks the database for the number of occurrences/support?
     *
     * @param gdb database.
     * @param r rule.
     * @param k threshold.
     * @param query pre-built cypher query.
     * @param v variable.
     * @param subject s.
     * @param danglingRules all the new rules that meet the threshold.
     */
    public void executeDandling(GraphDatabaseService gdb, Rule r,
                                double k, StringBuilder query, Long v, boolean subject, Set<Rule> danglingRules){

        StringBuilder tempQuery = new StringBuilder();
        Atom a = new Atom();
        tempQuery.append(" MATCH ");
        if (subject) {
            appendNode(tempQuery, v);
            tempQuery.append("-[r]->");
            appendNode(tempQuery, v + 100); //THINK: Is there a better way?

            a.setSubject(v);
            a.setObject(v+100);
        }
        else {
            appendNode(tempQuery, v+100);
            tempQuery.append("-[r]->");
            appendNode(tempQuery, v); //THINK: Is there a better way?

            a.setSubject(v+100);
            a.setObject(v);
        }
        tempQuery.append(" RETURN DISTINCT TYPE(r) as predicate, COUNT(DISTINCT id(headRel)) as support ORDER BY support DESC");

        Transaction tx = gdb.beginTx();

        String finalQuery = query.toString() + tempQuery.toString();
        Result res = gdb.execute(finalQuery);
        while (res.hasNext()){
            Map<String, Object> relation = res.next();
            if (((Number)relation.get("support")).intValue() >= k){
                Atom newAtom = a.deepCopyAtom();
                Long predicate = Long.parseLong((String)relation.get("predicate"));
                newAtom.setPredicateId(predicate);
                Rule newRule = r.deepCopyRule();
                newRule.getBodyAtoms().add(newAtom);
                danglingRules.add(newRule);

                // Let's set the properties of the rule
                newRule.setParent(r);
                int support = ((Number)relation.get("support")).intValue();
                newRule.setSupport(support);
                newRule.setHeadCoverage((support * 1.0)
                        / AMIE.predicateCount.get(r.getHeadAtom().getPredicateId()));
                metricAssistant.computePCAConfidence(newRule, gdb);
            }
            // Since we are sorting the support by descending order remaining
            // will also not satisfy the Threshold
            else{
                break;
            }
        }
        tx.close();
    }

    /**
     * This method adds closing atoms.
     * This operator adds a new atom to a rule so that both of its arguments are shared with the rule.
     *
     * @param gdb database.
     * @param r rule.
     * @param k threshold.
     * @param closedRules results.
     */
    public void addClosingAtoms(GraphDatabaseService gdb, Rule r, double k, Set<Rule> closedRules){
        Transaction tx = gdb.beginTx();
        StringBuilder query = buildCurrentRule(r);

        Set<Long> sVariables = r.getALLVariables();
        Set<Long> oVariables = r.getALLVariables();

        if (!r.isClosed()) {
            sVariables = r.getOpenVariables();
            oVariables = r.getOpenVariables();
        }

        for (Long subject: sVariables){
            for (Long object: oVariables){
                if (!subject.equals(object)){
                    StringBuilder tempQuery = new StringBuilder();
                    tempQuery.append(" MATCH ");
                    appendNode(tempQuery, subject);
                    tempQuery.append("-[r]->");
                    appendNode(tempQuery, object);

                    tempQuery.append(" RETURN DISTINCT TYPE(r) as predicate, COUNT(DISTINCT id(headRel)) as support ORDER BY support DESC");
                    String finalQuery = query.toString() + tempQuery.toString();

                    Result res = gdb.execute(finalQuery);
                    while (res.hasNext()){
                        Map<String, Object> relation = res.next();
                        if (((Number)relation.get("support")).intValue() >= k){
                            // check for redundancy
                            Long predicate = Long.parseLong((String)relation.get("predicate"));
                            Atom a = new Atom(predicate, subject, object);
                            Rule newRule = r.deepCopyRule();
                            newRule.getBodyAtoms().add(a);

                            // If this combination of subject and object already exists then move on to next.
                            if (!r.containsAtom(a)) {
                                closedRules.add(newRule);

                                // Let's set the properties of the rule
                                newRule.setParent(r);
                                int support = ((Number)relation.get("support")).intValue();
                                newRule.setSupport(support);
                                newRule.setHeadCoverage((support * 1.0)
                                        / AMIE.predicateCount.get(r.getHeadAtom().getPredicateId()));
                                metricAssistant.computePCAConfidence(newRule, gdb);
                            }
                        }

                        // Since we are sorting the support by descending order remaining
                        // will also not satisfy the Threshold
                        else{
                            break;
                        }
                    }
                }
            }
        }
        tx.close();
    }


    /**
     * This method takes a rule and build a cypher query of it.
     *
     * @param r rules.
     * @return cypher query.
     */
    public StringBuilder buildCurrentRule(Rule r) {
        Long s = r.getHeadAtom().getSubject();
        Long p = r.getHeadAtom().getPredicateId();
        Long o = r.getHeadAtom().getObject();

        StringBuilder query = new StringBuilder();
        query.append("MATCH ");

        for (Atom body: r.getBodyAtoms()){
            Long sPrime = body.getSubject();
            Long oPrime = body.getObject();

            appendNode(query,sPrime);
            query.append("-[:`").append(body.getPredicateId()).append("`]->");
            appendNode(query,oPrime);
            query.append(" MATCH ");
        }

        appendNode(query,s);
        query.append("-[headRel:`").append(p).append("`]->");
        appendNode(query,o);

        return query;
    }

    /**
     * A helper method to create cypher query.
     *
     * @param query buffer to add to.
     * @param node node to add.
     */
    public void appendNode(StringBuilder query, Long node){
        query.append("(n").append(node).append(")");
    }
}
