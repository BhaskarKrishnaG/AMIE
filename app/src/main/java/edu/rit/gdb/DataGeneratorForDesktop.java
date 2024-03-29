package edu.rit.gdb;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.Map;

public class DataGeneratorForDesktop {

    public static void main(String[] args) {
        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "WN11";
        final String graphDB = "/Users/bhaskarkrishnag/Downloads/Datasets/WN11/neodb";


        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(graphDB));
        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();
        Transaction tx = gdb.beginTx();

        ResourceIterable<Node> allNodes = gdb.getAllNodes();
        for (Node node: allNodes){
            session.run("CREATE (n) SET n.id = $id",
                    Map.of("id", node.getId()));
        }

        ResourceIterable<Relationship> allRelationships = gdb.getAllRelationships();
        int count = 0;
        for (Relationship rel: allRelationships){

            //TODO: YOU NEED TO ADD RELATIONSHIP NAME HERE!
            session.run("MATCH (s), (o)" +
                                " WHERE s.id = $sId AND o.id = $oId"+
                                " CREATE (s)-[:`"+rel.getType().name()+"` {entity: $entity}]->(o) ",
                        Map.of("sId", + rel.getStartNodeId(), "oId", rel.getEndNodeId(), "entity", rel.getProperty("entity")));

            count++;
            if (count%500==0)
                System.out.println(count);

        }

        session.close();

        tx.close();
        gdb.shutdown();
    }
}
