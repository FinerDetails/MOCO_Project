package com.example.moco_project;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

public class Zone {
    public LatLng getLocation() {
        return location;
    }

    private Location userLocation;
    private LatLng location;
    private GoogleMap map;

    public double getRadius() {
        return radius;
    }

    private double radius;

    public Zone(Location userLocation, GoogleMap map, double radius) {
        this.userLocation = userLocation;
        this.map = map;
        this.radius = radius;
        createZones();
    }

    private void createZones() {
        double currentLat = userLocation.getLatitude();
        double currentLng = userLocation.getLongitude();
        location = generatePoints(currentLat, currentLng);

        map.addCircle(new CircleOptions()
                //.center(new LatLng(latLng[0], latLng[1]))
                .center(location)
                .radius(radius) // Set radius of circle
                .strokeWidth(8)
                .strokeColor(Color.rgb(64, 39, 89))
                .fillColor(Color.argb(215, 64, 39, 89))
                .clickable(false));
    }

    public LatLng generatePoints(double currentLat, double currentLng) {
        Random random = new Random();
        final double conversionRate = 0.000009;
        final double minDistance = radius + 100.0; // Minimum radius from user location to center point of circle.
        final double maxDistance = radius + 200.0; // Maximum radius from user location to center point of circle.
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
