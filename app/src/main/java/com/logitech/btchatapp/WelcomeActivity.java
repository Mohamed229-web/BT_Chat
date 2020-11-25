package com.logitech.btchatapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.logitech.btchatapp.callback.NextCallback;
import com.logitech.btchatapp.ui.main.WelcomeFragment;
import com.logitech.btchatapp.utils.MyPreferences;

public class WelcomeActivity extends AppCompatActivity implements NextCallback {
    private static final String TAG = "WelcomeActivity";
    private MyPreferences myPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myPreferences = new MyPreferences(this);

        if (myPreferences.isStarted()){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_welcome);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, WelcomeFragment.newInstance(1))
                    .commitNow();
        }
    }

    @Override
    public void doAction(int mode) {
        if (mode == 1){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, WelcomeFragment.newInstance(2))
                    .commitNow();
        }else{
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            myPreferences.setStarted(true);
            finish();
        }
    }
}
