package com.example.moco_project;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

public class Zone {
    public LatLng getLocation() {
        return location;
    }

    private final Location userLocation;
    private static LatLng location;
    private static GoogleMap map;


    public double getZoneRadius() {
        return zoneRadius;
    }

    private static double zoneRadius;

    public Zone(Location userLocation, GoogleMap map, double zoneRadius) {
        this.userLocation = userLocation;
        this.map = map;
        this.zoneRadius = zoneRadius;
        LatLng userLatLng = new LatLng(userLocation.getLatitude(),userLocation.getLongitude());
        createZones(true, userLatLng);
    }

    public static Circle createZones(Boolean isPurpleZone, LatLng zoneLocation) {
        double currentLat;
        double currentLng;
        currentLat = zoneLocation.latitude;
        currentLng = zoneLocation.longitude;
        Circle circle;
        if (isPurpleZone) {
            // location = generatePoints(currentLat, currentLng, zoneRadius + 100, zoneRadius + 200);
            location = generatePoints(currentLat, currentLng, zoneRadius - 150, zoneRadius - 150);
            circle = map.addCircle(new CircleOptions()
                    .center(location)
                    .radius(zoneRadius) // Set radius of circle
                    .strokeWidth(8)
                    .strokeColor(Color.rgb(64, 39, 89))
                    .fillColor(Color.argb(215, 64, 39, 89))
                    .clickable(false));
        }
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

    public static LatLng generatePoints(double currentLat, double currentLng, double minDistance, double maxDistance) {
        Random random = new Random();
        final double conversionRate = 0.000009;
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

        if (distance > maxDistance) {
            distance = maxDistance;  // Cap the distance to the maximum distance
        }

        double latChange = distance * conversionRate * Math.cos(angle);
        double lngChange = distance * conversionRate * Math.sin(angle);

        double lat = currentLat + latChange;
        double lng = currentLng + lngChange;

        return new LatLng(lat, lng);
    }
}
