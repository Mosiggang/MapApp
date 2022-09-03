package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class SelectPoint extends AppCompatActivity implements View.OnClickListener {

    public EditText sPlace, ePlace, focusTxt;
    public Button sSearch, eSearch, sBtn, eBtn;
    public TMapPoint sPoint, ePoint;
    public TMapView tMapView;
    public LinearLayout linearLayoutTmap;
    public SlidingUpPanelLayout slide;
    public TextView placeName, placeAddr;
    public String[] addrs;
    public String sName, eName;
    public Double fLat, fLon;
    public Bitmap pin, dot;
    public TMapMarkerItem selectPin = new TMapMarkerItem();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_point);
        sPlace = (EditText) findViewById(R.id.sPointTxt);
        ePlace = (EditText) findViewById(R.id.ePointTxt);
        sSearch = (Button)findViewById(R.id.sSearchBtn);
        eSearch = (Button)findViewById(R.id.eSearchBtn);
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        slide = (SlidingUpPanelLayout)findViewById(R.id.slide);
        placeName = (TextView)findViewById(R.id.placeName);
        placeAddr = (TextView)findViewById(R.id.placeAddr);
        sBtn = (Button)findViewById(R.id.btnStart);
        eBtn = (Button)findViewById(R.id.btnEnd);
        slide.setTouchEnabled(false);
        tMapView = new TMapView(this);

        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        dot = BitmapFactory.decodeResource(this.getResources(), R.drawable.red_dot_pin);

        tMapView.setSKTMapApiKey("l7xx4df6476b09fd4a12962883291fb19544");
        linearLayoutTmap.addView(tMapView);
        sSearch.setOnClickListener(this);
        eSearch.setOnClickListener(this);
        sBtn.setOnClickListener(this);
        eBtn.setOnClickListener(this);
    }

    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        Double lat, lon;
        String pName, pAddr;
        char type = intent.getCharExtra("pType",'n');
        if(type != 'n'){
            lat = intent.getDoubleExtra("userPointLat",0);
            lon = intent.getDoubleExtra("userPointLon",0);
            pName = intent.getStringExtra("userPointName");
            pAddr = intent.getStringExtra("userPointAddress");
            setMarker(0, pName, lat, lon, pin);
            addrs = new String[1];
            addrs[0] = pAddr;
            setMarkerTouch();
            tMapView.setCenterPoint(lon, lat);
            switch (type){
                case 'S':
                    sPoint = setUserPoint(sPlace, pName, lat, lon);
                    sName = pName;
                    setMarker(60, sName, sPoint.getLatitude(), sPoint.getLongitude(), pin);
                    break;
                case 'E':
                    ePoint = setUserPoint(ePlace, pName, lat, lon);
                    eName = pName;
                    setMarker(61, eName, ePoint.getLatitude(), ePoint.getLongitude(), pin);
                    break;
            }
        }
        sPlace.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    focusTxt = sPlace;
                    sPlace.setText(null);
                    deleteAllMarker();
                    if(ePoint == null){
                        ePlace.setText(null);
                    }
                    if(selectPin != null){
                        Log.d("TEST", selectPin.getName());
                        selectPin.setIcon(dot);
                    }
                }
                else{
                    if(sPoint != null){
                        sPlace.setText(sName);
                    }
                    else if(sSearch.callOnClick() || linearLayoutTmap.hasFocus()){
                        sPlace.setText(sName);
                    }
                    else{
                        sPlace.setText(null);
                    }
                }
            }
        });
        ePlace.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    focusTxt = ePlace;
                    ePlace.setText(null);
                    deleteAllMarker();
                    if(sPoint == null){
                        sPlace.setText(null);
                    }
                    if(selectPin != null){
                        selectPin.setIcon(dot);
                    }
                }
                else{
                    if(ePoint != null){
                        ePlace.setText(eName);
                    }
                    else if(eSearch.callOnClick() || linearLayoutTmap.hasFocus()){
                    }
                    else{
                        ePlace.setText(null);
                    }
                }
            }
        });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sSearchBtn:
                sBtn.setVisibility(View.VISIBLE);
                eBtn.setVisibility(View.INVISIBLE);
                searchPlace(sPlace.getText().toString(), v.getContext());
                tMapView.removeMarkerItem(60+"");
                break;

            case R.id.eSearchBtn:
                sBtn.setVisibility(View.INVISIBLE);
                eBtn.setVisibility(View.VISIBLE);
                searchPlace(ePlace.getText().toString(), v.getContext());
                tMapView.removeMarkerItem(61+"");
                break;
            case R.id.btnStart:
                tMapView.removeMarkerItem(60+"");
                sName = placeName.getText().toString();
                sPoint = setUserPoint(sPlace, placeName.getText().toString(), fLat, fLon);
                setMarker(60, sName, sPoint.getLatitude(), sPoint.getLongitude(), pin);
                deleteAllMarker();
                startNavi();
                break;
            case R.id.btnEnd:
                tMapView.removeMarkerItem(61+"");
                eName = placeName.getText().toString();
                ePoint = setUserPoint(ePlace, placeName.getText().toString(), fLat, fLon);
                setMarker(61, eName, ePoint.getLatitude(), ePoint.getLongitude(), pin);
                deleteAllMarker();
                startNavi();
                break;
        }
    }

    private TMapPoint setUserPoint(EditText txt, String name, Double lat, Double lon){
        TMapPoint point = new TMapPoint(lat, lon);
        txt.setText(name);
        return point;
    }
    private void searchPlace(String userPlace, Context context){
        deleteAllMarker();
        String place = userPlace;
        TMapData tmapdata = new TMapData();
        Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<TMapPOIItem> poiItem = tmapdata.findAllPOI(place, 50);
                    int len = poiItem.size();
                    if (len > 0) {
                        addrs = new String[len];
                        for (int i = 0; i < len; i++) {
                            TMapPOIItem item = (TMapPOIItem) poiItem.get(i);
                            addrs[i] = item.getPOIAddress();
                            setMarker(i, item.getPOIName(), Double.parseDouble(item.frontLat), Double.parseDouble(item.frontLon), dot);
                            if(i == 0){
                                tMapView.setCenterPoint(Double.parseDouble(item.frontLon), Double.parseDouble(item.frontLat));
                            }
                        }
                    }
                }catch (NullPointerException e){
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast searchError = Toast.makeText(context,"검색된 장소가 없습니다", Toast.LENGTH_LONG);
                            searchError.show();
                        }
                    },0);
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void setMarker(int id,String name, Double lat, Double lon, Bitmap img){
        TMapMarkerItem markerItem = new TMapMarkerItem();
        markerItem.setIcon(img);
        markerItem.setPosition(0.5f, 1.0f); // 마커의 중심점을 중앙, 하단으로 설정
        markerItem.setTMapPoint(new TMapPoint(lat, lon)); // 마커의 좌표 지정
        markerItem.setCalloutTitle(name);
        markerItem.setName(name); // 마커의 타이틀 지정
        markerItem.setCanShowCallout(false);
        tMapView.addMarkerItem(id + "", markerItem); // 지도에 마커 추가
        tMapView.setZoomLevel(15);
    }

    private void setMarkerTouch(){
        tMapView.setOnClickListenerCallBack(new TMapView.OnClickListenerCallback() {
            @Override
            public boolean onPressEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
                return false;
            }
            @Override
            public boolean onPressUpEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
                if(arrayList.size() > 0 && !selectPin.equals(arrayList.get(0))){
                    selectPin.setIcon(dot);
                    selectPin = arrayList.get(0);
                    selectPin.setPosition(0.5f, 1.3f);
                    slide.setTouchEnabled(true);
                    selectPin.setIcon(pin);
                    focusTxt.setText(arrayList.get(0).getName());
                    placeName.setText(arrayList.get(0).getName());
                    placeAddr.setText(addrs[Integer.parseInt(arrayList.get(0).getID())]);
                    tMapView.setCenterPoint(tMapPoint.getLongitude(), tMapPoint.getLatitude());
                    slide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    fLat = tMapPoint.getLatitude();
                    fLon = tMapPoint.getLongitude();
                }
                return false;
            }
        });
    }
    void deleteAllMarker(){
        for(int i= 0; i< 50; i++){
            tMapView.removeMarkerItem(i+"");
        }
    }
    void startNavi(){
        if(sPoint != null && ePoint != null){
            Intent intent = new Intent(this, StartNavi.class);
            intent.putExtra("sName", sName);
            intent.putExtra("sLat", sPoint.getLatitude());
            intent.putExtra("sLon", sPoint.getLongitude());
            intent.putExtra("eName", eName);
            intent.putExtra("eLat", ePoint.getLatitude());
            intent.putExtra("eLon", ePoint.getLongitude());
            startActivity(intent);
        }
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
                    imm.hideSoftInputFromWindow(focusView.getWindowToken(),0);
                }
                focusView.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}