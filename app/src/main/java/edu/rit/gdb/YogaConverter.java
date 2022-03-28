package edu.rit.gdb;

import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;

public class YogaConverter {
    static Long counter = 1L;
    static HashMap<String, Long> string2LongMap = new HashMap<>();


    public static void main(String[] args) throws IOException {

        // For testing
        Long actedIn = 58L;
        Long created = 71L;
        Long produced = 91L;

        //
//        final String dataDir = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph";
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/Yoga2S/db";
//
//        File dataSet = new File(dataDir + "/yago2core.10kseedsSample.compressed.notypes.tsv");
//
//        BatchInserter inserter = BatchInserters.inserter(new File(neo4jFolder));
//
//        // Creating all the nodes and edges
//        String query = new String(Files.readAllBytes(Paths.get(dataSet.toURI())));
//        String[] lines = query.split("\n");
//        for (String line : lines) {
//
//            // Remove the dot at the end of the line. By observation it always a dot but we'll check.
//            if (line.lastIndexOf('.') == line.length()-1){
//                line = line.substring(0, line.length()-2);
//            }
//            String[] nodes = line.trim().split("[ \t]+");
//
//            String subject = nodes[0];
//            String predicate = nodes[1];
//            String object = nodes[2];
//
//            if (!string2LongMap.containsKey(subject)) {
//                string2LongMap.put(subject, counter);
//                inserter.createNode(counter, null);
//                ++counter;
//            }
//            if (!string2LongMap.containsKey(object)) {
//                string2LongMap.put(object, counter);
//                inserter.createNode(counter, null);
//                ++counter;
//            }
//
//            if (!string2LongMap.containsKey(predicate)) {
//                switch (predicate) {
//                    case "<actedIn>" -> actedIn = counter;
//                    case "<created>" -> created = counter;
//                    case "<produced>" -> produced = counter;
//                }
//                string2LongMap.put(predicate, counter);
//                inserter.createNode(counter, null);
//                ++counter;
//            }
//
//            inserter.createRelationship(str2Long(subject), str2Long(object),
//                    RelationshipType.withName(String.valueOf(str2Long(predicate))), null);
//        }
//
//        inserter.shutdown();

        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jFolder));
        ComputingMetrics testObject = new ComputingMetrics();

        // ?a <actedIn> ?b ?a <created> ?b => ?a <produced> ?b
        Atom aActedInB = new Atom(actedIn, 101L, 102L);
        Atom aCreatedB = new Atom(created, 101L, 102L);
        Atom aProducedB = new Atom(produced, 101L, 102L);
        Rule r1 = new Rule(aProducedB, Set.of(aActedInB, aCreatedB));
        testObject.computePCAConfidence(r1, gdb);
        gdb.shutdown();
    }

    static long str2Long(String node){
        return string2LongMap.get(node);
    }

}
