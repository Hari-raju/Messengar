package com.raju.messengar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


import androidx.appcompat.app.AppCompatActivity;

import com.raju.messengar.R;
import com.raju.messengar.databinding.ActivitySplashScreenBinding;

public class Splash_Screen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.raju.messengar.databinding.ActivitySplashScreenBinding splash = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(splash.getRoot());
        //getWindows() returns a surface
        //Windows manager is responsible for returning the window surface
        //It is simply meaning to display a full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Animation top = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        Animation bottom = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        //Setting from animation have to come
        splash.splashImageView.setAnimation(top);
        splash.splashAppName.setAnimation(bottom);

        final int DURATION_TIME = 4000;
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Splash_Screen.this, SignInActivity.class);
            startActivity(intent);
            finish();
        }, DURATION_TIME);
    }
}