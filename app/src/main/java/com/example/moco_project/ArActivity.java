package com.example.moco_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class ArActivity extends AppCompatActivity {
    Switch arcoreSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ar_availability);

        // Switch to allow pausing and resuming of ArActivity.
        arcoreSwitch = findViewById(R.id.arcore_switch);
        arcoreSwitch.setChecked(GameData.isArActivity());
        arcoreSwitch.setOnCheckedChangeListener(
                (view, checked) -> {
                    // Update the switch state
                    GameData.setArActivity(false);
                    if (!checked) {
                        startActivity(new Intent(ArActivity.this, MapActivity.class));
                    }
                }
        );

    }

    @Override
    protected void onResume() {
        super.onResume();

        // How to get latitude of first marker that has been generated:
        Toast.makeText(this, "first mushroom latitude: " +GameData.getMarkerData().get(0).getPosition().latitude , Toast.LENGTH_SHORT).show();
        //BTW the markers have IDs in their ".title" keys. IDs are given to markers when they are first generated.
        // They are numbers starting from 0 and their type is String.
        //When markers are clicked on the map, GameData.deleteMarkerByTitle(String title) is called.
        // This function deletes the correct instance from the markerdata list.
        //You can also get the last user location with GameData.getUserLocation()

    }
}