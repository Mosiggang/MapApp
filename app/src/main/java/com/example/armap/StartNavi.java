package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

public class StartNavi extends AppCompatActivity implements SensorEventListener, TMapGpsManager.onLocationChangedCallback {
    public Double sLat, sLon, eLat, eLon, userLat, userLon;
    public TMapPoint userPoint, ePoint;
    public String pathType;
    public String startName, endName;
    public LinearLayout linearLayoutTmap;
    public TMapView tMapView;
    public TextView sPlace, ePlace;
    public Button startAR, cLocationBtn;
    public SlidingUpPanelLayout slide;
    public Bitmap r_pin, b_pin, u_pin;
    public TMapMarkerItem userPin;
    public TMapGpsManager gps;
    public SensorManager sensorManager;
    public Sensor acc, mag;
    private final float[] accRead = new float[3];
    private final float[] magRead = new float[3];
    private final float[] rMatrix = new float[9];
    private final float[] oAngles = new float[3];
    public PathRequestThread getPath;
    public List<TMapPoint> pathPoints = new ArrayList<>();
    public ArrayList<String> descriptions = new ArrayList<>();
    public ArrayList<double []> descriptionPoints = new ArrayList<>();
    public ArrayList<double []> allPoints = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_navi);
        sPlace = (TextView) findViewById(R.id.sPointTxt);
        ePlace = (TextView) findViewById(R.id.ePointTxt);
        linearLayoutTmap = (LinearLayout) findViewById(R.id.linearLayoutTmap);
        startAR = (Button) findViewById(R.id.startAR);
        cLocationBtn = (Button) findViewById(R.id.btnCLocation);
        slide = (SlidingUpPanelLayout) findViewById(R.id.slide);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("l7xx4df6476b09fd4a12962883291fb19544");//l7xx4df6476b09fd4a12962883291fb19544
        linearLayoutTmap.addView(tMapView);
        r_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        b_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.b_pin);
        u_pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.direction);
        userPin = new TMapMarkerItem();
        userPin.setIcon(u_pin);

        gps = new TMapGpsManager(this);
        gps.setMinTime(1000);
        gps.setMinDistance(5);
        gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
        gps.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        gps.OpenGps();
        startAR.setOnClickListener(v -> {
            double[] last = new double[] {pathPoints.get(pathPoints.size() - 1).getLatitude(), pathPoints.get(pathPoints.size() - 1).getLongitude()};
            allPoints.add(last);
            descriptions.add("도착");
            descriptionPoints.add(last);
            Intent unityIntent = new Intent(this, UnityHandler.class);
            unityIntent.putExtra("Descriptions", descriptions);
            unityIntent.putExtra("DescriptionPoints", descriptionPoints);
            unityIntent.putExtra("AllPoints", allPoints);
            startActivity(unityIntent);

        });
        cLocationBtn.setOnClickListener(v -> {
            tMapView.removeMarkerItem("user");
            userPoint = gps.getLocation();
            userPin.setTMapPoint(userPoint);
            userPin.setIcon(u_pin);
            tMapView.setCenterPoint(userPoint.getLongitude(), userPoint.getLatitude());
            tMapView.addMarkerItem("user", userPin);
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        int fPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int cPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (fPermissionCheck == PackageManager.PERMISSION_DENIED || cPermissionCheck == PackageManager.PERMISSION_DENIED) { //위치 권한 확인
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                userLon = location.getLongitude(); // 위도
                userLat = location.getLatitude(); // 경도
                if (userPoint != null) {
                    tMapView.setCenterPoint(userLon, userLat);
                }
            }

        }
    }

    protected void onResume() {
        super.onResume();
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (acc != null) {
            sensorManager.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (mag != null) {
            sensorManager.registerListener((SensorEventListener) this, mag, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        tMapView.setCenterPoint(userPoint.getLongitude(), userPoint.getLatitude());
    }

    protected void onPause() {
        super.onPause();
        gps.CloseGps();
        sensorManager.unregisterListener((SensorEventListener) this);
    }

    protected void onStart() {
        super.onStart();
        pathPoints = new ArrayList<>();
        descriptions = new ArrayList<>();
        descriptionPoints = new ArrayList<>();
        allPoints = new ArrayList<>();
        tMapView.setPathRotate(true);
        tMapView.setRotateEnable(true);
        Intent intent = getIntent();
        startName = intent.getStringExtra("sName");
        endName = intent.getStringExtra("eName");
        pathType = intent.getStringExtra("PathType");
        sLat = intent.getDoubleExtra("sLat", 0);
        sLon = intent.getDoubleExtra("sLon", 0);
        eLat = intent.getDoubleExtra("eLat", 0);
        eLon = intent.getDoubleExtra("eLon", 0);
        if (pathType.equals("C2D")) {
            slide.setTouchEnabled(true);
            userPoint = gps.getLocation();
            userPin.setTMapPoint(userPoint);
            userPin.setIcon(u_pin);
            tMapView.addMarkerItem("user", userPin);
        } else {
            slide.setTouchEnabled(false);
            Toast.makeText(this,"AR 사용이 제한되는 검색 설정입니다.",Toast.LENGTH_SHORT).show();
            tMapView.setCenterPoint(sLon, sLat);
        }
        ePoint = new TMapPoint(eLat, eLon);
        userPoint = new TMapPoint(sLat, sLon);
        sPlace.setText(startName);
        ePlace.setText(endName);
        getPath = new PathRequestThread();
        getPath.start();
        try {
            getPath.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accRead, 0, accRead.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magRead, 0, magRead.length);
        }
        if (accRead != null && magRead != null) {
            updateOrientationAngles();
            try {
                tMapView.removeMarkerItem("user");
                Bitmap newPin = getRotatedBitmap(u_pin, (float) Math.toDegrees(oAngles[0]) - 90);
                userPin.setIcon(newPin);
                tMapView.addMarkerItem("user", userPin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateOrientationAngles() {
        boolean success;
        success = SensorManager.getRotationMatrix(rMatrix, null, accRead, magRead);
        if (success) {
            SensorManager.getOrientation(rMatrix, oAngles);
        }
    }

    public Bitmap getRotatedBitmap(Bitmap bitmap, float degrees) {
        if (bitmap == null) return null;
        if (degrees == 0.0f) return bitmap;
        Matrix m = new Matrix();
        m.setRotate(degrees + tMapView.getRotate(), (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        gps.CloseGps();
        finish();
    }

    @Override
    public void onLocationChange(Location location) {
        sLat = location.getLatitude();
        sLon = location.getLongitude();
        userPoint = gps.getLocation();
        userPin.setTMapPoint(userPoint);
        tMapView.setCenterPoint(userPoint.getLongitude(), userPoint.getLatitude());
    }

    /*public boolean isInPath() {
        boolean inPath = true;
        double pointLon, pointLat;
        double uPointLon, uPointLat;

        uPointLon = (Math.floor(userPoint.getLongitude() * 100000) / 100000.0);
        uPointLat = (Math.floor(userPoint.getLatitude() * 1000000) / 1000000.0);

        if(pathPoints.size() == 0){
            return inPath;
        }
        for (int i = 0; i < pathPoints.size(); i++) {
            pointLon = (Math.floor(pathPoints.get(i).getLongitude() * 100000) / 100000.0);
            pointLat = (Math.floor(pathPoints.get(i).getLatitude() * 1000000) / 1000000.0);

            if ((pointLon + 0.00002 >= uPointLon) || (pointLon - 0.00002 <= uPointLon)) {
                if((pointLat + 0.000003 >= uPointLat) || (pointLat - 0.000003 <= uPointLat))
                    inPath = true;
                return inPath;
            } else {
                inPath = false;
            }

        }

        return inPath;
    }*/

    class PathRequestThread extends Thread {
        TMapData tMapData = new TMapData();
        TMapPolyLine tMapPolyLine = new TMapPolyLine();
        public void run() {
            try {
                TMapMarkerItem[] markerItem = new TMapMarkerItem[2];
                for (int i = 0; i < 2; i++) {
                    markerItem[i] = new TMapMarkerItem();
                    markerItem[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                }
                markerItem[1].setIcon(r_pin);
                markerItem[0].setIcon(b_pin);
                tMapPolyLine = tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, userPoint, ePoint);
                markerItem[0].setTMapPoint(tMapPolyLine.getLinePoint().get(0));
                markerItem[1].setTMapPoint(tMapPolyLine.getLinePoint().get(tMapPolyLine.getLinePoint().size() - 1));
                tMapPolyLine.setLineWidth(20);
                tMapPolyLine.setOutLineWidth(30);
                tMapPolyLine.setOutLineColor(Color.BLUE);
                tMapPolyLine.setLineColor(Color.CYAN);
                tMapView.addMarkerItem("e", markerItem[0]);
                tMapView.addMarkerItem("s", markerItem[1]);
                tMapView.addTMapPolyLine("path", tMapPolyLine);

                pathPoints.addAll(tMapPolyLine.getLinePoint());
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
            tMapData.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, userPoint, ePoint, document -> {
                Element root = document.getDocumentElement();
                NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
                for( int i=0; i<nodeListPlacemark.getLength(); i++ ) {
                    NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();
                    for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
                        if( nodeListPlacemarkItem.item(j).getNodeName().equals("description") ) {
                            String[] description = nodeListPlacemarkItem.item(j).getTextContent().trim().split(",");
                            if(description.length == 1){
                                descriptions.add(nodeListPlacemarkItem.item(j).getTextContent().trim());
                            }
                        }
                        if(nodeListPlacemarkItem.item(j).getNodeName().equals("Point")){
                            NodeList p = nodeListPlacemarkItem.item(j).getChildNodes();
                            String[] dPoint = p.item(1).getFirstChild().getTextContent().split(",");
                            double lat = Double.parseDouble(dPoint[1]);
                            double lng = Double.parseDouble(dPoint[0]);
                            double[] coordinate = {lat, lng};
                            descriptionPoints.add(coordinate);
                            allPoints.add(coordinate);
                        }
                        if(nodeListPlacemarkItem.item(j).getNodeName().equals("LineString")){
                            NodeList lineP = nodeListPlacemarkItem.item(j).getChildNodes();
                            String[] points = lineP.item(1).getFirstChild().getTextContent().split(" ");
                            for(int l = 1; l < points.length - 1; l++){
                                String[] point = points[l].split(",");
                                double lat = Double.parseDouble(point[1]);
                                double lng = Double.parseDouble(point[0]);
                                double[] coordinate = {lat, lng};
                                allPoints.add(coordinate);
                            }

                        }
                    }
                }
            });

        }
    }
}
