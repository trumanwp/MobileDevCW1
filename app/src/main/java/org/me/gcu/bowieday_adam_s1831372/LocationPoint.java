package org.me.gcu.bowieday_adam_s1831372;

import android.util.Log;

import java.util.ArrayList;

/**
 * One end ("from" or "to") of a situation's location, taken from a
 * <from> or <to> tag inside <tpegLinearLocation>.
 */
public class LocationPoint
{
    private String role;            // "from" or "to"
    private double latitude;
    private double longitude;
    private ArrayList<String> roadNames = new ArrayList<>(); // i.e. "A90", "B957" (ilc / linkName)
    private String junction = "";   // i.e. "J19" (junctionName)
    private String description = ""; // i.e. "A74(M) J19 Eaglesfield" (intersectionName / nonLinkedPointName)

    public LocationPoint(String role)
    {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * Stores the latitude, converting the text from the xml (i.e. "55.065432") to a double.
     * @param latitude  String containing the latitude text
     */
    public void setLatitude(String latitude) {
        try {
            this.latitude = Double.parseDouble(latitude.trim());
        } catch (NumberFormatException e) {
            Log.e("LocationPoint", "Error parsing latitude: " + latitude);
        }
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Stores the longitude, converting the text from the xml (i.e. "-3.273135") to a double.
     * @param longitude  String containing the longitude text
     */
    public void setLongitude(String longitude) {
        try {
            this.longitude = Double.parseDouble(longitude.trim());
        } catch (NumberFormatException e) {
            Log.e("LocationPoint", "Error parsing longitude: " + longitude);
        }
    }

    public ArrayList<String> getRoadNames() {
        return roadNames;
    }

    public void addRoadName(String roadName) {
        if (!roadNames.contains(roadName))
        {
            roadNames.add(roadName);
        }
    }

    public String getJunction() {
        return junction;
    }

    public void setJunction(String junction) {
        this.junction = junction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return role + "{" +
                "lat=" + latitude +
                ", lon=" + longitude +
                ", roads=" + roadNames +
                ", junction='" + junction + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
} // End of class
