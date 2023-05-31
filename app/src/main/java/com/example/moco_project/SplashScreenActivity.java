package com.example.moco_project;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {
    private static final int SPLASH_SCREEN_DURATION = 3000; // 3 seconds
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start the MainActivity or another desired activity
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: Close the splash screen activity to prevent going back to it
            }
        }, SPLASH_SCREEN_DURATION);
    }
}
