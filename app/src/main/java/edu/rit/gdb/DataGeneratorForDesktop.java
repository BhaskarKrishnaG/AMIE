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
        final String PASSWORD = "RFDB";
        final String graphDB = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";


        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(graphDB));
        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();
        Transaction tx = gdb.beginTx();

        ResourceIterable<Node> allNodes = gdb.getAllNodes();
        for (Node node: allNodes){
            session.run("CREATE (n) SET n.id = $id , n.entity = $entity",
                    Map.of("id", node.getId(), "entity", node.getProperty("entity")));
        }

        ResourceIterable<Relationship> allRelationships = gdb.getAllRelationships();

        for (Relationship rel: allRelationships){
            session.run("MATCH (s), (o)" +
                                " WHERE id(s) = $sId AND id(o) = $oId"+
                                " CREATE (s)-[:`"+rel.getType().name()+"`]->(o) ",
                        Map.of("sId", + rel.getStartNodeId(), "oId", rel.getEndNodeId()));

        }

        session.close();

        tx.close();
        gdb.shutdown();
    }
}
