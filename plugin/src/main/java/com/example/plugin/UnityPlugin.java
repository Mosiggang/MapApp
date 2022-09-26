package com.example.plugin;
import android.app.Activity;
import android.widget.Toast;
import com.unity3d.player.UnityPlayer;

public class UnityPlugin{
    private static UnityPlugin _instance;

    private static Activity _context;

    public static UnityPlugin instance(){
        if(_instance == null){
            _instance = new UnityPlugin();
            _context = UnityPlayer.currentActivity;
        }
        return _instance;
    }
    public String getPackageName(){
        return _context.getPackageName();
    }
    public void showToast(){
        _context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(_context, "UNITY ACTIVATED", Toast.LENGTH_LONG).show();
            }
        });
    }
    public void unitySendMessage(String objectName, String methodName, String param){
        UnityPlayer.UnitySendMessage(objectName,methodName,param);
    }
}
