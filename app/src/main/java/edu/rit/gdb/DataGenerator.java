package edu.rit.gdb;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class DataGenerator {
    public static void main(String[] args) throws Exception {

        final String dataDir = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph";
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";

        File dataSet = new File(dataDir + "/allTriples.tsv");
        File entity2Id = new File(dataDir + "/entity2id.txt");
        File rel2Id = new File(dataDir + "/relation2id.txt");

        BatchInserter inserter = BatchInserters.inserter(new File(neo4jFolder));

        // Creating all the nodes and edges
        String query = new String(Files.readAllBytes(Paths.get(dataSet.toURI())));
        String[] lines = query.split("\n");
        for (String line : lines) {
            String[] nodes = line.trim().split("[ \t]+");

            long node1 = Long.parseLong(nodes[0]);
            String edge = nodes[1];
            long node2 = Long.parseLong(nodes[2]);

            if (!inserter.nodeExists(node1)) {
                inserter.createNode(node1, null);
            }
            if (!inserter.nodeExists(node2)) {
                inserter.createNode(node2, null);
            }

            inserter.createRelationship(node1, node2, RelationshipType.withName(edge), null);
        }

        // Add properties to the nodes.
        query = new String(Files.readAllBytes(Paths.get(entity2Id.toURI())));
        lines = query.split("\n");
        for (String line : lines) {
            String[] nodes = line.trim().split("\\s+");

            if (nodes.length == 2)
                inserter.setNodeProperties(Long.parseLong(nodes[1]), Map.of("entity", nodes[0]));
        }

        inserter.shutdown();

        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jFolder));
        Transaction tx = gdb.beginTx();

        query = new String(Files.readAllBytes(Paths.get(rel2Id.toURI())));
        lines = query.split("\n");
        for (String line : lines) {
            String[] nodes = line.trim().split("\\s+");
            if (nodes.length == 2) {
                Result res = gdb.execute("MATCH (vi)-[r]->(vj) WHERE TYPE(r) = $predId RETURN min(id(r)) as r", Map.of("predId", nodes[1]));
                long minId = ((Number)res.next().get("r")).longValue();
                gdb.getRelationshipById(minId).setProperty("predicate", nodes[0]);
            }
        }

        System.out.println("Loading complete...");
        tx.success();
        tx.close();

		/*
		 * Comment these lines to verify if the entered data is right
		 * The output format is node - edge - node
		 * entity property will be printed for nodes but edges will just be a Long. We need to traverse to min id edge
		 * to find the predicate property.
		 *
		 *
			tx = gdb.beginTx();
			ResourceIterable<Relationship> relationships = gdb.getAllRelationships();

			for(Relationship rel : relationships){
				System.out.println(rel.getStartNode().getProperty("entity") + " "
									+ rel.getType() + " "
									+ rel.getEndNode().getProperty("entity"));
			}
			tx.success();
			tx.close();
		*
		*
		*/
        gdb.shutdown();
    }
}
