package com.example.armap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
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

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public LinearLayout linearLayoutTmap;
    public Button searchBtn;
    public EditText searchTxt;
    public TMapView tMapView;
    public TextView placeName, placeAddr;
    public Button btnStart, btnEnd;
    public SlidingUpPanelLayout slide;
    public TMapPoint userPoint;
    public String userPointName;
    public TMapMarkerItem selectPin = new TMapMarkerItem();
    public Bitmap pin, dot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);;
        linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        searchBtn = (Button)findViewById(R.id.searchBtn);
        searchTxt = (EditText)findViewById(R.id.searchTxt);
        placeName = (TextView)findViewById(R.id.placeName);
        placeAddr = (TextView)findViewById(R.id.placeAddr);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnEnd = (Button)findViewById(R.id.btnEnd);
        slide = (SlidingUpPanelLayout)findViewById(R.id.slide);
        slide.setTouchEnabled(false);
        tMapView = new TMapView(this);

        tMapView.setSKTMapApiKey("l7xx4df6476b09fd4a12962883291fb19544");
        linearLayoutTmap.addView(tMapView);

        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.r_pin);
        dot = BitmapFactory.decodeResource(this.getResources(), R.drawable.red_dot_pin);

        searchBtn.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnEnd.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.searchBtn:
                tMapView.removeAllMarkerItem();
                String place = searchTxt.getText().toString();
                TMapData tmapdata = new TMapData();
                Handler handler = new Handler(Looper.getMainLooper());
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            ArrayList<TMapPOIItem> poiItem = tmapdata.findAllPOI(place, 50);
                            int len = poiItem.size();
                            if(len > 0){
                                TMapMarkerItem[] markerItems = new TMapMarkerItem[len];
                                String[] addrs = new String[len];
                                TMapPoint[] tMapPoints = new TMapPoint[len];
                                for (int i = 0; i < len; i++) {
                                    TMapPOIItem item = (TMapPOIItem) poiItem.get(i);
                                    markerItems[i] = new TMapMarkerItem();
                                    tMapPoints[i] = new TMapPoint(Double.parseDouble(item.frontLat), Double.parseDouble(item.frontLon));
                                    markerItems[i].setIcon(dot);
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
                                                selectPin.setIcon(dot);
                                                selectPin = arrayList.get(0);
                                                selectPin.setIcon(pin);
                                                selectPin.setPosition(0.5f, 1.3f);
                                                slide.setTouchEnabled(true);
                                                placeName.setText(arrayList.get(0).getName());
                                                String address = addrs[Integer.parseInt(arrayList.get(0).getID())];
                                                placeAddr.setText(address);
                                                tMapView.setCenterPoint(tMapPoint.getLongitude(), tMapPoint.getLatitude());
                                                slide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                                                userPoint = tMapPoint;
                                                userPointName = arrayList.get(0).getName();
                                            }
                                            return false;
                                        }
                                    });
                            }
                        }catch (NullPointerException e){
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast searchError = Toast.makeText(v.getContext(),"검색된 장소가 없습니다", Toast.LENGTH_LONG);
                                    searchError.show();
                                }
                            },0);
                        } catch (IOException | ParserConfigurationException | SAXException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                break;
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

    private Intent setUserPoint(char type, String name){
        Intent intent = new Intent(this, SelectPoint.class);
        intent.putExtra("pType", type);
        intent.putExtra("userPointName", name);
        intent.putExtra("userPointAddress", placeAddr.getText());
        intent.putExtra("userPointLat", userPoint.getLatitude());
        intent.putExtra("userPointLon", userPoint.getLongitude());
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