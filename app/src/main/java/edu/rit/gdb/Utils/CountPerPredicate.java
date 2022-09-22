package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CountPerPredicate {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/WN11/output.tsv"));

        Map<String, Integer> map= new HashMap<>();

        while(sc.hasNext()){
            String line = sc.nextLine();
            String[] splits = line.trim().split("\\s+");

            if (!map.containsKey(splits[1])){
                map.put(splits[1], 0);
            }

            map.put(splits[1], map.get(splits[1])+1);
        }

        System.out.println(map);
    }
}
