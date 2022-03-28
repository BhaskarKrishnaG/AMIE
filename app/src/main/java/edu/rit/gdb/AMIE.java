package edu.rit.gdb;

import edu.rit.gdb.Utils.AddingAtoms;
import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

public class AMIE {

    static Queue<Rule> queue = new LinkedList<>();
    static Set<Rule> output = new HashSet<>();
    final static double MIN_CONFPCA = 0.1;
    final static double MIN_HC = 0.01;
    final static int MAX_LEN = 3;
    public AddingAtoms addAtoms = new AddingAtoms();


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
     * This is the main loop of the algorithm.
     *
     * @param db graph database.
     */
    public void runAMIE(GraphDatabaseService db){

        initializeQueue(db);
        // Should I compute the metrics of these initial facts?
        // Let's try
        ComputingMetrics metricsAssistant = new ComputingMetrics();
        for(Rule r: queue){
            metricsAssistant.getHeadCoverage(r, db);
        }

        while (!queue.isEmpty()){
            Rule currentRule = queue.remove();

            boolean outputThisRule = currentRule.checkIfClosed()
                    && betterThanParent(currentRule) && passesPCAThreshold(currentRule);

            // Perfect rule = confPCA(1.0)
            // Think: Should we continue refining a perfect rule? Maybe we will get a longer perfect rule?

            boolean furtherRefine = true;

            // If the rule is bigger than just the headAtom then we shall check if it's needs further refining.
            if (currentRule.getLength() > 1)
                furtherRefine = !(currentRule.getConfPCA() == 1.0);

            if (furtherRefine) {
                // Apply all mining operations.
                double k = currentRule.getHeadCount() * MIN_HC;
                Set<Long> promisingRel = addAtoms.getPossibleRelations(db, currentRule, k);
                Set<Rule> tempRules = new HashSet<>();

                //Think: Do we get promising relations inside these operators?
                for (Long rel: promisingRel) {
                    addAtoms.addDanglingAtoms(db, currentRule, rel, k, tempRules);
                    addAtoms.addClosingAtoms(db, currentRule, rel, k, tempRules);
                }

                System.out.println(queue.toString());
                // TODO: check redundancy and add to final queue.
            }

            if (outputThisRule){
                output.add(currentRule);
            }
        }
    }


    /**
     * This method tells if a rule passes the minimum PCA confidence threshold;
     *
     * @param r current rule.
     * @return bool.
     */
    public boolean passesPCAThreshold(Rule r){

        return r.getConfPCA() >= MIN_CONFPCA;
    }


    /**
     * This method checks if a Rule (r) is better than it's parent.
     *
     * @param r current rule.
     * @return bool.
     */
    public boolean betterThanParent(Rule r){
        Rule parent = r.getParent();

        // TODO: AMIE implementation is going through all ancestors, do we need to as well?
        return r.getConfPCA() >= parent.getConfPCA();
    }


    /**
     * The main method.
     *
     * @param args //THINK: What all should the user pass?
     */
    public static void main(String[] args) {
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";
        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jFolder));

        new AMIE().runAMIE(gdb);
    }
}
