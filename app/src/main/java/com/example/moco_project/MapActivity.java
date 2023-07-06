package com.example.moco_project;

import android.Manifest.permission;
import android.annotation.SuppressLint;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapsInitializer.Renderer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MapActivity extends AppCompatActivity
        implements
        OnMapsSdkInitializedCallback,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener {

    /**
     * Request code for location permission request.
     */
    Switch arcoreSwitch;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in {@link
     * #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean permissionDenied = false;

    private GoogleMap map;
    private boolean cameraInitialized = false;
    private Zone zone;

    private boolean userInsideZone = false;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    public List<MarkerOptions> markerData = new ArrayList<>();

    private int mushrooms = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Use new maps renderer
        MapsInitializer.initialize(getApplicationContext(), Renderer.LATEST, this);
        setContentView(R.layout.activity_map);

        // Switch to allow pausing and resuming of ArActivity.
        arcoreSwitch = findViewById(R.id.arcore_switch);
        arcoreSwitch.setVisibility(View.GONE);
        arcoreSwitch.setChecked(SwitchState.isArActivity());
        arcoreSwitch.setOnCheckedChangeListener(
                (view, checked) -> {
                    // Update the switch state
                    SwitchState.setArActivity(true);
                    if (checked) {
                        Intent intent = new Intent(MapActivity.this, ArActivity.class);
                        // Share markerData with ArActivity
                        intent.putExtra("markerData", (Serializable) markerData);
                        startActivity(intent);
                    }
                }
        );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Move camera to location data
                    moveCameraTo(location);
                    cameraInitialized = true;
                    //Check whether or not user is inside the zone
                    if (zone != null){
                        whenUserInsideZone(location);
                    }
                }
            }
        };
        // Inputs the Map into the fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        enableMyLocation();
        startLocationUpdates();
        UiSettings uiSettings = map.getUiSettings();
        // Disable the My Location button
        uiSettings.setMyLocationButtonEnabled(false);
        waitForCameraAndCallgetLocation();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        // We return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        mushrooms++;
        Toast.makeText(this, "mushrooms: "+mushrooms, Toast.LENGTH_SHORT).show();
        marker.remove();
        return true;
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            return;
        }

        // 2. Otherwise, request location permissions from the user.
        PermissionUtils.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, true);
    }

    public void moveCameraTo(@NonNull Location location) {
        // Create a CameraUpdate object to specify the new camera position
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()),
                17f); // Zoom level: 17

        // Animate the camera movement to the new position
        map.animateCamera(cameraUpdate);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils
                .isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(createLocationRequest(),
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void waitForCameraAndCallgetLocation() {
        final Handler handler = new Handler();
        final int delay = 500; // Delay in milliseconds

        Runnable delayRunnable = new Runnable() {
            @Override
            public void run() {
                if (!cameraInitialized) {
                    handler.postDelayed(this, delay);
                }
                else{
                    getLocation();
                }
            }
        };
        handler.postDelayed(delayRunnable, delay);
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Create Zone based on the location
                        zone = new Zone(location,map,300);
                        generateMarkers();
                    }
                });
    }

    @Override
    public void onMapsSdkInitialized(@NonNull Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MapsDemo", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("MapsDemo", "The legacy version of the renderer is used.");
                break;
        }
    }
    public void whenUserInsideZone(Location location) {
        Location centerLocation = new Location("");
        centerLocation.setLatitude(zone.getLocation().latitude);
        centerLocation.setLongitude(zone.getLocation().longitude);

        float distanceBetweenZoneAndUser = location.distanceTo(centerLocation);

        if (distanceBetweenZoneAndUser < zone.getZoneRadius() && !userInsideZone){

            Toast.makeText(this, "Entering the zone", Toast.LENGTH_LONG).show();
            userInsideZone = true;
            arcoreSwitch.setVisibility(View.VISIBLE);
            // Launch ArActivity
            /*Intent intent = new Intent(MapActivity.this, ArActivity.class);
            startActivity(intent);*/
        }
        else if(distanceBetweenZoneAndUser > zone.getZoneRadius() && userInsideZone){
            Toast.makeText(this, "Exiting the zone", Toast.LENGTH_LONG).show();
            userInsideZone = false;
            arcoreSwitch.setVisibility(View.GONE);
        }
    }
    private void generateMarkers(){
        int width = 50; //In pixels
        int height = 50;
        int minMarkers = 100;
        int maxMarkers = 100;
        Random random = new Random();
        int randomMarkerAmount = random.nextInt(maxMarkers - minMarkers + 1) + minMarkers;
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mushroom_icon_edited);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);
        LatLng location = zone.getLocation();
        double zoneRadius = zone.getZoneRadius();
        for (int i = 0; i < randomMarkerAmount; i++ ) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(Zone.generatePoints(location.latitude, location.longitude, 0, zoneRadius))
                    .flat(true)
                    .icon(bitmapDescriptor);
            markerData.add(markerOptions);
            // Add the marker to the map
            map.addMarker(markerOptions);
            map.setOnMarkerClickListener(this);
        }
    }
}


