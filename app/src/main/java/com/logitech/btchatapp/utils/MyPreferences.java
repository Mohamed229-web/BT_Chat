package com.logitech.btchatapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferences {
    private SharedPreferences sharedPref;
    public MyPreferences(Context context) {
        sharedPref = context.getSharedPreferences(
                "my_shared_preferences", Context.MODE_PRIVATE);
    }

    public void setStarted(boolean started){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("started", true);
        editor.apply();
    }

    public boolean isStarted(){
        return sharedPref.getBoolean("started", false);
    }
}
