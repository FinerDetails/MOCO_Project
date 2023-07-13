package com.example.moco_project;

import android.graphics.Color;
import android.location.Location;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class that handles data which is shared between the MapActivity and ArActivity
 */
public class GameData {

    //This class holds shared data and methods meant to be accessed by both MapActivity and ArActivity
    private static boolean isArActivity = false;
    // amount of mushrooms that were collected. Number which would be shown in our bagpack
    private static int mushrooms = 0;
    //List that holds data regarding the placed mushrooms
    private static List<MarkerData> markerDataList = new ArrayList<>();
    // the current location of the user
    private static Location userLocation;
    // sets how fast the character gets hungry
    private static int hungerDecreaseInterval = 1000; //milliseconds
    // Maximum value of satisfaction. if hunger is 100 the person is satisfied
    private static int hunger = 100;


    // minimum amount of mushrooms that will be placed on the map
    private static int minMushroomAmount = 100;
    // maximum amount of mushrooms that will be placed on the map
    private static int maxMushroomAmount = 100;



    private static double mushroomClickDistance = 45.0;

    // returns the current hunger level of the user
    public static int getHunger() {
        return hunger;
    }

    // return the hungerinterval
    public static int getHungerDecreaseInterval() {
        return hungerDecreaseInterval;
    }


    /** increases the satisfaction level of the user. --> Lesser hungry*/
    public static void incrementHunger() {
        GameData.hunger += 10;
    }

    /** decreases the satisfaction level of the user. --> User gets more hungry*/
    public static void decrementHunger() {
        GameData.hunger -= 1;
    }

    /** returns the minimum amount of mushrooms*/
    public static int getMinMushroomAmount() {
        return minMushroomAmount;
    }

    /** returns the maximum amount of mushrooms*/
    public static int getMaxMushroomAmount() {
        return maxMushroomAmount;
    }

    public static double getMushroomClickDistance() {
        return mushroomClickDistance;
    }

    /** returns true if ArActivity is active. false if MapActivity is active*/
    public static boolean getIsArActivity() {
        return isArActivity;
    }

    /** sets if ArActivity is active or not*/
    public static void setIsArActivity(boolean isArActivity) {
        GameData.isArActivity = isArActivity;
    }

    /** returns a list that holds informationen regarding the placed mushrooms*/
    public static List<MarkerData> getMarkerData() {
        return markerDataList;
    }

    /** adds a new mushroom marker to the detail list*/
    public static void addMarker(MarkerData markerData) {
        markerDataList.add(markerData);
    }

    /** Checks if the user is within the zone of a marker in MapActivity and sets visibility based on that*/
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
                markerData.getMarker().setVisible(true);
                markerData.setUserInsideZone(true);
            }
            else if(distanceBetweenZoneAndUser > mushroomClickDistance && markerData.isUserInsideZone()){
                markerData.getCircle().setStrokeColor(Color.TRANSPARENT);
                markerData.getMarker().setVisible(false);
                markerData.setUserInsideZone(false);
            }
        }
    }

   /** Deletes a Marker through its ID*/
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

    /** Sets the location coordinates of an user*/
    public static void setUserLocation(Location location) {
        userLocation = userLocation;
    }


    //The unused methods were meant for getting and keeping track of collected mushrooms that could work as a score system
    //This was never implemented however


    public static int getMushrooms() {
        return mushrooms;
    }

    public static void addMushrooms(int mushrooms) {
        GameData.mushrooms = GameData.mushrooms + mushrooms;
    }

    public Location getUserLocation() {
        return userLocation;
    }
}
