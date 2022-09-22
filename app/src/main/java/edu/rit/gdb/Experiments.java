package edu.rit.gdb;

import edu.rit.gdb.Utils.Atom;
import edu.rit.gdb.Utils.ComputingMetrics;
import edu.rit.gdb.Utils.Rule;
import org.neo4j.driver.*;

import java.util.List;
import java.util.Map;

public class Experiments {

    public static void main(String[] args) {
        final String URI = "bolt://localhost:7687";
        final String USER = "neo4j";
        final String PASSWORD = "WN11";

        Driver driver = GraphDatabase.driver( URI, AuthTokens.basic( USER, PASSWORD ) );
        Session session = driver.session();


//        // Rule: ?a  <isKnownFor>  ?b   => ?a  <isCitizenOf>  ?b  0.002320186  0.003759398  0.025  1  266  40  ?a
//        Atom isCitizenOf = new Atom(38L, 0L, 1L);
//        Atom isKnownFOr = new Atom(42L, 0L, 1L);
//        Rule r = new Rule(isCitizenOf, List.of(isKnownFOr));
//
//        // Rule: ?a  <produced>  ?b   => ?a  <actedIn>  ?b   0.012197601 0.097402597 0.431654676 60  616 139 ?b
        Atom aProducedB = new Atom(90L, 0L, 1L, "<produced>");
//        Atom aActedInB = new Atom(70L, 0L, 1L);
////        r = new Rule(aActedInB, List.of(aProducedB));
//
//        // ?g  <isMarriedTo>  ?a  ?g  <produced>  ?b   => ?a  <produced>  ?b   0.024350649 0.064655172 0.064655172 15  232 232 ?b
//        Atom gProducedB = new Atom(90L, 100L, 1L);
//        Atom gIsMarriedToa = new Atom(45L, 100L, 0L);
//        r = new Rule(aProducedB, List.of(gProducedB, gIsMarriedToa));
//
//
//        ?a  <produced>  ?b   => ?a  <created>  ?b   0.008674102 0.136363636 0.763636364 84  616 110 ?b
        Atom aCreatedB = new Atom(57L, 0L, 1L);
        Rule r = new Rule(aCreatedB, List.of(aProducedB));

        // For Nari, Rule 4 of AMIE (facts) output
        // ?a  2  ?h  ?b  2  ?h   => ?a  0  ?b
        Atom aBCb = new Atom(2L, 0L, 100L);
        Atom bBCH = new Atom(2L, 1L, 100L);
        Atom aPb = new Atom(0L, 0L, 1L);
        r = new Rule(aPb, List.of(aBCb, bBCH));

        // (1)-[2]->(0)] => (0)-[8]->(1)
        Atom one2zero = new Atom(2L, 1L, 0L);
        Atom zero8one = new Atom(8L, 0L, 1L);
        r = new Rule(zero8one, List.of(one2zero));

        ComputingMetrics utl = new ComputingMetrics();
        System.out.println("Head coverage: " + utl.getHeadCoverage(r, session));
        System.out.println("Support: " + utl.computeSupport(r, session));
        System.out.println("PCA: " + utl.computePCAConfidence(r, session, false));
    }
}
