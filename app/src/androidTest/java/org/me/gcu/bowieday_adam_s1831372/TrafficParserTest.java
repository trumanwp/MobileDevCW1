package org.me.gcu.bowieday_adam_s1831372;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Parses the module's sample feeds (androidTest/assets) and checks the results
 * against values read directly from the xml files.
 * Runs on a device/emulator because PullParser is part of Android.
 */
@RunWith(AndroidJUnit4.class)
public class TrafficParserTest
{
    private String readAsset(String name) throws Exception
    {
        InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A"))
        {
            return s.next();
        }
    }

    @Test
    public void parsesUnplannedEvents() throws Exception
    {
        ArrayList<Situation> list = TrafficParser.parseData(readAsset("unplannedContentExample.xml"));
        assertEquals(4, list.size());

        Situation first = list.get(0);
        assertEquals("I_20260810_248_S", first.getId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", first.getRecordType());
        assertEquals("roadClosed", first.getSubType());
        assertTrue(first.getComment().startsWith("The A74M(S) junction 19"));
        assertEquals(0, first.getOperationalLanes());
        assertEquals("southBound", first.getDirection());
        assertEquals("2026-08-10T11:37:42+01:00", first.getStartTime().toString());

        assertEquals(2, first.getPoints().size());
        LocationPoint to = first.getPoints().get(0);
        assertEquals("to", to.getRole());
        assertEquals(55.065432, to.getLatitude(), 1e-9);
        assertEquals(-3.273135, to.getLongitude(), 1e-9);
        assertEquals("J19", to.getJunction());
        assertEquals("A74(M) J19 Eaglesfield", to.getDescription());
        assertEquals("[A74(M)]", first.getRoadNames().toString());

        // 2 ilc road names on each point
        assertEquals("[A90, B957, A933]", list.get(1).getRoadNames().toString());

        Situation queue = list.get(2);
        assertEquals("AbnormalTraffic", queue.getRecordType());
        assertEquals("queuingTraffic", queue.getSubType());
        assertEquals(2, queue.getLanesRestricted());
        assertEquals(-1, queue.getOperationalLanes());
    }

    @Test
    public void parsesRoadworks() throws Exception
    {
        ArrayList<Situation> list = TrafficParser.parseData(readAsset("roadworksContentExample.xml"));
        assertEquals(405, list.size());

        int withDelays = 0;
        for (Situation s : list)
        {
            assertTrue(s.isRoadworks());
            assertNotNull(s.getEndTime());
            assertEquals(2, s.getPoints().size());
            if (!s.getDelaysType().isEmpty())
            {
                withDelays++;
            }
        }
        assertEquals(157, withDelays);

        Situation first = list.get(0);
        assertEquals("D_159301_S", first.getId());
        assertEquals("roadworks", first.getSubType());
        assertEquals("longDelays", first.getDelaysType());
        assertEquals("2027-12-31T23:59Z", first.getEndTime().toString());
        assertEquals("[M8]", first.getRoadNames().toString());
        assertEquals("M8 (Jct 18 Off Slip to Entry A82)", first.getPoints().get(0).getDescription());
        // "Location - Work type" broken down at the last " - "
        assertEquals("M8 WB Woodside - A804 Phoenix Rd", first.getLocationText());
        assertEquals("Closure", first.getWorkType());
    }
} // End of class
