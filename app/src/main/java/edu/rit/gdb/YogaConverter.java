package edu.rit.gdb;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class YogaConverter {
    static Long counter = 1L;
    static HashMap<String, Long> string2LongMap = new HashMap<>();


    public static void main(String[] args) throws IOException {

        final String dataDir = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph";
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/Yago2S/db";

        File dataSet = new File(dataDir + "/yago2core.10kseedsSample.compressed.notypes.tsv");

        BatchInserter inserter = BatchInserters.inserter(new File(neo4jFolder));

        // Creating all the nodes and edges
        String query = new String(Files.readAllBytes(Paths.get(dataSet.toURI())));
        String[] lines = query.split("\n");

        int progress = 0;
        for (String line : lines) {

            // Remove the dot at the end of the line. By observation line always has a dot but we'll check.
            if (line.lastIndexOf('.') == line.length()-1){
                line = line.substring(0, line.length()-1);
            }
            String[] nodes = line.trim().split("\\s+");

            String subject = nodes[0];
            String predicate = nodes[1].trim();
            String object = nodes[2];

            if (!string2LongMap.containsKey(subject)) {
                string2LongMap.put(subject, counter);
                inserter.createNode(counter, Map.of("entity", subject));
                ++counter;
            }
            if (!string2LongMap.containsKey(object)) {
                string2LongMap.put(object, counter);
                inserter.createNode(counter, Map.of("entity", object));
                ++counter;
            }

            if (!string2LongMap.containsKey(predicate)) {
                string2LongMap.put(predicate, counter);
                ++counter;
            }

            inserter.createRelationship(str2Long(subject), str2Long(object),
                    RelationshipType.withName(String.valueOf(str2Long(predicate))), Map.of("entity", predicate));

            progress++;

            if (progress%500 == 0)
                System.out.println(progress);
        }

        inserter.shutdown();

    }

    static long str2Long(String node){
        return string2LongMap.get(node);
    }

}
