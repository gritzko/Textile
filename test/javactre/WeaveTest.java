/*
 * (c) Victor Grishchenko 2010
 */

package javactre;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author victor
 */
public class WeaveTest {

    public WeaveTest() {
    }

    /**
     * Test of redundant2version method, of class Weave.
     */
    @Test
    public void testRedundant2version() {
        String ver2 = Weave.straighten_weft2("aad.bbd+d*ac");
        assertEquals("d.bbac",ver2);
    }


    @Test
    public void testVersion2filtre () {
        String weave3 = "Aa1Bb1Ca2Db1Ea*Fc@Gc]";
        String ver2 = "a1b2c[";
        String filtre = Weave.version2filtre(ver2);
        String re = "(.("+filtre+"))|.{3}";
        //System.err.println("re: "+re);
        String text3 = weave3.replaceAll(re,"$1");
        assertEquals("Aa1Bb1Db1Ea*Fc@",text3);
    }


    @Test
    public void testAwarenessClosure () {
        Weave weave = new Weave("Test");
        weave.loadWeave5cFromString
         ("ॐ\0\0\0\0A\0\0a1Ga1b2Hb2c1Ba1a2Ca2a3Fa3b1Da3a4Ea4a5Ia5a]۝\0\0\0\1");
        String aware = weave.getAwareness("b1");
        assertEquals("a3b1",aware);
        String closure = weave.getAwarenessClosure("b1");
        assertEquals("a3b1",closure);
        closure = weave.getAwarenessClosure("b2");
        assertEquals("a3b2",closure);
        closure = weave.getAwarenessClosure("c1");
        assertEquals("a3b2c1",closure);
        closure = weave.getAwarenessClosure("a[");
        assertEquals("a[",closure);
    }
    

    @Test
    public void testGetText () {
        Weave weave = new Weave("Test");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0T\0\0a1Ea1a2Xa2b2Sa2a3\ba3b1Ta3a4۝\0\0\0\1\0a4b3");
        String text = weave.getText1();
        assertEquals("TEXT",text);
        text = weave.getText1ByWeft2("a4");
        assertEquals("TEST",text);
        text = weave.getText1ByWeft2("a4b1");
        assertEquals("TET",text);
    }


    @Test
    public void testAddPatch5c () {
        Weave weave = new Weave("Test");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0T\0\0a1Ea1a2Sa2a3Ta3a4\0a4b3۝\0\0\0\1");
        assertEquals("TEST",weave.getText1());
        weave.addPatch5c("\ba3b1");
        assertEquals("TET",weave.getText1());
        weave.addPatch5c("Xa2b2");
        assertEquals("TEXT",weave.getText1());
        assertEquals("TET",weave.getText1ByWeft2("a4b1"));
    }


    @Test
    public void testInsertMarkSegment () {
        Weave weave = new Weave("Test");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0T\0\0a1Ea1a2Xa2b2Sa2a3\ba3b1Ta3a4۝\0\0\0\1");
        weave.addPatch5c("\0a4c1");
        String text = weave.getText1ByWeft2("c1");
        assertEquals("TEXT",text);
    }


    @Test
    public void testAwareSiblings () {
        Weave weave = new Weave("Test");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0T\0\0a1Ea1a2Sa2a3Ta3a4۝\0\0\0\1");
        weave.addPatch5c("\0a4c1Ka2c2");
        assertEquals("TEKST",weave.getText1());
    }


    @Test
    public void testUnawareSiblings () {
        Weave weave = new Weave("Test");
        weave.loadSourcesFromString("Alice\nBob\nCarol\n");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0<\0\0a1>a1a2۝\0\0\0\1");
        weave.addPatch5c("\0a2b1Aa1b2");
        weave.addPatch5c("Ba1c1");
        assertEquals("<AB>",weave.getText1());
    }


    @Test
    public void testUnawareUndeletion () {
        Weave weave = new Weave("Test");
        weave.loadSourcesFromString("Alice\nBob\nCarol\n");
        weave.loadWeave5cFromString
                ("ॐ\0\0\0\0A\0\0a1Ca1a2۝\0\0\0\1");
        weave.addPatch5c("\ba1b1");
        weave.addPatch5c("Ba1c1\ba1c2");
        weave.addPatch5c("\7a1b2");
        assertEquals("BC",weave.getText1());
        weave.addPatch5c("\0c2c3\7a1b4");
        assertEquals("ABC",weave.getText1());
    }


    @Test
    public void testSetText () {
        Weave weave = new Weave("Test");
        weave.loadSourcesFromString("Alice\nBob\nCarol\n");
        weave.setNewText("TEST","Alice");
        weave.setNewText("TET","Bob");
        weave.setNewText("TEXT","Carol");
        assertEquals(
                "ॐ\0\0\0\0T\0\0a1Ea1a2Xa2c1Sa2a3\ba3b1Ta3a4۝\0\0\0\1",
                weave.getWeave5c()
                );
        weave.markWeft("Bob");
        assertEquals(
                "ॐ\0\0\0\0T\0\0a1Ea1a2Xa2c1Sa2a3\ba3b1Ta3a4۝\0\0\0\1\0a4b2\0c1b2",
                weave.getWeave5c()
                );
}


}
