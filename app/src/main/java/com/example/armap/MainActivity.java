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
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, TMapGpsManager.onLocationChangedCallback{
    public LinearLayout linearLayoutTmap;
    public EditText searchTxt;
    public TMapView tMapView;
    public TextView placeName, placeAddr;
    public Button btnStart, btnEnd;
    public SlidingUpPanelLayout slide;
    public TMapPoint userPoint, cLocation;
    public String userPointName;
    public TMapMarkerItem selectPin = new TMapMarkerItem(), cPin;
    public Bitmap pin, r_dot, b_dot;
    public SensorManager sensorManager;
    public Sensor acc, mag;
    private final float[] accRead = new float[3];
    private final float[] magRead = new float[3];
    private final float[] rMatrix = new float[9];
    private final float[] oAngles = new float[3];
    public TMapGpsManager gps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MAIN ACTIVITY","WOW CREATE");
        setContentView(R.layout.activity_main);
        tMapView = new TMapView(this);
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        searchTxt = (EditText)findViewById(R.id.searchTxt);
        placeName = (TextView)findViewById(R.id.placeName);
        placeAddr = (TextView)findViewById(R.id.placeAddr);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnEnd = (Button)findViewById(R.id.btnEnd);
        slide = (SlidingUpPanelLayout)findViewById(R.id.slide);
        tMapView.setSKTMapApiKey("l7xx4df6476b09fd4a12962883291fb19544");
        linearLayoutTmap.addView(tMapView);
        slide.setTouchEnabled(false);
        slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        r_dot = BitmapFactory.decodeResource(this.getResources(), R.drawable.red_dot_pin);
        b_dot = BitmapFactory.decodeResource(this.getResources(), R.drawable.direction);
        btnStart.setOnClickListener(this);
        btnEnd.setOnClickListener(this);
        searchTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                tMapView.removeAllMarkerItem();
                String place = searchTxt.getText().toString();
                TMapData tmapdata = new TMapData();
                Handler handler = new Handler(Looper.getMainLooper());
                Log.d("START POI:", place);

                Thread thread = new Thread(() -> {
                    try{
                        ArrayList<TMapPOIItem> poiItem = tmapdata.findAroundKeywordPOI(cLocation,place, 33,100);
                        Log.d("AROUND POI:", place + ", " + poiItem.size());
                        int len = poiItem.size();
                        if(len > 0){
                            TMapMarkerItem[] markerItems = new TMapMarkerItem[len];
                            String[] addrs = new String[len];
                            TMapPoint[] tMapPoints = new TMapPoint[len];
                            for (int i = 0; i < len; i++) {
                                TMapPOIItem item = (TMapPOIItem) poiItem.get(i);
                                markerItems[i] = new TMapMarkerItem();
                                tMapPoints[i] = new TMapPoint(Double.parseDouble(item.frontLat), Double.parseDouble(item.frontLon));
                                markerItems[i].setIcon(r_dot);
                                markerItems[i].setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
                                markerItems[i].setTMapPoint(tMapPoints[i]); // 마커의 좌표 지정
                                markerItems[i].setCalloutTitle(item.getPOIName());
                                addrs[i] = item.getPOIAddress();
                                markerItems[i].setName(item.getPOIName()); // 마커의 타이틀 지정
                                markerItems[i].setCanShowCallout(false);
                                tMapView.addMarkerItem(i + "", markerItems[i]); // 지도에 마커 추가
                            }
                            tMapView.setZoomLevel(15);
                            tMapView.setCenterPoint(tMapPoints[0].getLongitude(), tMapPoints[0].getLatitude());
                            tMapView.setOnClickListenerCallBack(new TMapView.OnClickListenerCallback() {
                                @Override
                                public boolean onPressEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
                                    return false;
                                }

                                @Override
                                public boolean onPressUpEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
                                    if(arrayList.size() > 0 && !selectPin.equals(arrayList.get(0))) {
                                        selectPin.setIcon(r_dot);
                                        selectPin = arrayList.get(0);
                                        selectPin.setIcon(pin);
                                        selectPin.setPosition(0.5f, 1.3f);
                                        slide.setTouchEnabled(true);
                                        placeName.setText(arrayList.get(0).getName());
                                        if(!Objects.equals(arrayList.get(0).getID(), "user")){
                                            String address = addrs[Integer.parseInt(arrayList.get(0).getID())];
                                            placeAddr.setText(address);
                                            tMapView.setCenterPoint(tMapPoint.getLongitude(), tMapPoint.getLatitude());
                                            slide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                                            userPoint = tMapPoint;
                                            userPointName = arrayList.get(0).getName();
                                        }
                                    }
                                    return false;
                                }
                            });

                        }
                    }catch (NullPointerException e){
                        handler.postDelayed(() -> {
                            Toast searchError = Toast.makeText(v.getContext(),"검색된 장소가 없습니다", Toast.LENGTH_LONG);
                            searchError.show();
                        },0);
                    } catch (IOException | ParserConfigurationException | SAXException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
                return false;
            }
        });
        cPin = new TMapMarkerItem();
        cPin.setIcon(b_dot);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        setUserLocation();

    }

    protected void onRestart() {
        super.onRestart();
        Log.d("MAIN ACTIVITY","WOW RESTART");
    }
    protected void onStart() {
        super.onStart();
        tMapView.removeAllMarkerItem();

        placeName.setText(null);
        placeAddr.setText(null);
        searchTxt.setText(null);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        setUserLocation();
        Log.d("MAIN ACTIVITY","WOW START");
        tMapView.setMarkerRotate(false);
        tMapView.setPOIRotate(false);
        tMapView.setRotateEnable(true);
        cPin.setIcon(b_dot);
        tMapView.setCenterPoint(cLocation.getLongitude(), cLocation.getLatitude());
        tMapView.addMarkerItem("user", cPin);

    }

    protected void onResume() {
        super.onResume();
        slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        slide.setTouchEnabled(false);
        Log.d("MAIN ACTIVITY","WOW RESUME");
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (acc != null) {
            sensorManager.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        if (mag != null) {
            sensorManager.registerListener((SensorEventListener) this, mag, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }
    protected void onPause() {
        super.onPause();
        Log.d("MAIN ACTIVITY","WOW PAUSE");
        gps.CloseGps();
        sensorManager.unregisterListener((SensorEventListener) this);
    }
    protected void setUserLocation(){
        //tMapView = new TMapView(this);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) { //위치 권한 확인
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                Double userLon = location.getLongitude(); // 위도
                Double userLat = location.getLatitude(); // 경도
                cLocation = new TMapPoint(userLat, userLon);
                if (userPoint != null) {
                    cPin.setTMapPoint(userPoint);
                    tMapView.setCenterPoint(userLon, userLat);
                    tMapView.addMarkerItem("user", cPin);
                }
            }
        }
        gps = new TMapGpsManager(this);
        gps.setMinTime(1000);
        gps.setMinDistance(5);
        gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
        gps.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        gps.OpenGps();
        cLocation = gps.getLocation();
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accRead, 0, accRead.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magRead, 0, magRead.length);
        }
        if(accRead != null && magRead != null){
            updateOrientationAngles();
            try {
                tMapView.removeMarkerItem("user");
                Bitmap newPin = getRotatedBitmap(b_dot, (float) Math.toDegrees(oAngles[0]) - 90);
                cPin.setIcon(newPin);
                tMapView.addMarkerItem("user",cPin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public Bitmap getRotatedBitmap(Bitmap bitmap, float degrees){
        if(bitmap == null) return null;
        if (degrees == 0.0f) return bitmap;
        Matrix m = new Matrix();
        m.setRotate(degrees + tMapView.getRotate(), (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateOrientationAngles() {
        boolean success;
        success = SensorManager.getRotationMatrix(rMatrix, null, accRead, magRead);
        if(success){
            SensorManager.getOrientation(rMatrix, oAngles);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.btnStart:
                Intent sIntent = setUserPoint('S', userPointName);
                startActivity(sIntent);
                break;
            case R.id.btnEnd:
                Intent eIntent = setUserPoint('E', userPointName);
                startActivity(eIntent);
                break;
        }
    }

    @Override
    public void onLocationChange(Location location) {
        double userLat = location.getLatitude();
        double userLon = location.getLongitude();
        cLocation = gps.getLocation();
        cPin.setTMapPoint(cLocation);
        tMapView.setCenterPoint(userLon, userLat);
        tMapView.setLocationPoint(userLon,userLat);
    }


    private Intent setUserPoint(char type, String name){
        Intent intent;
        if(type == 'E'){//바로 도착을 눌렀을때 바로 경로 찾아줌, StartNavi의 인텐트도 고쳐야함
            intent = new Intent(this, StartNavi.class);
            intent.putExtra("sName", "현재위치");
            intent.putExtra("eName", name);
            intent.putExtra("sLat", cLocation.getLatitude());
            intent.putExtra("sLon", cLocation.getLongitude());
            intent.putExtra("eLat", userPoint.getLatitude());
            intent.putExtra("eLon", userPoint.getLongitude());
            intent.putExtra("PathType", "C2D");
        }
        else{
            intent = new Intent(this, SelectPoint.class);
            intent.putExtra("pType", type);
            intent.putExtra("userPointName", name);
            intent.putExtra("userPointAddress", placeAddr.getText());
            intent.putExtra("userPointLat", userPoint.getLatitude());
            intent.putExtra("userPointLon", userPoint.getLongitude());
        }
        return intent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View focusView = getCurrentFocus();
        if(focusView != null){
            Rect rect = new Rect();
            focusView.getGlobalVisibleRect(rect);
            int x = (int)ev.getX();
            int y = (int)ev.getY();
            if(!rect.contains(x, y)){
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                if(imm != null){
                    placeName.getText();
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(),0);
                }
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

}