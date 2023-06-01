package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
    public String sName, eName, searchPlace;
    public Double fLat, fLon;
    public Bitmap pin, dot, sPin;
    public TMapMarkerItem selectPin = new TMapMarkerItem();
    public int searchChecker = 0;
    public char type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SELECT POINT","HEY CREATE");
        setContentView(R.layout.activity_select_point);
        sPlace = (EditText) findViewById(R.id.sPointTxt);
        ePlace = (EditText) findViewById(R.id.ePointTxt);
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        slide = (SlidingUpPanelLayout)findViewById(R.id.slide);
        placeName = (TextView)findViewById(R.id.placeName);
        placeAddr = (TextView)findViewById(R.id.placeAddr);
        sBtn = (Button)findViewById(R.id.btnStart);
        eBtn = (Button)findViewById(R.id.btnEnd);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("T MAP API KEY HERE");
        linearLayoutTmap.addView(tMapView);
        slide.setTouchEnabled(false);
        slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        sPin = BitmapFactory.decodeResource(this.getResources(), R.drawable.b_pin);
        dot = BitmapFactory.decodeResource(this.getResources(), R.drawable.red_dot_pin);

        sBtn.setOnClickListener(this);
        eBtn.setOnClickListener(this);

        sPlace.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchPlace = sPlace.getText().toString();
                sBtn.setVisibility(View.VISIBLE);
                eBtn.setVisibility(View.INVISIBLE);
                searchPlaceFunc(searchPlace, v.getContext(), "start");
                sPoint = null;
                tMapView.removeMarkerItem(101+"");
                return false;
            }
        });
        ePlace.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchPlace = ePlace.getText().toString();
                sBtn.setVisibility(View.INVISIBLE);
                eBtn.setVisibility(View.VISIBLE);
                ePlace.setText(searchPlace);
                searchPlaceFunc(searchPlace, v.getContext(), "end");
                ePoint = null;
                tMapView.removeMarkerItem(102+"");
                return false;
            }
        });
    }
    protected void onRestart() {
        super.onRestart();
        Log.d("SELECT POINT","HEY RESTART");
        /*sPoint = ePoint = null;
        sName = eName = null;
        placeName.setText(null);
        placeAddr.setText(null);
        focusTxt = null;
        sPlace.setText(null);
        ePlace.setText(null);
        slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        slide.setTouchEnabled(false);
        tMapView.removeAllMarkerItem();
        getIntent().removeExtra("pType");
        getIntent().getExtras().clear();*/
    }

    protected void onStart() {
        super.onStart();
        Log.d("SELECT POINT","HEY START");
        Intent intent = getIntent();
        double lat, lon;
        String pName, pAddr;
        //tMapView.removeAllMarkerItem();
        tMapView.setPathRotate(true);
        tMapView.setRotateEnable(true);
        type = intent.getCharExtra("pType",'n');
        if(type != 'n'){
            lat = intent.getDoubleExtra("userPointLat",0);
            lon = intent.getDoubleExtra("userPointLon",0);
            pName = intent.getStringExtra("userPointName");
            pAddr = intent.getStringExtra("userPointAddress");
            addrs = new String[1];
            addrs[0] = pAddr;
            setMarkerTouch();
            tMapView.setCenterPoint(lon, lat);
            switch (type){
                case 'S':
                    sPoint = setUserPoint(sPlace, pName, lat, lon);
                    sName = pName;
                    setMarker(101, sName, sPoint.getLatitude(), sPoint.getLongitude(), sPin);
                    break;
                case 'E':
                    ePoint = setUserPoint(ePlace, pName, lat, lon);
                    eName = pName;
                    setMarker(102, eName, ePoint.getLatitude(), ePoint.getLongitude(), pin);
                    break;
            }
            getIntent().getExtras().clear();
        }
        else{
            setMarkerTouch();
        }
        sPlace.setOnFocusChangeListener((v, hasFocus) -> {

            if(hasFocus){
                slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                slide.setTouchEnabled(false);
                placeName.setText("장소를 선택해주세요");
                placeAddr.setText("장소를 선택해주세요");
                focusTxt = sPlace;
                sPlace.setText(null);
                deleteAllMarker();
                if(ePoint == null){
                    ePlace.setText(null);
                }
                if(selectPin != null){
                    selectPin.setIcon(dot);
                }
            }
        });
        ePlace.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                slide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                slide.setTouchEnabled(false);
                placeName.setText("장소를 선택해주세요");
                placeAddr.setText("장소를 선택해주세요");
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
        });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnStart:
                tMapView.removeMarkerItem(101+"");
                sName = placeName.getText().toString();
                sPoint = setUserPoint(sPlace, placeName.getText().toString(), fLat, fLon);
                setMarker(101, sName, sPoint.getLatitude(), sPoint.getLongitude(), sPin);
                deleteAllMarker();
                startNavi();
                break;
            case R.id.btnEnd:
                tMapView.removeMarkerItem(102+"");
                eName = placeName.getText().toString();
                ePoint = setUserPoint(ePlace, placeName.getText().toString(), fLat, fLon);
                setMarker(102, eName, ePoint.getLatitude(), ePoint.getLongitude(), pin);
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
    private void searchPlaceFunc(String userPlace, Context context, String type){
        deleteAllMarker();
        String place = userPlace;
        TMapData tmapdata = new TMapData();
        Handler handler = new Handler(Looper.getMainLooper());
        Thread thread = new Thread(() -> {
            try {
                ArrayList<TMapPOIItem> poiItem = new ArrayList<>();
                if(type == "start"){
                    setMarker(102, eName, ePoint.getLatitude(), ePoint.getLongitude(), pin);
                    poiItem = tmapdata.findAroundKeywordPOI(ePoint,place, 33,100);
                }
                else{
                    Log.d("SELEC", "EPOINT &" + place);
                    setMarker(101, sName, sPoint.getLatitude(), sPoint.getLongitude(), sPin);
                    poiItem = tmapdata.findAroundKeywordPOI(sPoint,place, 33,100);
                }
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
                handler.postDelayed(() -> {
                    Toast searchError = Toast.makeText(context,"검색된 장소가 없습니다", Toast.LENGTH_LONG);
                    searchError.show();
                },0);
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
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
        for(int i= 0; i< 100; i++){
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
            intent.putExtra("PathType", "S2D");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }
}