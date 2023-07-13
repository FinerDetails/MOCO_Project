package com.example.moco_project;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;


/**
 * Class for the big purple Zone. --> Adventure zone in which the activation of the AR mode is possible
 */
public class Zone {

    // Latitude and Longitude position of the zone
    private static LatLng location;
    // the map on which the Zone is going to be drawn
    private static GoogleMap map;
    // stores the radius for the zone -> defines how big the zone circle will be
    private static double zoneRadius;

    /** Constructor for the adventure zone*/
    public Zone(Location userLocation, GoogleMap map, double zoneRadius) {
        this.map = map;
        this.zoneRadius = zoneRadius;
        LatLng userLatLng = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
        createZones(true, userLatLng);
    }

    /** returns the current location of the zone*/
    public LatLng getLocation() {
        return location;
    }

    /** returns the radius of the zone*/
    public double getZoneRadius() {
        return zoneRadius;
    }

    /** Creates the zone circle on the map*/
    public static Circle createZones(Boolean isPurpleZone, LatLng zoneLocation) {
        double currentLat;
        double currentLng;
        currentLat = zoneLocation.latitude;
        currentLng = zoneLocation.longitude;
        Circle circle;

        //Creation of the purple Zone
        if (isPurpleZone) {
            // location = generatePoints(currentLat, currentLng, zoneRadius + 100, zoneRadius + 200); this line is left to be able to switch to another configuration
            location = generatePoints(currentLat, currentLng, zoneRadius - 150, zoneRadius - 150);
            circle = map.addCircle(new CircleOptions()
                    .center(location)
                    .radius(zoneRadius) // Set radius of circle
                    .strokeWidth(8)
                    .strokeColor(Color.rgb(64, 39, 89))
                    .fillColor(Color.argb(215, 64, 39, 89))
                    .clickable(false));
        }
        //Creation for Zones surrounding the markers.
        else{
            location = new LatLng(currentLat,currentLng);
            circle = map.addCircle(new CircleOptions()
                    .center(location)
                    .radius(GameData.getMushroomClickDistance()) // Set radius of circle
                    .strokeWidth(8)
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(Color.TRANSPARENT)
                    .clickable(false));
        }
        return circle;
    }


    /** Randomly generates a LatLng object to a wanted distance*/
    public static LatLng generatePoints(double currentLat, double currentLng, double minDistance, double maxDistance) {
        Random random = new Random();
        final double conversionRate = 0.000009;
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

        if (distance > maxDistance) {
            distance = maxDistance;  //Cap the distance to the maximum distance
        }

        double latChange = distance * conversionRate * Math.cos(angle);
        double lngChange = distance * conversionRate * Math.sin(angle);

        double lat = currentLat + latChange;
        double lng = currentLng + lngChange;

        return new LatLng(lat, lng);
    }
}
