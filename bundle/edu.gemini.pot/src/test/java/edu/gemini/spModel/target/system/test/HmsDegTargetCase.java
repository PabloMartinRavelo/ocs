/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: HmsDegTargetCase.java 18053 2009-02-20 20:16:23Z swalker $
 */
package edu.gemini.spModel.target.system.test;

import edu.gemini.spModel.target.system.CoordinateTypes.Epoch;
import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.target.system.HmsDegTarget;
import edu.gemini.spModel.target.system.HMS;
import edu.gemini.spModel.target.system.ICoordinate;
import edu.gemini.spModel.target.system.DMS;
import static edu.gemini.spModel.test.TestFile.ser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class SPTargetTest tests classes related to SPTarget.
 */
public final class HmsDegTargetCase {
    ITarget _t1;
    ITarget _t2;

    @Before
    public void setUp() throws Exception {
        _t1 = new HmsDegTarget();
        _t2 = new HmsDegTarget();
    }

    // Create targets of various types
    @Test
    public void testSimple() {
        HmsDegTarget t1 = new HmsDegTarget();
        assertNotNull(t1);

        ICoordinate ra = new HMS("10:11:12.345");
        ICoordinate dec = new DMS("-20:30:40.567");
        t1.setC1C2(ra, dec);

        assertEquals(t1.raToString(), "10:11:12.345");
        assertEquals(t1.decToString(), "-20:30:40.57");
    }

    private void _doTestOne(String raIn, String decIn,
                            String raEx, String decEx) {
        _t1.setC1(raIn);
        _t1.setC2(decIn);
        String raOut = _t1.c1ToString();
        String decOut = _t1.c2ToString();

        assertEquals("Failed comparison,", raEx, raOut);
        assertEquals("Failed comparison,", decEx, decOut);
    }

    @Test
    public void testWithStrings() {
        _doTestOne("0:0:0", "-0:0:0", "00:00:00.000", "00:00:00.00");
        _doTestOne("12:13:14.5", "32:33:34.0", "12:13:14.500", "32:33:34.00");
        _doTestOne("22:13:0", "-2:33:34.0", "22:13:00.000", "-02:33:34.00");
    }

    @Test
    public void testSerialization() throws Exception {
        final HmsDegTarget outObject = new HmsDegTarget();
        outObject.setRaDec("10:11:12.34", "-11:12:13.4");
        final HmsDegTarget inObject = ser(outObject);
        assertTrue(outObject.equals(inObject));
    }
}
