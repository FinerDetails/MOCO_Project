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

    private final Location userLocation;
    private LatLng location;
    private final GoogleMap map;


    public double getZoneRadius() {
        return zoneRadius;
    }

    private final double zoneRadius;

    public Zone(Location userLocation, GoogleMap map, double zoneRadius) {
        this.userLocation = userLocation;
        this.map = map;
        this.zoneRadius = zoneRadius;
        createZones();
    }

    private void createZones() {
        double currentLat = userLocation.getLatitude();
        double currentLng = userLocation.getLongitude();
        // location = generatePoints(currentLat, currentLng, zoneRadius + 100, zoneRadius + 200); -150
        location = generatePoints(currentLat, currentLng, zoneRadius -10, zoneRadius -10);
        map.addCircle(new CircleOptions()
                .center(location)
                .radius(zoneRadius) // Set radius of circle
                .strokeWidth(8)
                .strokeColor(Color.rgb(64, 39, 89))
                .fillColor(Color.argb(215, 64, 39, 89))
                .clickable(false));
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
