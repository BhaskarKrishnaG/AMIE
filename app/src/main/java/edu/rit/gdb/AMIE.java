package edu.rit.gdb;

import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

public class AMIE {

    static Queue<Rule> queue = new LinkedList<>();
    static Set<Rule> output = new HashSet<>();

    /**
     * This method will add all the transactions (Rules of size 1) to the queue which will later be extended.
     *
     * @param db database.
     */
    public void initializeQueue(GraphDatabaseService db){
        Transaction tx = db.beginTx();
        Result facts = db.execute("MATCH (s)-[p]->(o) return id(s) as sId, id(r) relId, id(o) as oId");

        while (facts.hasNext()){
            Map<String, Object> triple = facts.next();
            Rule newRule = new Rule();
            Atom newAtom = new Atom((Long)triple.get("relId"), (Long)triple.get("sId"), (Long)triple.get("rId"));
            newRule.setHeadAtom(newAtom);

            queue.add(newRule);
        }
        tx.close();
    }

    public static void main(String[] args) {
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";
        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jFolder));
    }
}
