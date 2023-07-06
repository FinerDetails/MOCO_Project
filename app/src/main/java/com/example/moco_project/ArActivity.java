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
    List<MarkerOptions> markerData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ar_availability);

        // Retrieve the markerData list from the intent
        Intent intent = getIntent();
        if (intent != null) {
            markerData = (List<MarkerOptions>) intent.getSerializableExtra("markerData");
        }

        // Switch to allow pausing and resuming of ArActivity.
        arcoreSwitch = findViewById(R.id.arcore_switch);
        arcoreSwitch.setChecked(SwitchState.isArActivity());
        arcoreSwitch.setOnCheckedChangeListener(
                (view, checked) -> {
                    // Update the switch state
                    SwitchState.setArActivity(false);
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
        Toast.makeText(this, "first mushroom latitude " +markerData.get(0).getPosition().latitude , Toast.LENGTH_SHORT).show();

    }
}