import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.Set;

public class TestComputingMetrics {

    // Adam = 0; Bob = 1; Carl = 2
    // Paris = 100; Rome = 101; Zurich = 102
    // livesIn = 1000; wasBornIn = 1001
    static Atom adamLivesInParis = new Atom(1000L, 0L, 100L);
    static Atom adamLivesInRome = new Atom(1000L, 0L, 101L);
    static Atom bobLivesInZurich = new Atom(1000L, 1L, 102L);
    static Atom adamWasBornInInParis = new Atom(1001L, 0L, 100L);
    static Atom carlWasBornInInRome = new Atom(1001L, 2L, 101L);

    // R: livesIn(x, y) => wasBornIn(x, y)
    static Rule r1 = new Rule(adamWasBornInInParis, Set.of(adamLivesInParis));

    // R: isMarriedTo(x,z) ∧ hasChild(z,y) => hasChild(x,y)
    static Atom a1 = new Atom(10L, 0L, 1L);
    static Atom a2 = new Atom(11L, 1L, 2L);
    static Atom a3 = new Atom(11L, 0L, 2L);

    static Rule r2 = new Rule(a3, Set.of(a1, a2));

    // Verifying with AMIE
    // ?b  1  ?a   => ?a  1  ?b
    static Atom bSpouseA = new Atom(1L, 100L, 101L);
    static Atom aSpouseB = new Atom(1L, 101L, 100L);
    static Rule amie1 = new Rule(aSpouseB, Set.of(bSpouseA));

    // ?h  0  ?b  ?a  1  ?h   => ?a  0  ?b
    static Atom hParentB = new Atom(0L, 103L, 100L);
    static Atom aParentB = new Atom(0L, 101L, 100L);
    static Atom aSpouseH = new Atom(1L, 101L, 103L);
    static Rule amie2 = new Rule(aParentB, Set.of(hParentB, aSpouseH));

    // ?g  0  ?b  ?g  1  ?a   => ?a  0  ?b
    static Atom gParentB = new Atom(0L, 104L, 100L);
    static Atom gSpouseA = new Atom(1L, 104L, 101L);
    static Rule amie3 = new Rule(aParentB, Set.of(gParentB, gSpouseA));

    // ?a  2  ?h  ?b  2  ?h   => ?a  0  ?b
    static Atom aBCH = new Atom(2L, 101L, 103L);
    static Atom bBCH = new Atom(2L, 100L, 103L);
    static Rule amie4 = new Rule(aParentB, Set.of(aBCH, bBCH));

    // ?a  0  ?h  ?b  0  ?h   => ?a  1  ?b
    static Atom aParentH = new Atom(0L, 101L, 103L);
    static Atom bParentH = new Atom(0L, 100L, 103L);
    static Rule amie5 = new Rule(aSpouseB, Set.of(aParentH, bParentH));

    // ?a  0  ?h  ?h  2  ?b   => ?a  2  ?b
    static Atom hBornInB = new Atom(2L, 103L, 100L);
    static Atom aBornInB = new Atom(2L, 101L, 100L);
    static Rule amie6 = new Rule(aBornInB, Set.of(aParentH, hBornInB));

    // ?g  0  ?a  ?g  2  ?b   => ?a  2  ?b
    static Atom gParentA = new Atom(0L, 104L, 101L);
    static Atom gBCB = new Atom(2L, 104L, 100L);
    static Rule amie7 = new Rule(aBornInB, Set.of(gParentA, gBCB));

    public static void main(String[] args) {
        final String neo4jFolder = "/Users/bhaskarkrishnag/IdeaProjects/AMIE/RoyalsGraph/db";
        GraphDatabaseService gdb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jFolder));

        ComputingMetrics testObject = new ComputingMetrics();

//        testObject.computeSupport(r1, gdb);
//        testObject.computeSupport(r2, gdb);
//        testObject.computePCAConfidence(r2, gdb);
//        testObject.computePCAConfidenceFixObject(r2, gdb);

//        testObject.getHeadCoverage(amie1, gdb);
        testObject.computePCAConfidence(amie1, gdb);

//        testObject.getHeadCoverage(amie2, gdb);
        testObject.computePCAConfidence(amie2, gdb);

//        testObject.getHeadCoverage(amie3, gdb);
        testObject.computePCAConfidence(amie3, gdb);

//        testObject.getHeadCoverage(amie4, gdb);
        testObject.computePCAConfidence(amie4, gdb);

//        testObject.getHeadCoverage(amie5, gdb);
        testObject.computePCAConfidence(amie5, gdb);
        testObject.computePCAConfidence(amie6, gdb);
        testObject.computePCAConfidence(amie7, gdb);
    }
}
