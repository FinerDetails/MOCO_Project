package com.example.moco_project;


import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

public class MarkerData {
    public Marker getMarker() {
        return marker;
    }

    public Circle getCircle() {
        return circle;
    }

    private Marker marker;
    private Circle circle;

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

    public MarkerData(Marker marker, Circle circle) {
        this.marker = marker;
        this.circle = circle;
        this.id = marker.getTitle();
    }


}
