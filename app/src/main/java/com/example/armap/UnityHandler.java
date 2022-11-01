package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;

public class UnityHandler extends AppCompatActivity {
    public ArrayList<String> descriptions = new ArrayList<>();
    public ArrayList<double []> descriptionPoints = new ArrayList<>();
    public ArrayList<double []> allPoints = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unity_handler);
        UnityPlayer mUnityPlayer = new UnityPlayer(this);
        Intent gIntent = getIntent();
        descriptionPoints = (ArrayList<double[]>) gIntent.getSerializableExtra("DescriptionPoints");
        descriptions = (ArrayList<String>) gIntent.getSerializableExtra("Descriptions");
        allPoints = (ArrayList<double[]>)gIntent.getSerializableExtra("AllPoints");
        mUnityPlayer.UnitySendMessage("GeospatialHandler","getDescLen", String.valueOf(descriptions.size()));
        mUnityPlayer.UnitySendMessage("GeospatialHandler","getDescPointsLen",String.valueOf(descriptionPoints.size()));
        mUnityPlayer.UnitySendMessage("GeospatialHandler","getAllPointsLen",String.valueOf(allPoints.size()));
        for(int i = 0; i < descriptionPoints.size(); i++){
            for(int j = 0; j <descriptionPoints.get(i).length; j++) {
                mUnityPlayer.UnitySendMessage("GeospatialHandler", "getDescriptionPoints", String.valueOf(descriptionPoints.get(i)[j]));
            }
        }
        mUnityPlayer.UnitySendMessage("GeospatialHandler","getDescriptionPoints", "0");
        for(int i = 0 ; i < descriptions.size(); i++){
            mUnityPlayer.UnitySendMessage("GeospatialHandler","getDescriptions",descriptions.get(i));
        }
        for(int i = 0; i < allPoints.size(); i++) {
            for (int j = 0; j < allPoints.get(i).length; j++) {
                mUnityPlayer.UnitySendMessage("GeospatialHandler", "getAllPoints", String.valueOf(allPoints.get(i)[j]));
            }
        }
        mUnityPlayer.UnitySendMessage("GeospatialHandler", "getAllPoints", "0");

        Intent uIntent = new Intent(this, UnityPlayerActivity.class);
        uIntent.putExtra("Descriptions", descriptions);
        uIntent.putExtra("DescriptionPoints", descriptionPoints);
        uIntent.putExtra("AllPoints", allPoints);
        startActivity(uIntent);

    }
}