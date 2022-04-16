package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class getLines {

    public static void main(String[] args) throws FileNotFoundException {
        Scanner file = new Scanner(new File("/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/yago2core.10kseedsSample.compressed.notypes.tsv"));
        while (file.hasNextLine()){
            String line = file.nextLine();
            if (line.contains("<isLocatedIn>"))
                System.out.println(line);
        }
        file.close();
    }
}
