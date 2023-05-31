package com.example.moco_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LoadGameRecyclerViewAdapter extends RecyclerView.Adapter<LoadGameRecyclerViewAdapter.ViewHolder> {

    private ArrayList<LoadGameItem> loadGameList = new ArrayList<>();

    public LoadGameRecyclerViewAdapter() {
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { //creates instances for each LoadGameItem view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.load_game_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { //manipulates LoadGameItem view
        holder.itemDate.setText("PLACEHOLDER");
    }

    @Override
    public int getItemCount() {
        return loadGameList.size();
    }

    public void setLoadGameList(ArrayList<LoadGameItem> loadGameList) {
        this.loadGameList = loadGameList;
        notifyDataSetChanged(); //refresh item list
    }

    public class ViewHolder extends RecyclerView.ViewHolder{ //responsible for holding references to load_game_item views.
        private TextView itemDate;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemDate = itemView.findViewById(R.id.itemDate);
        }
    }
}
