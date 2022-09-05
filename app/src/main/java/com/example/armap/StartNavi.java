package com.example.armap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.JsonReader;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class StartNavi extends AppCompatActivity {
    public Double sLat, sLon, eLat, eLon, userLat, userLon;
    public TMapPoint userPoint;
    public String sName, eName;
    public String startName, endName;
    public LinearLayout linearLayoutTmap;
    public TMapView tMapView;
    public TextView sPlace, ePlace;
    public Button startAR;
    public SlidingUpPanelLayout slide;
    public Bitmap r_pin, b_pin, u_pin;
    public TMapMarkerItem userPin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_navi);

        sPlace = (TextView)findViewById(R.id.sPointTxt);
        ePlace = (TextView)findViewById(R.id.ePointTxt);
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        startAR = (Button)findViewById(R.id.startAR);
        slide = (SlidingUpPanelLayout)findViewById(R.id.slide);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("TMAP APP KEY");
        linearLayoutTmap.addView(tMapView);
        r_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        b_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.b_pin);
        u_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.blue_dot);
        userPin = new TMapMarkerItem();
        userPin.setIcon(u_pin);
        startAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast t = Toast.makeText(v.getContext(),"준비중인 서비스입니다.", Toast.LENGTH_LONG);
                t.show();
            }
        });
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final LocationListener gpsLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String provider = location.getProvider();  // 위치정보
                userLon = location.getLongitude(); // 위도
                userLat = location.getLatitude(); // 경도
                userPoint = new TMapPoint(userLat, userLon);
                if(userPoint != null){
                    tMapView.removeMarkerItem("User");
                    userPin.setTMapPoint(userPoint);
                    tMapView.setCenterPoint(userPoint.getLongitude(), userPoint.getLatitude());
                    tMapView.addMarkerItem("User", userPin);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /*Log.d("PERMISSON", "DENIY");
            moveTaskToBack(true);
            finishAndRemoveTask();
            System.exit(0);
            return;*/
        }else{
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null){
                String provider = location.getProvider();  // 위치정보
                userLon = location.getLongitude(); // 위도
                userLat = location.getLatitude(); // 경도
                userPoint = new TMapPoint(userLat, userLon);
                if(userPoint != null){
                    userPin.setTMapPoint(userPoint);
                    tMapView.setCenterPoint(userPoint.getLongitude(), userPoint.getLatitude());
                    tMapView.addMarkerItem("User", userPin);
                }
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationListener);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsLocationListener);
        }

    }

    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        startName = intent.getStringExtra("sName");
        endName = intent.getStringExtra("eName");
        try {
            sName = URLEncoder.encode(startName, "UTF-8");
            eName = URLEncoder.encode(endName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sLat = intent.getDoubleExtra("sLat", 0);
        sLon = intent.getDoubleExtra("sLon", 0);

        eLat = intent.getDoubleExtra("eLat", 0);
        eLon = intent.getDoubleExtra("eLon", 0);
        sPlace.setText(startName);
        ePlace.setText(endName);
        Log.d("POINT", sName + "[" + sLon + ", " + sLat + "]");
        Log.d("POINT", eName + "[" + eLon + ", " + eLat + "]");
        final List<PointNTurn>[] pt = new List[]{new ArrayList<PointNTurn>()};
        ArrayList<List<Double>> points = new ArrayList<>();
        List<Integer> turn = new ArrayList<>();
        ArrayList<TMapPoint> alTMapPoint = new ArrayList<TMapPoint>();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                URL tMapEndpoint;
                try {
                    tMapEndpoint = new URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&startX=" + sLon + "&startY=" + sLat + "&endX=" + eLon + "&endY=" + eLat + "&startName=" + sName + "&endName=" + eName + "&appKey=l7xx4df6476b09fd4a12962883291fb19544");
                    HttpsURLConnection mapConn = (HttpsURLConnection)tMapEndpoint.openConnection();
                    //mapConn.addRequestProperty("searchOption", "10");
                    if(mapConn.getResponseCode() == 200){
                        InputStream responseBody = mapConn.getInputStream();
                        InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
                        JsonReader jsonReader = new JsonReader(responseBodyReader);
                        jsonReader.beginObject(); // Start processing the JSON object
                        while (jsonReader.hasNext()) { // Loop through all keys
                            String key = jsonReader.nextName();
                            if(key.equals("features")){
                                pt[0] = readFeatures(jsonReader);
                            }
                            else{
                                jsonReader.skipValue();
                            }
                        }
                        jsonReader.endObject();
                        jsonReader.close();
                    } else {
                        Log.d("ERROR", mapConn.getResponseCode() + ", " + mapConn.getResponseMessage());
                    }
                    mapConn.disconnect();
                    ArrayList<List<Double>> p = null;
                    for(int i = 0; i < pt[0].size(); i++){
                        int t;
                        t = pt[0].get(i).getTurnType();
                        p = pt[0].get(i).getPoint();
                        if(t != -1) {
                            turn.add(t);
                        }
                        for(int j = 0; j < p.size(); j++){
                            points.add(p.get(j));
                        }
                        //points.add(p);
                    }
                    TMapPolyLine tMapPolyLine = new TMapPolyLine();
                    tMapPolyLine.setLineColor(Color.CYAN);
                    tMapPolyLine.setLineWidth(15);
                    tMapPolyLine.setOutLineWidth(25);
                    tMapPolyLine.setOutLineColor(Color.BLUE);
                    Log.d("SIZE", points.size() + "");
                    for(int i = 0 ; i < points.size(); i++){
                        alTMapPoint.add(new TMapPoint(points.get(i).get(1), points.get(i).get(0)));
                        tMapPolyLine.addLinePoint(alTMapPoint.get(i));
                    }
                    TMapMarkerItem[] marker = new TMapMarkerItem[3];
                    for(int i = 0; i < 2; i++){
                        marker[i] = new TMapMarkerItem();
                        marker[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                    }
                    marker[0].setIcon(b_pin);
                    marker[1].setIcon(r_pin);
                    marker[0].setTMapPoint(alTMapPoint.get(0)); // 마커의 좌표 지정
                    marker[1].setTMapPoint(alTMapPoint.get(points.size() - 1)); // 마커의 좌표 지정
                    slide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    slide.setTouchEnabled(true);
                    tMapView.addTMapPolyLine("Line1", tMapPolyLine);
                    tMapView.addMarkerItem("S",marker[0]);
                    tMapView.addMarkerItem("E", marker[1]);

                }catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }

    public List<PointNTurn> readFeatures(JsonReader reader) throws IOException{
        List<PointNTurn> pt = new ArrayList<PointNTurn>();
        reader.beginArray();
        while(reader.hasNext()){
            pt.add(readFeatureArr(reader));
        }
        reader.endArray();
        return pt;
    }
    public PointNTurn readFeatureArr(JsonReader reader) throws IOException{//리턴 2개(방향, 포인트)
        ArrayList<List<Double>> coordinate = new ArrayList<>();
        int turn = -1;
        reader.beginObject();
        while(reader.hasNext()){
            String key = reader.nextName();
            if(key.equals("geometry")){
                coordinate = readGeometry(reader);
            }
            else if(key.equals("properties")){
                turn = readProperties(reader);
            }
            else{
                reader.skipValue();
            }
        }
        reader.endObject();
        return new PointNTurn(coordinate, turn);
    }
    public ArrayList<List<Double>> readGeometry(JsonReader reader) throws IOException{
        ArrayList<List<Double>> coordinate = new ArrayList<>();
        String value = "Point";
        reader.beginObject();
        while(reader.hasNext()){
            String key = reader.nextName();
            if(key.equals("type")){
                value = reader.nextString();
            }
            else if(key.equals("coordinates") && value.equals("Point")){
                coordinate.add(readCoordinate(reader));
            }
            else if(key.equals("coordinates") && value.equals("LineString")){
                reader.beginArray();
                while(reader.hasNext()){
                    coordinate.add(readCoordinate(reader));
                }
                reader.endArray();
            }
            else{
                coordinate.add(Collections.singletonList(-1.0));
                coordinate.add(Collections.singletonList(-1.0));
                reader.skipValue();
            }
        }
        reader.endObject();
        return coordinate;
    }
    /*public ArrayList<List<Double>> readCoordinateArr(JsonReader reader) throws IOException{
        ArrayList<List<Double>> coordinate = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()){
            coordinate.add(readCoordinate(reader));
        }
        reader.endArray();
        return coordinate;
    }*/

    public List<Double> readCoordinate(JsonReader reader) throws IOException{
        List<Double> coordinate = new ArrayList<Double>();
        reader.beginArray();
        while (reader.hasNext()){
            coordinate.add(reader.nextDouble());
        }
        reader.endArray();
        return coordinate;
    }

    public int readProperties(JsonReader reader) throws IOException{
        int turn = -1;
        reader.beginObject();
        while (reader.hasNext()){
            String key = reader.nextName();
            if(key.equals("turnType")){
                turn = reader.nextInt();
            }
            else{
                reader.skipValue();
            }
        }
        reader.endObject();
        return turn;
    }
}

class PointNTurn{
    private ArrayList<List<Double>> point = new ArrayList<>();
    private int turnType;

    public PointNTurn(){
        point.add(Collections.singletonList(-1.0));
        point.add(Collections.singletonList(-1.0));
        turnType = -1;
    }
    public PointNTurn(ArrayList<List<Double>> coordinate, int turn) {
        this.point = coordinate;
        this.turnType = turn;
    }
    public ArrayList<List<Double>> getPoint(){
        return point;
    }
    public int getTurnType(){
        return turnType;
    }
}