package edu.rit.gdb;

import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;

import java.util.*;

public class Apriori_AMIE {

    ComputingMetrics metricAssistant = new ComputingMetrics();

    static String S1_graph = "MATCH (a)-[headRel0]->(b)";

    /*
     *      ( )-[ ]->( )
     *       ^
     *       |
     *      [ ]
     *       |
     *      ( )
     */
    static String S2_graph_1 = "MATCH (g)-[bodyRel1]->(a) MATCH (a)-[headRel]->(b)";

    /*
     *      ( )-[ ]->( )
     *       |
     *      [ ]
     *       |
     *       v
     *      ( )
     */
    static String S2_graph_2 = "MATCH (a)-[bodyRel1]->(g) MATCH (a)-[headRel]->(b)";

    /*
     *      ( )-[ ]->( )
     *                ^ fac
     *                |
     *               [ ]
     *                |
     *               ( )
     */
    static String S2_graph_3 = "MATCH (g)-[bodyRel1]->(b) MATCH (a)-[headRel]->(b)";

    /*
     *      ( )-[ ]->( )
     *                |
     *               [ ]
     *                |
     *                v
     *               ( )
     */
    static String S2_graph_4 = "MATCH (b)-[bodyRel1]->(g) MATCH (a)-[headRel]->(b)";

    /*
     *      ( )-[ ]->( )
     *       |        ^
     *       |________|
     *
     */
    static String S2_graph_5 = "MATCH (a)-[bodyRel1]->(b) MATCH (a)-[headRel]->(b)";

    /*
     *      ( )-[ ]->( )
     *       ^        |
     *       |________|
     *
     */
    static String S2_graph_6 = "MATCH (b)-[bodyRel1]->(a) MATCH (a)-[headRel]->(b)";

    String[] graphs = new String[]{S2_graph_1, S2_graph_2, S2_graph_3, S2_graph_4, S2_graph_5, S2_graph_6};

    // This map will memorize the PCA of each predicate/relationship for size 2 rules.
    static Map<Rule, Double> relPCA = new HashMap<>();
    public static HashMap<Long, Integer> predicateCount = new HashMap<>();
    public static HashMap<Long, String> predicateName = new HashMap<>();
    Set<Rule> rules = new HashSet<>();

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

    public void mainLoop(Session gdb){

        // Can we filter using support within the query?
        String filter = " WITH TYPE(bodyRel1) as bodyRel, TYPE(headRel) as headRel,  COUNT(DISTINCT id(headRel)) as" +
                " support RETURN bodyRel, headRel, support";

        int counter = 0;

        for( String graph: graphs){
            Result results = gdb.run(graph + filter);
            while (results.hasNext()){
                Record res = results.next();

//                System.out.println(graph.charAt(7) + "-" + graph.charAt(22) + "-" + graph.charAt(32) + "-" + graph.charAt(47));

                // Filtering based on headcount
                if (res.get("support").asInt() > predicateCount.get(Long.parseLong(res.get("headRel").asString())) * 0.01) {
                    Atom headAtom = new Atom(Long.parseLong(res.get("headRel").asString()), graph.charAt(33) + 1L, graph.charAt(48) + 1L
                            , predicateName.get(Long.parseLong(res.get("headRel").asString())));
                    Atom bodyAtom = new Atom(Long.parseLong(res.get("bodyRel").asString()), graph.charAt(7) + 1L, graph.charAt(23) + 1L
                            , predicateName.get(Long.parseLong(res.get("bodyRel").asString())));
                    Rule nRule = new Rule(headAtom, List.of(bodyAtom));
                    int supp = res.get("support").asInt();
                    nRule.setSupport(supp);
                    nRule.setHeadCoverage((supp * 1.0)
                            / predicateCount.get(nRule.getHeadAtom().getPredicateId()));
                    double pca = metricAssistant.computePCAConfidence(nRule, gdb, false);
                    rules.add(nRule);
                    relPCA.put(nRule, pca);
                }
            }
        }

        filter = " WITH TYPE(bodyRel1) as bodyRel1, TYPE(bodyRel2) as bodyRel2, TYPE(headRel) as headRel,  " +
                 "COUNT(DISTINCT id(headRel)) as support RETURN bodyRel1, bodyRel2, headRel, support";
        for( String graph: graphs) {
                // Get new combination of graphs.
                List<String> nGraphs = extendGraph(graph);

                // For each new combination.
                for (String nGraph: nGraphs){
                    Result results = gdb.run(nGraph + filter);
                    while (results.hasNext()){
                        Record res = results.next();

                        if (res.get("support").asInt() > predicateCount.get(Long.parseLong(res.get("headRel").asString())) * 0.01) {
                            Atom headAtom = new Atom(Long.parseLong(res.get("headRel").asString()), nGraph.charAt(59) + 1L, nGraph.charAt(74) + 1L
                                    , predicateName.get(Long.parseLong(res.get("headRel").asString())));
                            Atom bodyAtom1 = new Atom(Long.parseLong(res.get("bodyRel1").asString()), nGraph.charAt(7) + 1L, nGraph.charAt(23) + 1L
                                    , predicateName.get(Long.parseLong(res.get("bodyRel1").asString())));
                            Atom bodyAtom2 = new Atom(Long.parseLong(res.get("bodyRel2").asString()), nGraph.charAt(33) + 1L, nGraph.charAt(49) + 1L
                                    , predicateName.get(Long.parseLong(res.get("bodyRel2").asString())));
                            Rule nRule = new Rule(headAtom, List.of(bodyAtom1, bodyAtom2));
                            int supp = res.get("support").asInt();
                            nRule.setSupport(supp);
                            nRule.setHeadCoverage((supp * 1.0)
                                    / predicateCount.get(nRule.getHeadAtom().getPredicateId()));
                            double pca = metricAssistant.computePCAConfidence(nRule, gdb, false);
                            if (!pruneRule(gdb, nRule, pca))
                                rules.add(nRule);
                        }
                    }
                }
                counter++;
        }

        for (Rule r: rules){
            System.out.println(r + "\t" + r.getConfPCA());
        }
        System.out.println(rules.size());
    }


    /**
     * This method will cut the graph at different places to check if the "parents" have better pca
     *
     * @param currRule rule being broken
     * @param pca of the rule
     * @return true if pca is < ALL_PARENTS
     *         false otherwise
     */
    public boolean pruneRule(Session gdb, Rule currRule, double pca){
        boolean prune = false;
        for (Atom a: currRule.getBodyAtoms()){
            Rule smallerRule = new Rule();

            smallerRule.getBodyAtoms().add(a);
            smallerRule.setHeadAtom(currRule.getHeadAtom());

            // metricAssistant.computePCAConfidence(smallerRule, gdb,false)
            // relPCA.get(smallerRule)
            if (pca < metricAssistant.computePCAConfidence(smallerRule, gdb,false)){
                prune = true;
                break;
            }
        }
        return prune;
    }

    public List<String> extendGraph(String graph){
        List<String> nGraphs = new ArrayList<>();
        if ((graph.charAt(7) == 'a' && graph.charAt(23) == 'g') || (graph.charAt(7) == 'g' && graph.charAt(23) == 'a')){
            nGraphs.add("MATCH (b)-[bodyRel2]->(g) " + graph);
            nGraphs.add("MATCH (g)-[bodyRel2]->(b) " + graph);
        }
        else if ((graph.charAt(7) == 'b' && graph.charAt(23) == 'g') || (graph.charAt(7) == 'g' && graph.charAt(23) == 'b')){
            nGraphs.add("MATCH (a)-[bodyRel2]->(g) " + graph);
            nGraphs.add("MATCH (g)-[bodyRel2]->(a) " + graph);
        }
        else {
            nGraphs.add("MATCH (a)-[bodyRel2]->(b) " + graph);
            nGraphs.add("MATCH (b)-[bodyRel2]->(a) " + graph);
        }

        return nGraphs;
    }

    public void setup(Session gdb){
        memorizePredicateCount(gdb);
        mainLoop(gdb);
    }

    public static void main(String[] args) {
        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "YAGO";

        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();
        new Apriori_AMIE().setup(session);

        // [ (99)-[<diedIn>]->(104),  (98)-[<hasCapital>]->(104)] => (98)-[<isCitizenOf>]->(99)	0.0
        Atom a = new Atom(40L, 1L, 100L);
        Atom b = new Atom(3L, 0L, 100L);
        Atom c = new Atom(38L, 0L, 1L);
        Rule r = new Rule(c, List.of(a, b));
//        new Apriori_AMIE().
    }
}
