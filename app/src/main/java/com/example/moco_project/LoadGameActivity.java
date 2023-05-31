package com.example.moco_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class LoadGameActivity extends AppCompatActivity {
    private RecyclerView savedGameRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_game);
        savedGameRecyclerView = findViewById(R.id.savedGameRecyclerView);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the current activity
            }
        });

        ArrayList<LoadGameItem> loadGameList = new ArrayList<>();
        loadGameList.add(new LoadGameItem("placeholder1"));
        loadGameList.add(new LoadGameItem("placeholder2"));
        loadGameList.add(new LoadGameItem("placeholder3"));
        loadGameList.add(new LoadGameItem("placeholder4"));
        loadGameList.add(new LoadGameItem("placeholder5"));
        loadGameList.add(new LoadGameItem("placeholder6"));
        loadGameList.add(new LoadGameItem("placeholder7"));
        loadGameList.add(new LoadGameItem("placeholder8"));
        loadGameList.add(new LoadGameItem("placeholder9"));
        loadGameList.add(new LoadGameItem("placeholder10"));
        loadGameList.add(new LoadGameItem("placeholder11"));
        loadGameList.add(new LoadGameItem("placeholder12"));
        loadGameList.add(new LoadGameItem("placeholder13"));
        loadGameList.add(new LoadGameItem("placeholder14"));
        loadGameList.add(new LoadGameItem("placeholder15"));
        LoadGameRecyclerViewAdapter adapter = new LoadGameRecyclerViewAdapter();
        adapter.setLoadGameList(loadGameList);
        savedGameRecyclerView.setAdapter(adapter);
        savedGameRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

}