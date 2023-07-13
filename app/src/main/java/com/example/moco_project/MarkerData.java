package com.example.moco_project;


import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Class that stores mushroom informations which is shared between the MapActivity and ArActivity.
 * This class is used to hold the information required for both Map and AR side manipulation of markers.
 */
public class MarkerData {

    // Mushroom Marker
    private Marker marker;

    // Circle around mushroom
    private Circle circle;

    // markerOptions of a mushroom Marker
    private MarkerOptions markerOption;

    // stores the value if user is inside zone. At beginning always false
    private boolean isUserInsideZone = false;

    // Marker ID
    private String id;


    /** Constructor for MarkerData*/
    public MarkerData(Marker marker, Circle circle, MarkerOptions markerOption) {
        this.marker = marker;
        this.circle = circle;
        this.markerOption = markerOption;
        this.id = marker.getTitle();
    }


    /** returns a mushroom marker*/
    public Marker getMarker() {
        return marker;
    }

    /** returns the circle of a mushroom*/
    public Circle getCircle() {
        return circle;
    }

    /**
     * returns MarkerOptions of a mushroom. This is still required for anchoring because ARCOre is not able
     * to work with the Position of Marker directly.
     * @return markerOption of specific mushroom
     */
    public MarkerOptions getMarkerOption() {return markerOption;}

    /** checks if the User is inside the marker circle
     * true -> is inside; false -> is not inside
     */
    public boolean isUserInsideZone() {
        return isUserInsideZone;
    }

    /**
     * places true or false for if the user is inside the zone or not
     * @param userInsideZone
     */
    public void setUserInsideZone(boolean userInsideZone) {
        isUserInsideZone = userInsideZone;
    }

    /** returns the marker ID*/
    public String getId() {
        return id;
    }
}
