package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PrintJoiningVariable {

    public static void main(String[] args) throws FileNotFoundException {

        String output = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/YagoOutput.txt";
        Scanner sc = new Scanner(new File(output));
        TreeMap<String, Set<String>> predicate2JoinVar = new TreeMap<>();

        // First line has the count of total rules mines. Skip the line.
        sc.nextLine();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] splitString = line.split("<");
            int splitStrLen = splitString.length - 1;
            String predicate = splitString[splitStrLen].substring(0, splitString[splitStrLen].indexOf(">"));
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

        System.out.println("Predicate\t\tJoining Variable");
        for (String predicate: predicate2JoinVar.keySet()){
            if (predicate2JoinVar.get(predicate).size() == 1)
                System.out.println(predicate + "\t\t" + predicate2JoinVar.get(predicate).toString());
        }
    }
}
