package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.JsonReader;
import android.util.Log;
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
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class StartNavi extends AppCompatActivity {
    public Double sLat, sLon, eLat, eLon;
    public String sName, eName;
    public String startName, endName;
    public LinearLayout linearLayoutTmap;
    public TMapView tMapView;
    public TextView sPlace, ePlace;
    public Button startAR;
    public SlidingUpPanelLayout slide;
    public Bitmap r_pin, b_pin;
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
        tMapView.setSKTMapApiKey("l7xx4df6476b09fd4a12962883291fb19544");
        linearLayoutTmap.addView(tMapView);
        r_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        b_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.b_pin);
        startAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast t = Toast.makeText(v.getContext(),"준비중인 서비스입니다.", Toast.LENGTH_LONG);
                t.show();
            }
        });
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
                    for(int i = 0; i < pt[0].size(); i++){
                        int t;
                        List<Double> p;
                        t = pt[0].get(i).getTurnType();
                        p = pt[0].get(i).getPoint();
                        if(t != -1) {
                            turn.add(t);
                        }
                        points.add(p);
                    }
                    TMapPolyLine tMapPolyLine = new TMapPolyLine();
                    tMapPolyLine.setLineColor(Color.BLUE);
                    tMapPolyLine.setLineWidth(10);
                    for(int i = 0 ; i < points.size(); i++){
                        alTMapPoint.add(new TMapPoint(points.get(i).get(1), points.get(i).get(0)));
                        tMapPolyLine.addLinePoint(alTMapPoint.get(i));
                        Log.d("FINAL_POINT", "[" + tMapPolyLine.getLinePoint().get(i).getLongitude() + "," + tMapPolyLine.getLinePoint().get(i).getLatitude() + "]");
                        Log.d("FINAL_POINT", "[" + tMapPolyLine.getLinePoint().get(i).getLongitude() + "," + tMapPolyLine.getLinePoint().get(i).getLatitude() + "]");
                        //Log.d("FINAL_TURN", turn.get(i) + "");
                    }
                    TMapMarkerItem[] marker = new TMapMarkerItem[2];
                    for(int i = 0; i < 2; i++){
                        marker[i] = new TMapMarkerItem();
                        marker[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                    }
                    marker[0].setIcon(b_pin);
                    marker[1].setIcon(r_pin);
                    marker[0].setTMapPoint(alTMapPoint.get(0)); // 마커의 좌표 지정
                    marker[1].setTMapPoint(alTMapPoint.get(points.size() - 1)); // 마커의 좌표 지정
                    tMapView.addMarkerItem("S",marker[0]);
                    tMapView.addMarkerItem("E", marker[1]);
                    tMapView.setCenterPoint(points.get(0).get(0), points.get(0).get(1));
                    slide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    slide.setTouchEnabled(true);
                    tMapView.addTMapPolyLine("Line1", tMapPolyLine);
                    tMapView.invalidate();
                }catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

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
        List<Double> coordinate = new ArrayList<Double>();
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
    public List<Double> readGeometry(JsonReader reader) throws IOException{
        List<Double> coordinate = new ArrayList<Double>();
        String value = "Point";
        reader.beginObject();
        while(reader.hasNext()){
            String key = reader.nextName();
            if(key.equals("type")){
                value = reader.nextString();
            }
            else if(key.equals("coordinates") && value.equals("Point")){
                coordinate = readCoordinate(reader);
            }
            else if(key.equals("coordinates") && value.equals("LineString")){
                coordinate = readCoordinateArr(reader);
            }
            else{
                coordinate.add(-1.0);
                coordinate.add(-1.0);
                reader.skipValue();
            }
        }
        reader.endObject();
        return coordinate;
    }
    public List<Double> readCoordinateArr(JsonReader reader) throws IOException{
        List<Double> coordinate = new ArrayList<Double>();

        reader.beginArray();
        while (reader.hasNext()){
            coordinate = readCoordinate(reader);
        }
        reader.endArray();
        return coordinate;
    }

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
    private List<Double> point = new ArrayList<Double>();
    private int turnType;

    public PointNTurn(){
        point.add(-1.0);
        point.add(-1.0);
        turnType = -1;
    }
    public PointNTurn(List<Double> coordinate, int turn) {
        this.point = coordinate;
        this.turnType = turn;
    }
    public List<Double> getPoint(){
        return point;
    }
    public int getTurnType(){
        return turnType;
    }
}