package edu.rit.gdb.Utils;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * This class was written to check the functionality of predicates
 *
 * We will iterate through all the predicates in the database and compute the functionality
 * fun(r) := #x : ∃y : r(x,y)
 *           ---------------
 *          #(x, y) : r(x, y)
 */
public class CheckFunctionality {

    public static void main(String[] args) throws FileNotFoundException {

        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "YAGO";

        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();
        Map<String, Integer> supportOfPreds = new HashMap<>();
        TreeMap<String, String> namesOfPreds = new TreeMap<>();

        Result res = session.run("MATCH ()-[r]->() return DISTINCT type(r) as predicate, count(*) as supp, r.entity as name");
        while (res.hasNext()){
            Record row = res.next();
            supportOfPreds.put(row.get("predicate").asString(), row.get("supp").asInt());
            namesOfPreds.put(row.get("name").asString(), row.get("predicate").asString());
        }

        String output = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/YagoOutput.txt";
//        String output = "/Users/bhaskarkrishnag/Downloads/Datasets/WN11/AMIE_ALL.output";
        Scanner sc = new Scanner(new File(output));
        TreeMap<String, Set<String>> predicate2JoinVar = new TreeMap<>();

        // First line has the count of total rules mines. Skip the line.
        sc.nextLine();
        while (sc.hasNextLine()){
            String line = sc.nextLine();

            // Let's break the rule to extract the head predicate.
            String[] splitString = line.split("=>");
            int headSection = splitString.length - 1;
            String[] possiblePredicate = splitString[headSection].trim().split(" ");
            String predicate = possiblePredicate[2];
            String joiningVar = line.substring(line.length()-2);

            if (predicate2JoinVar.containsKey(predicate)){
                predicate2JoinVar.get(predicate).add(joiningVar);
            }
            else {
                Set<String> newSet = new HashSet<>();
                newSet.add(joiningVar);
                predicate2JoinVar.put(predicate, newSet);
            }
        }

        System.out.printf("%40s\t%s\t%s\t%s\n","Predicate", "Functionality", "Reverse-Functionality", "AMIE-Functional-variable");

        // ASK: Should the direction matter here? When checking for reverse functional should we be looking for
        // (s)-[r]<-(o) ??
        for (String name: predicate2JoinVar.keySet()){
            Record row = session.run("  MATCH (s)-[r]->(o) " +
                                            " WHERE type(r) = $predicate " +
                                            " RETURN count(DISTINCT s) as unqSub, count(DISTINCT o) as unqObj",
                    Map.of("predicate", namesOfPreds.get(name))).next();

            System.out.printf( "%40s\t%f\t%f\t%s\n" ,name, row.get("unqSub").asInt()*(1.0)/supportOfPreds.get(namesOfPreds.get(name))
                    , row.get("unqObj").asInt()*(1.0)/supportOfPreds.get(namesOfPreds.get(name))
                    , predicate2JoinVar.get(name));
        }
    }
}
