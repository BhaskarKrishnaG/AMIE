package edu.rit.gdb;

import edu.rit.gdb.Utils.*;
import org.neo4j.driver.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class AMIE {

//    static Queue<Rule> queue = new LinkedList<>();
    PriorityQueue<Rule> queue = new PriorityQueue<>(Collections.reverseOrder());


    static List<Rule> output = new ArrayList<>();
    static List<Rule> rejectedByParentPruning = new ArrayList<>();
    final static double MIN_confPCA = 0.5;
    final static double MIN_HC = 0.01;
    final static int MAX_LEN = 3;
    public AddingAtoms addAtoms = new AddingAtoms();
    public static HashMap<Long, Integer> predicateCount = new HashMap<>();
    ComputingMetrics metricsAssistant = new ComputingMetrics();

    // Predicate to relation name.
    public static HashMap<Long, String> predicateName = new HashMap<>();

    /**
     * This method will add all the transactions (Rules of size 1) to the queue which will later be extended.
     *
     * @param gdb database.
     */
    public void initializeQueue(Session gdb){

        // Get all the unique predicates/relationships these will be our initial facts.
        Result facts = gdb.run("MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as predicates");

        while (facts.hasNext()){
            Map<String, Object> triple = facts.next().asMap();
            Rule newRule = new Rule();

            // The subject and object are just variables for the algorithm.
            Atom newAtom = new Atom(Long.parseLong((String)triple.get("predicates")), 0L, 1L,
                    predicateName.get(Long.parseLong((String)triple.get("predicates"))));
            newRule.setHeadAtom(newAtom);
            newRule.setFunctionalVariable(metricsAssistant.getFunctionality(gdb, newRule));

            queue.add(newRule);
        }
    }


    /**
     * This method will store the count of number of facts per relation.
     */
    public void memorizePredicateCount(Session gdb) {

        String query = "MATCH ()-[r]->() RETURN DISTINCT TYPE(r) as predicate, r.entity as relName, COUNT(r) as factCount";
        Result relations = gdb.run(query);
        while (relations.hasNext()){
            Map<String, Object> relation = relations.next().asMap();
            predicateCount.put(Long.parseLong((String)relation.get("predicate")),
                    ((Number)relation.get("factCount")).intValue());
            predicateName.put(Long.parseLong((String)relation.get("predicate")),
                    (String)relation.get("relName"));
        }
    }


    /**
     * This is the main loop of the algorithm.
     *
     * @param gdb graph database.
     */
    public void runAMIE(Session gdb){

        memorizePredicateCount(gdb);
        initializeQueue(gdb);

        for(Rule r: queue){
            metricsAssistant.getHeadCoverage(r, gdb);
        }

        while (!queue.isEmpty()){
            Rule currentRule = queue.poll();

            if (currentRule.getHeadAtom().getPredicateId().equals(42L)
            && currentRule.getLength() == 2
            && currentRule.getBodyAtoms().equals(List.of(new Atom(57L, 0L, 1L)))){
                System.out.println("here");
            }
            boolean closed = currentRule.isClosed();

            boolean outputThisRule = closed
                    && passesPCAThreshold(currentRule) && betterThanParent(currentRule);

            // Perfect rule = confPCA(1.0)
            // Think: Should we continue refining a perfect rule? Maybe we will get a longer perfect rule?

            boolean furtherRefine = true;

            // If the rule is bigger than just the headAtom then we shall check if it needs further refining.
            if (currentRule.getLength() > 1) {
                furtherRefine = !closed || (currentRule.getConfPCA() != 1.0 && currentRule.getLength() < MAX_LEN);
            }

            if (furtherRefine) {
                // Apply all mining operations.
                double k = predicateCount.get(currentRule.getHeadAtom().getPredicateId()) * MIN_HC;
//                double k = currentRule.getHeadCoverage() * MIN_HC;
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
                if (!exitsInOutput(currentRule)){
                    output.add(currentRule);
                }
            }
            else{
                rejectedByParentPruning.add(currentRule);
            }
        }
    }

    public boolean exitsInOutput(Rule currRule){
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
            return b.getSubject()>1 && a.getObject().equals(b.getObject()) && a.predicateId.equals(b.predicateId);

        else if (a.getObject()>1)
            return b.getObject()>1 && a.getSubject().equals(b.getSubject()) && a.predicateId.equals(b.predicateId);

        else
            return a.getSubject().equals(b.getSubject()) && a.getObject().equals(b.getObject()) && a.predicateId.equals(b.predicateId);
    }

    /**
     * This method tells if a rule passes the minimum PCA confidence threshold;
     *
     * @param r current rule.
     * @return bool.
     */
    public boolean passesPCAThreshold(Rule r){

        return r.getConfPCA() >= MIN_confPCA;
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

        for (Rule rejected : rejectedByParentPruning){
            if ( r.getLength() == rejected.getLength()
                    && r.isClosed() == rejected.isClosed()
                    && r.getHeadAtom().getPredicateId().equals(rejected.getHeadAtom().getPredicateId())
                    && r.getConfPCA().equals(rejected.getConfPCA())
                    && r.getHeadCoverage().equals(rejected.getHeadCoverage())) {
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

        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "WN11";

        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();

        if (args.length < 1) {
            System.out.println("Please pass the folder containing the Neo4j KB.");
            return;
        }

        final String neo4jFolder = args[0];

        System.out.println("Database: " + neo4jFolder);

        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(neo4jFolder))
                .setConfig(GraphDatabaseSettings.pagecache_memory, "10G" )
                .newGraphDatabase();

        new AMIE().runAMIE(session);
        Collections.sort(output);
        for (Rule allRules: output){
            System.out.printf("%-90s \t%f\t%f\t%d\n", allRules, allRules.getConfPCA(), allRules.getHeadCoverage()
                    , AMIE.predicateCount.get(allRules.getHeadAtom().getPredicateId()));
        }
        System.out.println("\n\nThe mined Rules are: " + output.size());

        CompareOutput.compareResults(output);

        gdb.shutdown();

    }
}
