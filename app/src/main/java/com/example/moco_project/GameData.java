package com.example.moco_project;

import android.graphics.Color;
import android.location.Location;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameData {
    private static boolean isArActivity = false;
    private static int mushrooms = 0;
    private static List<MarkerData> markerDataList = new ArrayList<>();

    private static Location userLocation;
    private static int hungerDecreaseInterval = 1000; //milliseconds
    private static int hunger = 100;



    private static int minMushroomAmount = 100;
    private static int maxMushroomAmount = 100;



    private static double mushroomClickDistance = 20.0;

    public static int getHunger() {
        return hunger;
    }

    public static int getHungerDecreaseInterval() {
        return hungerDecreaseInterval;
    }



    public static void incrementHunger() {
        GameData.hunger += 10;
    }
    public static void decrementHunger() {
        GameData.hunger -= 1;
    }


    public static int getMinMushroomAmount() {
        return minMushroomAmount;
    }
    public static int getMaxMushroomAmount() {
        return maxMushroomAmount;
    }
    public static double getMushroomClickDistance() {
        return mushroomClickDistance;
    }
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

    public static List<MarkerData> getMarkerData() {
        return markerDataList;
    }

    public static void addMarker(MarkerData markerData) {
        markerDataList.add(markerData);
    }
    static public void checkMarkerLocation(Location location){
        Iterator<MarkerData> iterator = markerDataList.iterator();
        while (iterator.hasNext()) {
            MarkerData markerData = iterator.next();
            Location centerLocation = new Location("");
            centerLocation.setLatitude(markerData.getCircle().getCenter().latitude);
            centerLocation.setLongitude(markerData.getCircle().getCenter().longitude);
            float distanceBetweenZoneAndUser = location.distanceTo(centerLocation);


            if (distanceBetweenZoneAndUser < mushroomClickDistance && !markerData.isUserInsideZone()) {
                markerData.getCircle().setStrokeColor(Color.rgb(255, 138, 101));
                markerData.setUserInsideZone(true);
            }
            else if(distanceBetweenZoneAndUser > mushroomClickDistance && markerData.isUserInsideZone()){
                markerData.getCircle().setStrokeColor(Color.TRANSPARENT);
                markerData.setUserInsideZone(false);
            }
        }
    }
    static public void deleteMarkerById(String title){
        Iterator<MarkerData> iterator = markerDataList.iterator();
        while (iterator.hasNext()) {
            MarkerData markerData = iterator.next();
            if (markerData.getId().equals(title)) {
                markerData.getCircle().remove();
                markerData.getMarker().remove();
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
