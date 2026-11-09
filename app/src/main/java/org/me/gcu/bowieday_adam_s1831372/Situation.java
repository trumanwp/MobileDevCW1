package org.me.gcu.bowieday_adam_s1831372;

import android.util.Log;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 * One <situation> from the Traffic Scotland DATEX II feeds:
 * an unplanned incident or a roadworks entry.
 */
public class Situation
{
    private String id;
    private String recordType = "";     // xsi:type: RoadOrCarriagewayOrLaneManagement, AbnormalTraffic, MaintenanceWorks
    private String subType = "";        // roadClosed, queuingTraffic, roadworks, ...
    private String comment = "";        // generalPublicComment
    private String locationText = "";   // roadworks comment broken down: part before the last " - "
    private String workType = "";       // roadworks comment broken down: part after the last " - " (i.e. "TTL")
    private String probability = "";
    private String validityStatus = "";
    private String complianceOption = "";
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;     // roadworks only
    private int operationalLanes = -1;  // -1 when not in the xml
    private int lanesRestricted = -1;
    private String delaysType = "";     // delays, longDelays, veryLongDelays
    private boolean urgentRoadworks;
    private String direction = "";      // northBound, eastBound, ...
    private ArrayList<LocationPoint> points = new ArrayList<>();

    public Situation(String id)
    {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isRoadworks() {
        return recordType.equalsIgnoreCase("MaintenanceWorks");
    }

    public String getRecordType() {
        return recordType;
    }

    /**
     * Readable type for the list, i.e. "Road closed", "Queuing traffic" or "Roadworks - TTL".
     */
    public String getDisplayType() {
        if (isRoadworks())
        {
            return workType.isEmpty() ? "Roadworks" : "Roadworks - " + workType;
        }
        return splitCamelCase(subType.isEmpty() ? recordType : subType);
    }

    // "roadClosed" -> "Road closed"
    private static String splitCamelCase(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray())
        {
            if (result.length() == 0)
            {
                result.append(Character.toUpperCase(c));
            }
            else if (Character.isUpperCase(c))
            {
                result.append(' ').append(Character.toLowerCase(c));
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getComment() {
        return comment;
    }

    /**
     * Stores the generalPublicComment text.
     * Roadworks comments follow the pattern "Location - Work type",
     * i.e. "A82 Onich to Corrychurrachan SB - TTL", so they are broken down here.
     * @param comment  String with the comment text
     */
    public void setComment(String comment) {
        this.comment = comment.trim();
        int split = this.comment.lastIndexOf(" - ");
        if (isRoadworks() && split > 0)
        {
            locationText = this.comment.substring(0, split).trim();
            workType = this.comment.substring(split + 3).trim();
        }
        else
        {
            locationText = this.comment;
        }
    }

    public String getLocationText() {
        return locationText;
    }

    public String getWorkType() {
        return workType;
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability;
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus;
    }

    public String getComplianceOption() {
        return complianceOption;
    }

    public void setComplianceOption(String complianceOption) {
        this.complianceOption = complianceOption;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = parseTime(creationTime);
    }

    public OffsetDateTime getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(String versionTime) {
        this.versionTime = parseTime(versionTime);
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = parseTime(startTime);
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = parseTime(endTime);
    }

    public int getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(String operationalLanes) {
        this.operationalLanes = parseInt(operationalLanes);
    }

    public int getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(String lanesRestricted) {
        this.lanesRestricted = parseInt(lanesRestricted);
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public boolean isUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(String urgentRoadworks) {
        this.urgentRoadworks = Boolean.parseBoolean(urgentRoadworks.trim());
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public ArrayList<LocationPoint> getPoints() {
        return points;
    }

    public void addPoint(LocationPoint point) {
        points.add(point);
    }

    /**
     * All road names mentioned by this situation's location points, without repeats.
     */
    public ArrayList<String> getRoadNames() {
        ArrayList<String> roads = new ArrayList<>();
        for (LocationPoint p : points)
        {
            for (String road : p.getRoadNames())
            {
                if (!roads.contains(road))
                {
                    roads.add(road);
                }
            }
        }
        return roads;
    }

    // Times in the feed look like "2026-08-10T11:38:10+01:00"
    private OffsetDateTime parseTime(String text) {
        try {
            return OffsetDateTime.parse(text.trim());
        } catch (DateTimeParseException e) {
            Log.e("Situation", "Error parsing time: " + text);
            return null;
        }
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            Log.e("Situation", "Error parsing number: " + text);
            return -1;
        }
    }

    @Override
    public String toString() {
        return "Situation{" +
                "id='" + id + '\'' +
                ", type='" + recordType + '/' + subType + '\'' +
                ", comment='" + comment + '\'' +
                ", workType='" + workType + '\'' +
                ", start=" + startTime +
                ", end=" + endTime +
                ", operationalLanes=" + operationalLanes +
                ", lanesRestricted=" + lanesRestricted +
                ", delays='" + delaysType + '\'' +
                ", direction='" + direction + '\'' +
                ", points=" + points +
                '}';
    }
} // End of class
