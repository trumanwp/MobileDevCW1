package org.me.gcu.bowieday_adam_s1831372;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Parses the DATEX II xml String (unplanned events or roadworks) with PullParser,
 * building one Situation object per <situation> tag.
 * Unknown tags are just skipped, so new fields in the feed won't crash the app.
 */
public class TrafficParser
{
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    public static ArrayList<Situation> parseData(String dataToParse)
    {
        ArrayList<Situation> allSituations = new ArrayList<>();
        Situation aSituation = null;
        LocationPoint aPoint = null;        // the <from> or <to> being parsed, null when outside them
        boolean insideComment = false;      // flag: inside <generalPublicComment>
        String lastValue = "";              // last <value> text; its meaning comes in the following descriptor type tag
        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput( new StringReader( dataToParse ) );
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if (eventType == XmlPullParser.START_TAG) // Found a start tag
                {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("situation"))
                    {
                        aSituation = new Situation(xpp.getAttributeValue(null, "id"));
                        Log.d("MyTag", "New situation found: " + aSituation.getId());
                    }
                    else if (aSituation == null)
                    {
                        // Header info before the first situation: not needed
                    }
                    else if (tag.equalsIgnoreCase("situationRecord"))
                    {
                        aSituation.setRecordType(xpp.getAttributeValue(XSI, "type"));
                    }
                    else if (tag.equalsIgnoreCase("situationRecordCreationTime"))
                    {
                        aSituation.setCreationTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("situationRecordVersionTime"))
                    {
                        aSituation.setVersionTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("probabilityOfOccurrence"))
                    {
                        aSituation.setProbability(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("validityStatus"))
                    {
                        aSituation.setValidityStatus(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("overallStartTime"))
                    {
                        aSituation.setStartTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("overallEndTime"))
                    {
                        aSituation.setEndTime(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("numberOfOperationalLanes"))
                    {
                        aSituation.setOperationalLanes(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("numberOfLanesRestricted"))
                    {
                        aSituation.setLanesRestricted(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("delaysType"))
                    {
                        aSituation.setDelaysType(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("urgentRoadworks"))
                    {
                        aSituation.setUrgentRoadworks(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("complianceOption"))
                    {
                        aSituation.setComplianceOption(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                            || tag.equalsIgnoreCase("abnormalTrafficType")
                            || tag.equalsIgnoreCase("roadMaintenanceType"))
                    {
                        aSituation.setSubType(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("generalPublicComment"))
                    {
                        insideComment = true;
                    }
                    else if (tag.equalsIgnoreCase("tpegDirection"))
                    {
                        aSituation.setDirection(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to"))
                    {
                        aPoint = new LocationPoint(tag);
                    }
                    else if (tag.equalsIgnoreCase("latitude") && aPoint != null)
                    {
                        aPoint.setLatitude(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("longitude") && aPoint != null)
                    {
                        aPoint.setLongitude(xpp.nextText());
                    }
                    else if (tag.equalsIgnoreCase("value"))
                    {
                        String temp = xpp.nextText().trim();
                        if (insideComment)
                        {
                            aSituation.setComment(temp);
                        }
                        else
                        {
                            lastValue = temp;
                        }
                    }
                    // The descriptor type tags come after their <value>, telling us what it was
                    else if (tag.equalsIgnoreCase("tpegIlcPointDescriptorType") && aPoint != null)
                    {
                        xpp.nextText();
                        aPoint.addRoadName(lastValue); // i.e. "A90"
                    }
                    else if (tag.equalsIgnoreCase("tpegJunctionPointDescriptorType") && aPoint != null)
                    {
                        xpp.nextText();
                        aPoint.setJunction(lastValue); // i.e. "J19"
                    }
                    else if (tag.equalsIgnoreCase("tpegOtherPointDescriptorType") && aPoint != null)
                    {
                        String descriptorType = xpp.nextText();
                        if (descriptorType.equalsIgnoreCase("linkName"))
                        {
                            aPoint.addRoadName(lastValue); // roadworks give the road name this way, i.e. "M8"
                        }
                        else
                        {
                            aPoint.setDescription(lastValue); // intersectionName / nonLinkedPointName
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) // Found an end tag
                {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("generalPublicComment"))
                    {
                        insideComment = false;
                    }
                    else if ((tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to")) && aPoint != null)
                    {
                        aSituation.addPoint(aPoint);
                        aPoint = null;
                    }
                    else if (tag.equalsIgnoreCase("situation") && aSituation != null)
                    {
                        allSituations.add(aSituation); // add to collection
                        Log.d("MyTag", "Situation parsing completed: " + aSituation);
                        aSituation = null;
                    }
                }
                eventType = xpp.next(); // Get the next event before looping again
            } // End of while
        }
        catch (XmlPullParserException ae1)
        {
            Log.e("MyTag", "Parsing error " + ae1);
        }
        catch (IOException ae1)
        {
            Log.e("MyTag", "IO error during parsing");
        }

        Log.d("MyTag", "End of document reached, " + allSituations.size() + " situations");
        return allSituations;
    } // End of parseData

} // End of class
