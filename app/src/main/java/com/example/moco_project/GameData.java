package com.example.moco_project;

import android.location.Location;

import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class GameData {
    private static boolean isArActivity = false;
    private static int mushrooms = 0;
    private static List<MarkerOptions> markerData = new ArrayList<>();

    private static Location userLocation;


    public static boolean getIsArActivity() {
        return isArActivity;
    }

    public static void setIsArActivity(boolean isArActivity) {
        GameData.isArActivity = isArActivity;
    }

    public static int getMushrooms() {
        return mushrooms;
    }

    public static void addMushrooms(int mushrooms) {
        GameData.mushrooms = GameData.mushrooms + mushrooms;
    }

    public static List<MarkerOptions> getMarkerData() {
        return markerData;
    }

    public static void addMarker(MarkerOptions markerOptions) {
        markerData.add(markerOptions);
    }

    public static void deleteMarkerByTitle(String title) {
        for(MarkerOptions marker : markerData) {
            if(marker.getTitle().equals(title)) {
                markerData.remove(marker);
            }
        }
    }

    public static void setUserLocation(Location location) {
        userLocation = userLocation;
    }

    public Location getUserLocation() {
        return userLocation;
    }
}
