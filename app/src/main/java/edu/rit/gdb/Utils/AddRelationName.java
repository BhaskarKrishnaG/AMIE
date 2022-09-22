package edu.rit.gdb.Utils;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class AddRelationName {

    public static void main(String[] args) throws FileNotFoundException {
        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "WN11";

        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ));
        Session session = driver.session();

        Scanner sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/WN11/relation2id.txt"));
        HashMap<String, String> type2Name = new HashMap<>();

        // Skip the first line. It WILL? be the count.
        sc.nextLine();

        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] possibleName = line.trim().split("\\s+");

            // type2Name.put(0, <actorOf>)
            type2Name.put(possibleName[1], possibleName[0]);
        }

        session.close();

    }
}
