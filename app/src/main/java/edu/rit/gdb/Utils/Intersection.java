package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Intersection {

    public static void main(String[] args) throws FileNotFoundException {
//        Scanner f1 = new Scanner(new File("/Users/bhaskarkrishnag/Desktop/hasCapital.txt"));
//        Scanner f2 = new Scanner(new File("/Users/bhaskarkrishnag/Desktop/isCitizenOf.txt"));
//        HashMap<String, String> aLocatedInB = new HashMap<>();
//        HashMap<String, String> hHasCapitalB = new HashMap<>();
//
//        //?h <hasCapital> ?b ?a <isCitizenOf> ?h => ?a <isLocatedIn> ?b
//        while (f1.hasNextLine()){
//            String[] line = f1.nextLine().split("\\s+");
//            hHasCapitalB.put(line[0], line[2]);
//        }
//        f1.close();
//
//        while (f2.hasNextLine()){
//            String line = f2.nextLine().trim();
//            if (line.lastIndexOf('.') == line.length()-1){
//                line = line.substring(0, line.length()-1);
//            }
//
//            String[] words = line.split("\\s+");
//
//            if (hHasCapitalB.containsKey(words[2]))
//                aLocatedInB.put(words[0], hHasCapitalB.get(words[2]));
//
//        }
//        f2.close();
//
//        for (String k: aLocatedInB.keySet()){
//            System.out.println(k + "\t" + aLocatedInB.get(k));
//        }

        int count = 0;
        Scanner f1 = new Scanner(new File("/Users/bhaskarkrishnag/Desktop/aLivesInB"));
        HashMap<String, String> aLocatedInB = new HashMap<>();
        while (f1.hasNextLine()){
            String line = f1.nextLine().trim();
            if (line.lastIndexOf('.') == line.length()-1){
                line = line.substring(0, line.length()-1);
            }

            String[] words = line.split("\\s+");
            aLocatedInB.put(words[0], words[1]);
        }
        f1.close();
        Scanner f2 = new Scanner(new File("/Users/bhaskarkrishnag/Desktop/GT.txt"));
        while (f2.hasNextLine()){
            String line = f2.nextLine().trim();
            if (line.lastIndexOf('.') == line.length()-1){
                line = line.substring(0, line.length()-1);
            }

            String[] words = line.split("\\s+");
            if (aLocatedInB.containsKey(words[0])){
                System.out.println("\nCalculated: " + words[0] +"\t" + aLocatedInB.get(words[0]));
                System.out.println("GT : " + line);

                if (words[2].equals(aLocatedInB.get(words[0])))
                    count++;
            }
        }
        System.out.println(count);
        f2.close();
    }
}
