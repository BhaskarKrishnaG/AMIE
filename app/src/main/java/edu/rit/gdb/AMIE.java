package edu.rit.gdb;

import edu.rit.gdb.Utils.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class AMIE {

    static Queue<Rule> queue = new LinkedList<>();
    static List<Rule> output = new ArrayList<>();
    final static double MIN_confPCA = 0.0;
    final static double MIN_HC = 0.01;
    final static int MAX_LEN = 3;
    public AddingAtoms addAtoms = new AddingAtoms();
    public static HashMap<Long, Integer> predicateCount = new HashMap<>();

    // Predicate to relation name.
    public static HashMap<Long, String> predicateName = new HashMap<>();

    /**
     * This method will add all the transactions (Rules of size 1) to the queue which will later be extended.
     *
     * @param db database.
     */
    public void initializeQueue(GraphDatabaseService db){
        Transaction tx = db.beginTx();

        // Get all the unique predicates/relationships these will be our initial facts.
        Result facts = db.execute("MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as predicates");

        while (facts.hasNext()){
            Map<String, Object> triple = facts.next();
            Rule newRule = new Rule();

            // The subject and object are just variables for the algorithm.
            Atom newAtom = new Atom(Long.parseLong((String)triple.get("predicates")), 0L, 1L,
                    predicateName.get(Long.parseLong((String)triple.get("predicates"))));
            newRule.setHeadAtom(newAtom);

            queue.add(newRule);
        }
        tx.close();
    }


    /**
     * This method will store the count of number of facts per relation.
     */
    public void memorizePredicateCount(GraphDatabaseService gdb) {
        Transaction tx = gdb.beginTx();

        String query = "MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as predicate, r.entity as relName, COUNT(r) as factCount";
        Result relations = gdb.execute(query);
        while (relations.hasNext()){
            Map<String, Object> relation = relations.next();
            predicateCount.put(Long.parseLong((String)relation.get("predicate")),
                    ((Number)relation.get("factCount")).intValue());
            predicateName.put(Long.parseLong((String)relation.get("predicate")),
                    (String)relation.get("relName"));
        }
        tx.close();
    }


    /**
     * This is the main loop of the algorithm.
     *
     * @param gdb graph database.
     */
    public void runAMIE(GraphDatabaseService gdb){

        memorizePredicateCount(gdb);
        initializeQueue(gdb);

        ComputingMetrics metricsAssistant = new ComputingMetrics();
        for(Rule r: queue){
            metricsAssistant.getHeadCoverage(r, gdb);
        }

        while (!queue.isEmpty()){
            Rule currentRule = queue.remove();
            boolean closed = currentRule.isClosed();

            boolean outputThisRule = closed
                    && betterThanParent(currentRule) && passesPCAThreshold(currentRule);

            // Perfect rule = confPCA(1.0)
            // Think: Should we continue refining a perfect rule? Maybe we will get a longer perfect rule?

            boolean furtherRefine = true;

            // If the rule is bigger than just the headAtom then we shall check if it's needs further refining.
            if (currentRule.getLength() > 1) {
                furtherRefine = !closed || (!(currentRule.getConfPCA() == 1.0) && currentRule.getLength() < MAX_LEN);
            }

            if (furtherRefine) {
                // Apply all mining operations.
                double k = currentRule.getHeadCoverage() * MIN_HC;
                List<Rule> tempRules = new ArrayList<>();

                // AMIE mines only length 3 rules, so if we are already length 2 then only add closing atoms.
                if (currentRule.getLength() < 2) {
                    addAtoms.addDanglingAtoms(gdb, currentRule, k, tempRules);
                }
                addAtoms.addClosingAtoms(gdb, currentRule, k, tempRules);

                // Check redundancy and add to final queue.
                for (Rule possibleNewRule: tempRules){
                    if (!queue.contains(possibleNewRule)){
                        queue.add(possibleNewRule);
                    }
                }
            }

            if (outputThisRule){
                if (!exitsInQueue(currentRule)){
                    output.add(currentRule);
                }
            }
        }
    }

    public boolean exitsInQueue(Rule currRule){
        boolean found = false;
        for (Rule r: output) {
            if (currRule.getLength() == r.getLength()) {
                if (r.getLength() == 2) {
                    found = compareAtom(currRule.getHeadAtom(), r.getHeadAtom())
                            && compareAtom(currRule.getBodyAtoms().get(0), r.getBodyAtoms().get(0));

                } else {
                    found = compareAtom(currRule.getHeadAtom(), r.getHeadAtom())
                            && ((compareAtom(currRule.getBodyAtoms().get(0), r.getBodyAtoms().get(0))
                            && compareAtom(currRule.getBodyAtoms().get(1), r.getBodyAtoms().get(1)))
                            || (compareAtom(currRule.getBodyAtoms().get(0), r.getBodyAtoms().get(1)))
                            && compareAtom(currRule.getBodyAtoms().get(1), r.getBodyAtoms().get(0)));
                }
                if (found) {
                    break;
                }
            }
        }
        return found;
    }

    public boolean compareAtom(Atom a, Atom b){
        if (a.getSubject() > 1)
            return b.getSubject()>1 && a.getObject().equals(b.getObject()) && a.relationshipName.equals(b.relationshipName);

        else if (a.getObject()>1)
            return b.getObject()>1 && a.getSubject().equals(b.getSubject()) && a.relationshipName.equals(b.relationshipName);

        else
            return a.getSubject().equals(b.getSubject()) && a.getObject().equals(b.getObject()) && a.relationshipName.equals(b.relationshipName);

//        if (a.getSubject() > 1)
//            return b.getSubject()>1 && a.getObject().equals(b.getObject()) && a.predicateId.equals(b.predicateId);
//
//        else if (a.getObject()>1)
//            return b.getObject()>1 && a.getSubject().equals(b.getSubject()) && a.predicateId.equals(b.predicateId);
//
//        else
//            return a.getSubject().equals(b.getSubject()) && a.getObject().equals(b.getObject()) && a.predicateId.equals(b.predicateId);
    }

    /**
     * This method tells if a rule passes the minimum PCA confidence threshold;
     *
     * @param r current rule.
     * @return bool.
     */
    public boolean passesPCAThreshold(Rule r){

        return r.getConfPCA() > MIN_confPCA;
    }


    /**
     * This method checks if a Rule (r) is better than it's parent.
     *
     * @param r current rule.
     * @return bool.
     */
    public boolean betterThanParent(Rule r){

        boolean isBetter = true;

        for (Rule ancestor : r.getParent()) {
            if (ancestor.getLength() > 1 && ancestor.isClosed()
                    && r.getConfPCA() < ancestor.getConfPCA()) {
                isBetter = false;
                break;
            }
        }
        return isBetter;
    }


    /**
     * The main method.
     *
     * @param args //THINK: What all should the user pass?
     */
    public static void main(String[] args) throws FileNotFoundException {

        if (args.length < 1) {
            System.out.println("Please pass the folder containing the Neo4j KB.");
            return;
        }

        final String neo4jFolder = args[0];

        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(neo4jFolder))
                .setConfig(GraphDatabaseSettings.pagecache_memory, "10G" )
                .newGraphDatabase();

        new AMIE().runAMIE(gdb);
        Collections.sort(output);
        for (Rule allRules: output){
            System.out.printf("%-90s \t%f\n", allRules, allRules.getConfPCA());
        }
        System.out.println("\n\nThe mined Rules are: " + output.size());

        CompareOutput.compareResults(output);

        gdb.shutdown();
    }
}
