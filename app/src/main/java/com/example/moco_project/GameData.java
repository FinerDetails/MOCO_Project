package com.example.moco_project;

import android.location.Location;

import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class GameData {
    private static boolean isArActivity = false;

    public static boolean isArActivity() {
        return isArActivity;
    }

    public static void setArActivity(boolean isArActivity) {
        GameData.isArActivity = isArActivity;
    }

    private static int mushrooms = 0;
    public static int getMushrooms() {
        return mushrooms;
    }

    public static void addMushrooms(int mushrooms) {
        GameData.mushrooms = GameData.mushrooms + mushrooms;
    }



    public static List<MarkerOptions> markerData = new ArrayList<>();
    public static List<MarkerOptions> getMarkerData() {
        return markerData;
    }
    public static void addMarker(MarkerOptions markerOptions){
        markerData.add(markerOptions);
    }
    static public void deleteMarkerByTitle(String title){
        for(MarkerOptions marker : markerData){
            if(marker.getTitle().equals(title)){
                markerData.remove(marker);
            }
        }
    }
    private static Location userLocation;
    public static void setUserLocation(Location locationData) {
        userLocation = locationData;
    }
    public Location getUserLocation() {
        return userLocation;
    }

}
