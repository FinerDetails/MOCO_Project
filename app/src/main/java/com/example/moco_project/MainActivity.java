package com.example.moco_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

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