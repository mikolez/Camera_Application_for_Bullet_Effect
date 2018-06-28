package com.example.user.cameraapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class VideoHighFPSActivity extends AppCompatActivity {

    public static Bitmap action;
    public static View parentLayout;
    public static SensorManager mSensorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_high_fps);

        parentLayout = findViewById(android.R.id.content);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CaptureHighSpeedVideoMode.newInstance())
                    .commit();
        }
    }
}
