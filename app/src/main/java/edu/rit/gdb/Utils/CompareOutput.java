package edu.rit.gdb.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CompareOutput {

    final static Map<String, Long> char2Long = Map.of("?a", 0L, "?b", 1L, "?g", 100L, "?h", 100L);
    static Rule[]  rules = new Rule[0];
    static Double[] pcas = new Double[0];

    public static Rule createRule(String r){
        Rule newRule = new Rule();
        Atom headAtom = new Atom();
        String[] atoms = r.split("=>");
        String hAtom = atoms[1].trim();
        String[] hAtoms = hAtom.split("\\s+");
        for (String h: hAtoms){
            if (h.trim().equals("?a")){
                headAtom.setSubject(0L);
            }
            else if (h.trim().equals("?b")){
                headAtom.setObject(1L);
            }
            else {
                headAtom.setRelationshipName(h.trim());
            }
        }
        newRule.setHeadAtom(headAtom);

        String[] bAtoms = atoms[0].trim().split("\\s+");
        Atom bodyAtoms = new Atom();
        List<Atom> bodyAtom = new ArrayList<>();
        int counter = 1;
        for (String b: bAtoms){
            if (counter==1){
                bodyAtoms.setSubject(char2Long.get(b.trim()));
            }
            else if (counter == 2){
                bodyAtoms.setRelationshipName(b.trim());
                bodyAtoms.setPredicateId(999L);
            }
            else {
                counter = 0;
                bodyAtoms.setObject(char2Long.get(b.trim()));
                bodyAtom.add(bodyAtoms.deepCopyAtom());
                bodyAtoms = new Atom();
            }
            counter++;
        }

        newRule.setBodyAtoms(bodyAtom);

        return newRule;
    }

    public static void parseAMIE() throws FileNotFoundException {
        Scanner file = new Scanner(new File("/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/RoyalGraph.output"));
//        Scanner file = new Scanner(new File("/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/YagoOutput.txt"));
        int counter = -1;
        while (file.hasNextLine()){
            String line = file.nextLine();
            if (counter == -1){
                rules = new Rule[Integer.parseInt(line)];
                pcas = new Double[Integer.parseInt(line)];
            }
            else {
                if (counter == 59)
                    System.out.println("");
                String[] words = line.split("\\s+");
                double pca = Double.parseDouble(words[words.length - 5]);
                // 8 till 0
                StringBuilder r = new StringBuilder();
                for (int i = words.length - 8; i >= 0; i--) {
                    r.insert(0, " ");
                    r.insert(0, words[i]);
                }
                rules[counter] = createRule(r.toString().trim());
                pcas[counter] = pca;
            }
            counter++;
        }

        file.close();
    }

    public static void compareResults(List<Rule> output) throws FileNotFoundException {
        parseAMIE();
        boolean found = false;
        List<Rule> minedExtra = new ArrayList<>();
        int foundCounter = 0;

        for (Rule r: output){
            for (Rule rule : rules) {
                if (rule.getLength() == r.getLength()) {
                    if (r.getLength() == 2) {
                        found = compareAtom(rule.getHeadAtom(), r.getHeadAtom())
                                && compareAtom(rule.getBodyAtoms().get(0), r.getBodyAtoms().get(0));

                    } else {
                        found = compareAtom(rule.getHeadAtom(), r.getHeadAtom())
                                && ((compareAtom(rule.getBodyAtoms().get(0), r.getBodyAtoms().get(0))
                                && compareAtom(rule.getBodyAtoms().get(1), r.getBodyAtoms().get(1)))
                                || (compareAtom(rule.getBodyAtoms().get(0), r.getBodyAtoms().get(1)))
                                && compareAtom(rule.getBodyAtoms().get(1), r.getBodyAtoms().get(0)));
                    }
                    if (found) {
//                        System.out.println("\nMatched");
//                        System.out.println(r + "\t" + r.getConfPCA());
//                        System.out.println(rule + "\t" + rule.getConfPCA());
                        foundCounter++;
                        break;
                    }
                }
            }
            if (!found){
                minedExtra.add(r);
            }
        }

        System.out.println("Matched rules count: " + foundCounter);
        System.out.println("Additional rules minded: " + minedExtra.size());
        System.out.println(minedExtra.toString());
    }

    public static boolean compareAtom(Atom a, Atom b){
        if (a.getSubject() > 1)
            return b.getSubject()>1 && a.getObject().equals(b.getObject()) && a.relationshipName.equals(b.relationshipName);

        else if (a.getObject()>1)
            return b.getObject()>1 && a.getSubject().equals(b.getSubject()) && a.relationshipName.equals(b.relationshipName);

        else
            return a.getSubject().equals(b.getSubject()) && a.getObject().equals(b.getObject()) && a.relationshipName.equals(b.relationshipName);
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Rule> output = new ArrayList<>();
        Rule newRule = new Rule(new Atom(999L, 0L, 1L, "<isPoliticianOf>"),
                List.of(new Atom(999L, 0L, 1L, "<livesIn>")));
        newRule.setConfPCA(0.333333333);
        output.add(newRule);
        compareResults(output);

    }
}
