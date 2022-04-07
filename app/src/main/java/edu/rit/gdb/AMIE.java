package edu.rit.gdb;

import edu.rit.gdb.Utils.AddingAtoms;
import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.*;

public class AMIE {

    static Queue<Rule> queue = new LinkedList<>();
    static Set<Rule> output = new HashSet<>();
    final static double MIN_confPCA = 0.0;
    final static double MIN_HC = 0.01;
    final static int MAX_LEN = 3;
    public AddingAtoms addAtoms = new AddingAtoms();
    public static HashMap<Long, Integer> predicateCount = new HashMap<>();


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
            Atom newAtom = new Atom(Long.parseLong((String)triple.get("predicates")), 0L, 1L);
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

        String query = "MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as predicate, COUNT(r) as factCount";
        Result relations = gdb.execute(query);
        while (relations.hasNext()){
            Map<String, Object> relation = relations.next();
            predicateCount.put(Long.parseLong((String)relation.get("predicate")),
                    ((Number)relation.get("factCount")).intValue());
        }
        tx.close();
    }


    /**
     * This is the main loop of the algorithm.
     *
     * @param gdb graph database.
     */
    public void runAMIE(GraphDatabaseService gdb){

        initializeQueue(gdb);
        memorizePredicateCount(gdb);

        ComputingMetrics metricsAssistant = new ComputingMetrics();
        for(Rule r: queue){
            metricsAssistant.getHeadCoverage(r, gdb);
        }

        while (!queue.isEmpty()){
            Rule currentRule = queue.remove();

            boolean outputThisRule = currentRule.isClosed()
                    && betterThanParent(currentRule) && passesPCAThreshold(currentRule);

            // Perfect rule = confPCA(1.0)
            // Think: Should we continue refining a perfect rule? Maybe we will get a longer perfect rule?

            boolean furtherRefine = true;

            // If the rule is bigger than just the headAtom then we shall check if it's needs further refining.
            if (currentRule.getLength() > 1) {
                furtherRefine = (!(currentRule.getConfPCA() == 1.0) && currentRule.getLength() < MAX_LEN);
            }

            if (furtherRefine) {
                // Apply all mining operations.
                double k = currentRule.getHeadCoverage() * MIN_HC;
                Set<Rule> tempRules = new HashSet<>();

                addAtoms.addDanglingAtoms(gdb, currentRule, k, tempRules);
                addAtoms.addClosingAtoms(gdb, currentRule, k, tempRules);

                // Check redundancy and add to final queue.
                for (Rule possibleNewRule: tempRules){
                    if (!queue.contains(possibleNewRule)){
                        queue.add(possibleNewRule);
                    }
                }
            }

            if (outputThisRule){
                output.add(currentRule);
            }
            System.out.println("Rules mined: " + output.size());
        }
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
                    && r.getConfPCA() <= ancestor.getConfPCA()) {
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
    public static void main(String[] args) {
//        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";
//        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/Yoga2S/db";

        if (args.length < 1) {
            System.out.println("Please pass the folder containing the Neo4j KB.");
            return;
        }

        final String neo4jFolder = args[0];
        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(neo4jFolder))
                .setConfig(GraphDatabaseSettings.pagecache_memory, "10G" )
                .newGraphDatabase();

        new AMIE().runAMIE(gdb);
        System.out.println("\n\nThe mined Rules are: " + output.size());
        for (Rule allRules: output){
            System.out.println(allRules.toString());
        }

        gdb.shutdown();
    }
}
