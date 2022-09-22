package edu.rit.gdb.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class DatasetCreator {

    public static void main(String[] args) throws IOException {

        // Memorize entities
        Scanner sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/YAGO3-10/entity2id.txt"));
        // Skip the count line.
        sc.nextLine();
        HashMap<Integer, String> entity2Name = new HashMap<>();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] words = line.trim().split("\\s+");
            if (words.length != 2){
                System.out.println("Something went wrong in entities: " + Arrays.toString(words));
            }
            else {
                entity2Name.put(Integer.parseInt(words[1].trim()), words[0].trim());
            }
        }
        sc.close();

        // Memorize relation names.
        sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/YAGO3-10/relation2id.txt"));
        // Skip the count line.
        sc.nextLine();
        HashMap<Integer, String> relation2Name = new HashMap<>();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] words = line.trim().split("\\s+");
            if (words.length != 2){
                System.out.println("Something went wrong relations: " + Arrays.toString(words));
            }
            else {
                relation2Name.put(Integer.parseInt(words[1].trim()), words[0].trim());
            }
        }
        sc.close();


        // We will write everything to this.
        PrintWriter writer = new PrintWriter( Files.newBufferedWriter(Path.of("/Users/bhaskarkrishnag/Downloads/Datasets/YAGO3-10/output.tsv")));
        // Parse training triples.
        sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/NELL-995/train2id.txt"));
        // Skip the count line.
        sc.nextLine();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] words = line.trim().split("\\s+");
            if (words.length != 3){
                System.out.println("Something went wrong training: " + Arrays.toString(words));
            }
            else {
                writer.println(entity2Name.get(Integer.parseInt(words[0].trim())) + "\t"
                        + relation2Name.get(Integer.parseInt(words[2].trim())) + "\t"
                        + entity2Name.get(Integer.parseInt(words[1].trim())));
            }
        }
        System.out.println("Done writing training.");
        sc.close();

        // Parse validation triples.
        sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/NELL-995/valid2id.txt"));
        // Skip the count line.
        sc.nextLine();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] words = line.trim().split("\\s+");
            if (words.length != 3){
                System.out.println("Something went wrong validation: " + Arrays.toString(words));
            }
            else {
                writer.println(entity2Name.get(Integer.parseInt(words[0].trim())) + "\t"
                        + relation2Name.get(Integer.parseInt(words[2].trim())) + "\t"
                        + entity2Name.get(Integer.parseInt(words[1].trim())));
            }
        }
        System.out.println("Done writing testing.");
        sc.close();

        // Parse test triples.
        sc = new Scanner(new File("/Users/bhaskarkrishnag/Downloads/Datasets/NELL-995/test2id.txt"));
        // Skip the count line.
        sc.nextLine();
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] words = line.trim().split("\\s+");
            if (words.length != 3){
                System.out.println("Something went wrong testing: " + Arrays.toString(words));
            }
            else {
                writer.println(entity2Name.get(Integer.parseInt(words[0].trim())) + "\t"
                        + relation2Name.get(Integer.parseInt(words[2].trim())) + "\t"
                        + entity2Name.get(Integer.parseInt(words[1].trim())));
            }
        }
        System.out.println("Done writing testing.");
        sc.close();
        writer.close();
    }
}
