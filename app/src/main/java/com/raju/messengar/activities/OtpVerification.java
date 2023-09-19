package com.raju.messengar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import com.raju.messengar.R;
import com.raju.messengar.databinding.ActivityOtpVerificationBinding;

public class OtpVerification extends AppCompatActivity {
    private ActivityOtpVerificationBinding otpBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        otpBinding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(otpBinding.getRoot());

    }
}