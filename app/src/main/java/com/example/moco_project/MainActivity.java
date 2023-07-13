package com.example.moco_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

/**
 * This class is our starting point for the app.
 * It was supposed to hold the starting menu with the possibility to change settings and restart/start
 * a new game.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Launch MapActivity
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);

        // Finish the current activity
        //finish();

    }
}