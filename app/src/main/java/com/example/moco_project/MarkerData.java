package com.example.moco_project;


import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MarkerData {

    //This class is used to hold the information required for both Map and AR side manipulation of markers.
    public Marker getMarker() {
        return marker;
    }

    public Circle getCircle() {
        return circle;
    }

    public MarkerOptions getMarkerOption() {return markerOption;}

    private Marker marker;
    private Circle circle;

    private MarkerOptions markerOption;

    public boolean isUserInsideZone() {
        return isUserInsideZone;
    }

    public void setUserInsideZone(boolean userInsideZone) {
        isUserInsideZone = userInsideZone;
    }

    private boolean isUserInsideZone = false;

    public String getId() {
        return id;
    }

    private String id;

    public MarkerData(Marker marker, Circle circle, MarkerOptions markerOption) {
        this.marker = marker;
        this.circle = circle;
        this.markerOption = markerOption;
        this.id = marker.getTitle();
    }


}
