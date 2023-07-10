package com.example.moco_project;

import android.location.Location;

import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameData {
    private static boolean isArActivity = false;
    private static int mushrooms = 0;
    private static List<MarkerOptions> markerData = new ArrayList<>();

    private static Location userLocation;

    public static int getHunger() {
        return hunger;
    }

    public static int getHungerDecreaseInterval() {
        return hungerDecreaseInterval;
    }

    private static int hungerDecreaseInterval = 1000; //milliseconds

    public static void incrementHunger() {
        GameData.hunger += 10;
    }
    public static void decrementHunger() {
        GameData.hunger -= 1;
    }

    private static int hunger = 100;


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

    static public void deleteMarkerByTitle(String title){
        Iterator<MarkerOptions> iterator = markerData.iterator();
        while (iterator.hasNext()) {
            MarkerOptions marker = iterator.next();
            if (marker.getTitle().equals(title)) {
                iterator.remove();
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
