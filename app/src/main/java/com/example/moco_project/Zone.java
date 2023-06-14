package com.example.moco_project;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

public class Zone {
    private Location location;
    private GoogleMap map;

    public Zone(Location location, GoogleMap map) {
        this.location = location;
        this.map = map;
        createZones();
    }

    private void createZones() {
        double currentLat = location.getLatitude();
        double currentLng = location.getLongitude();
        double [] latLng = generatePoints(currentLat, currentLng);

        map.addCircle(new CircleOptions()
                .center(new LatLng(latLng[0], latLng[1]))
                .radius(100) // Set radius of circle
                .strokeWidth(8)
                .strokeColor(Color.rgb(64, 39, 89))
                .fillColor(Color.argb(215, 64, 39, 89))
                .clickable(false));
    }

    public double[] generatePoints(double currentLat, double currentLng) {
        Random random = new Random();
        final double conversionRate = 0.000009;
        final double minDistance = 150.0; // Minimum radius from user location to center point of circle.
        final double maxDistance = 300.0; // Maximum radius from user location to center point of circle.
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

        if (distance > maxDistance) {
            distance = maxDistance;  // Cap the distance to the maximum distance
        }

        double latChange = distance * conversionRate * Math.cos(angle);
        double lngChange = distance * conversionRate * Math.sin(angle);

        double lat = currentLat + latChange;
        double lng = currentLng + lngChange;

        return new double[]{lat, lng};
    }
}
