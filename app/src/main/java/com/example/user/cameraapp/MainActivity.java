package com.example.user.cameraapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button videoButton;
    private Button dataButton;
//    private Button ipButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoButton = (Button) findViewById(R.id.videoButton);
        dataButton = (Button) findViewById(R.id.dataButton);
//        ipButton = (Button) findViewById(R.id.ipButton);

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoButton();
            }
        });
        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDataButton();
            }
        });
//        ipButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onIpButton();
//            }
//        });

        File path = getApplicationContext().getExternalFilesDir(null);
        File ipAddress = new File(path, "ipAddress.txt");

        if (!ipAddress.exists()) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(ipAddress, true), 1024);
                out.write("");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = blueAdapter.getBondedDevices();

                if(bondedDevices.size() > 0) {
                    Object[] devices = (Object[]) bondedDevices.toArray();
                    BluetoothDevice device = (BluetoothDevice) devices[0];
                    System.out.println(device.getName());
                    ParcelUuid[] uuids = device.getUuids();
                    BluetoothSocket socket = null;
                    try {
                        socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                        socket.connect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void onVideoButton() {
        Intent videoIntent = new Intent(this, VideoHighFPSActivity.class);
        startActivity(videoIntent);
    }

    private void onDataButton() {
        Intent dataIntent = new Intent(this, DataActivity.class);
        startActivity(dataIntent);
    }

    private void onIpButton() {
        Intent dataIntent = new Intent(this, ipAddressActivity.class);
        startActivity(dataIntent);
    }
}
