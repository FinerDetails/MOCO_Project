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
import com.google.android.gms.maps.model.Circle;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/** This class uses Google maps as background and orientation point.
 * It handles the */
public class MapActivity extends AppCompatActivity
        implements
        OnMapsSdkInitializedCallback,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean permissionDenied = false;

    // A fresh copy of the google map
    private GoogleMap map;
    private boolean cameraInitialized = false;

    // An adventure zone on top of the map
    private Zone zone;

    // Devines if user is inside the zone or not
    private boolean userInsideZone = false;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;

    // Switch for changing between ArActivity and MapActivity
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch arcoreSwitch;

    // The bar that displays the current hunger status of the user
    private ProgressBar hungerBar;
    private Timer timer;
    private ConstraintLayout loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Enables use of the new map renderer
        MapsInitializer.initialize(getApplicationContext(), Renderer.LATEST, this);

        setContentView(R.layout.activity_map);
        //Loading
        loadingScreen = findViewById(R.id.loadingContainer);

        //Switch to allow pausing and resuming of ArActivity
        arcoreSwitch = findViewById(R.id.arcore_switch);
        arcoreSwitch.setVisibility(View.GONE);
        arcoreSwitch.setChecked(GameData.getIsArActivity());
        arcoreSwitch.setOnCheckedChangeListener(
                (view, checked) -> {
                    //Update the switch state
                    if (checked) {
                        GameData.setIsArActivity(true);
                        startActivity(new Intent(MapActivity.this, ArActivity.class));
                    }
                }
        );
        //Initializing an instance of the Fused Location Provider Client, which will be used to access the devices location services.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            //Callback for when the user location has been updated.
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // location is GPS-Location on Map
                for (Location location : locationResult.getLocations()) {
                    // Move camera to location data
                    moveCameraTo(location);
                    cameraInitialized = true;
                    GameData.setUserLocation(location);
                    //Checks whether or not user is inside the zone
                    if (zone != null){
                        whenUserInsidePurpleZone(location);
                        GameData.checkMarkerLocation(location);
                    }

                }
            }
        };
        //Inputs the Map into the layout fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Sets up timer for decreasing hunger meter;
        hungerBar = findViewById(R.id.hungerBar);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                hungerBar.setProgress(GameData.getHunger());
                GameData.decrementHunger(); //decrement hunger
            }
        }, 0, GameData.getHungerDecreaseInterval());
    }

    @Override
    protected void onResume() {
        super.onResume();
        arcoreSwitch.setChecked(GameData.getIsArActivity());
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        timer.cancel();
        super.onDestroy();
    }

    /** Callback for rendered map*/
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

    /**
     * We return false to indicate that we have not consumed the event and that we wish
     * for the default behavior to occur (which is for the camera to move such that the
     * marker is centered and for the marker's info window to open, if it has one).
     * @param marker the mushroom on map
     * @return true -> mushroom was eaten. lesser hungry; false -> mushroom is not visible and cant be consumed
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        //Only increments hunger if the clicked Marker has been set to visible
        if(marker.isVisible()){
            GameData.incrementHunger();
            GameData.deleteMarkerById(marker.getTitle());
        }
        return true;
    }

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
        //Create a CameraUpdate object to specify the new camera position on the map
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()),
                17f); // Zoom level: 17

        //Animate the camera movement to the new position
        map.animateCamera(cameraUpdate);
    }

    /** Requests user location results*/
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
            // Enable the my location layer if the permission has been granted and start location updates.
            enableMyLocation();
            startLocationUpdates();
        } else {
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

    /** Displays a dialog with error message explaining that the location permission is missing.*/
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /**
     * Creates a request for continuous location updates
     * Methods for LocationRequest became depricated since the start of development, but they still work.
     */
    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    /** Requests continuous location updates*/
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(createLocationRequest(),
                locationCallback,
                Looper.getMainLooper());
    }

    /** Stops continuous location updates*/
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /** Waits for the map camera to be ready before requesting the last known location.*/
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


    /** Requests the last known location one time.*/
    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location ->  {
            //Got last known location. In some rare situations this can be null.
            if (location != null) {
                //Create Zone based on the location
                zone = new Zone(location,map,300);
                generateMarkers();
            }
        });
    }

    /** Logs the use of the map renderer*/
    @Override
    public void onMapsSdkInitialized(@NonNull Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MapsRenderer", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("MapsRenderer", "The legacy version of the renderer is used.");
                break;
        }
    }

    /**
     * Detects if the user is in the zone and sets the visibility of the AR switch based on that.
     * Detection is based on radius of Zone compared to calculated distance of users location to center of the Zone
     */
    public void whenUserInsidePurpleZone(Location location) {
        Location centerLocation = new Location("");
        centerLocation.setLatitude(zone.getLocation().latitude);
        centerLocation.setLongitude(zone.getLocation().longitude);

        float distanceBetweenZoneAndUser = location.distanceTo(centerLocation);

        if (distanceBetweenZoneAndUser < zone.getZoneRadius() && !userInsideZone){

            Toast.makeText(this, "Entering the zone", Toast.LENGTH_LONG).show();
            //Updates global variable
            userInsideZone = true;
            //Sets visibility of AR switch
            arcoreSwitch.setVisibility(View.VISIBLE);
        }
        else if(distanceBetweenZoneAndUser > zone.getZoneRadius() && userInsideZone){
            Toast.makeText(this, "Exiting the zone", Toast.LENGTH_SHORT).show();
            userInsideZone = false;
            arcoreSwitch.setVisibility(View.GONE);
        }
    }
    /** Randomly generates new Markers on the map based on values in GameData and saves it to a list.*/
    private void generateMarkers(){
        int width = 50; //In pixels
        int height = 50;
        int minMarkers = GameData.getMinMushroomAmount();
        int maxMarkers = GameData.getMaxMushroomAmount();
        Random random = new Random();
        int randomMarkerAmount = random.nextInt(maxMarkers - minMarkers + 1) + minMarkers;

        //Adding an image to the markers
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mushroom_icon_edited);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap);

        LatLng location = zone.getLocation(); //Location of center of the Zone
        double zoneRadius = zone.getZoneRadius();
        for (int i = 0; i < randomMarkerAmount; i++ ) {
            //Location for center point of a single Marker
            LatLng generationLocation = Zone.generatePoints(location.latitude, location.longitude, 0, zoneRadius-GameData.getMushroomClickDistance());
            Circle circle = Zone.createZones(false,generationLocation);
            //API provided settings for the Marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(generationLocation)
                    .flat(true)
                    .icon(bitmapDescriptor).title(String.valueOf(i))
                    .visible(false);

            //Adds the marker to the map
            Marker marker = map.addMarker(markerOptions);
            map.setOnMarkerClickListener(this);

            //Creates new instance of a single Marker that holds the required info to manipulate the marker across different activities
            MarkerData markerData = new MarkerData(marker,circle, markerOptions);
            //Pushes instance to a list inside GameData
            GameData.addMarker(markerData);
            //Now that the Zone and the Markers have been created, the loadingScreen is lifted.
            loadingScreen.setVisibility(View.GONE);

        }
    }
}
