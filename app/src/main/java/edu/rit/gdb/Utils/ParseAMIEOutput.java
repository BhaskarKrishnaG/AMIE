package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ParseAMIEOutput {

    public void sortMe(HashMap<String, Float> ruleToPCA){
        HashMap<String, Float> sorted = ruleToPCA.entrySet().stream()
                                        .sorted(Map.Entry.comparingByValue())
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                                (e1, e2) -> e1, LinkedHashMap::new));

        for (String r: sorted.keySet()){
            System.out.printf("%-100s %f\n",r , sorted.get(r));
        }

        System.out.println("Total: " + sorted.size());
    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner file = new Scanner(new File(args[0]));
        HashMap<String, Float> ruleToPCA = new HashMap<>();

        while (file.hasNextLine()){
            String line = file.nextLine().trim();
            String[] arrLine = line.split("\s+");
            float pca = Float.parseFloat(arrLine[arrLine.length - 5]);
            StringBuilder rule = new StringBuilder();
            // 8 till 0
            for (int i = arrLine.length - 8; i >= 0; i--) {
                rule.insert(0, " ");
                rule.insert(0, arrLine[i]);
            }
            ruleToPCA.put(rule.toString(), pca);
        }

        new ParseAMIEOutput().sortMe(ruleToPCA);

        file.close();
    }
}
