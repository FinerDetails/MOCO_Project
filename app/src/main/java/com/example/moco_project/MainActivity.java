package com.example.moco_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Button newGameButton;
    private Button loadGameButton;
    private Button settingsButton;
    private Button quitButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newGameButton = findViewById(R.id.button_new_game);
        loadGameButton = findViewById(R.id.button_load_game);
        settingsButton = findViewById(R.id.button_settings);
        quitButton = findViewById(R.id.button_quit);

        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,newGameButton.getText(),Toast.LENGTH_SHORT).show();
                // Code to be executed when the button is clicked
                // Add your desired functionality here
                //TODO
            }
        });

        loadGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoadGameActivity.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"test",Toast.LENGTH_SHORT).show();
                // Code to be executed when the button is clicked
                // Add your desired functionality here
                //TODO
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"test",Toast.LENGTH_SHORT).show();
                // Code to be executed when the button is clicked
                // Add your desired functionality here
                //TODO
            }
        });

    }
}