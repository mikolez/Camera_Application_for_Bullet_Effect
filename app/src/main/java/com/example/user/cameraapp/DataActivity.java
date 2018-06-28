package com.example.user.cameraapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataActivity extends AppCompatActivity {

    public static final float PEAK_THRESHOLD = 10;
    public static final long TRIM_THRESHOLD = 1500;

    private Button startButton;
    private Button stopButton;
    private static SensorManager mSensorManager;
    private static SensorEventListener sensorEventListener;
    public static long dataStartTimeInMillis;
    public static long dataStopTimeInMillis;
    public static long timeToSend;
    private long endOfJump = 0;
    private long startOfJump = 0;
    private ArrayList<Float> accelerometerData;
    private ArrayList<Long> timeData;
    public static File GeneralData;
    public static File VerticalAccelerationData;
    private static long lastUpdate = System.currentTimeMillis();
    private boolean isDataRecording = false;
    public static String ipAddress;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean isScreenOn = true;

    private static File rotationMatrixFile;
    ArrayList<float[]> rotationMatrixData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        startButton = (Button) findViewById(R.id.startButton);
//        stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDataRecording) {
                    startButton.setText("Stop");
//                    startButton.setBackgroundColor(Color.RED);
//                    startButton.setTextColor(Color.BLACK);
                    onStartButton();
                } else {
                    startButton.setText("Start");
//                    float[] colorList = {0x3F, 0x51, 0xB5};
//                    startButton.setBackgroundColor(Color.HSVToColor(colorList));
                    onStopButton();
                }
            }
        });
//        stopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onStopButton();
//            }
//        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);

        accelerometerData = new ArrayList<>();
        timeData = new ArrayList<>();
        rotationMatrixData = new ArrayList<>();
    }

    private void onStartButton() {
        if (!isDataRecording) {
            isDataRecording = true;
            new TimeDataStart(this).execute();
//            Toast.makeText(getApplicationContext(), "Data recording started!", Toast.LENGTH_SHORT).show();

            final long startTime = System.currentTimeMillis();

            sensorEventListener = new SensorEventListener() {
                float accelerometer_x, accelerometer_y, accelerometer_z;
                float gravity_x, gravity_y, gravity_z;
                float verticalAcceleration;

                float[] mGravity;
                float[] mGeomagnetic;
                float[] R;

                @Override
                public void onSensorChanged(SensorEvent event) {
                    long currentTime = System.currentTimeMillis();
                    if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        accelerometer_x = event.values[0];
                        accelerometer_y = event.values[1];
                        accelerometer_z = event.values[2];
                    }
                    if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                        gravity_x = -event.values[0];
                        gravity_y = -event.values[1];
                        gravity_z = -event.values[2];
                    }
                    if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {

                        if (event.values[0] == 0 && isScreenOn == true) {
//                            turnOffScreen();
                        }

                        if (event.values[0] != 0 && isScreenOn == false) {
//                            turnOnScreen();
                        }
                    }

                    float accelerometer_norm = (float) Math.sqrt(accelerometer_x * accelerometer_x + accelerometer_y * accelerometer_y + accelerometer_z * accelerometer_z);
                    float gravity_norm = (float) Math.sqrt(gravity_x * gravity_x + gravity_y * gravity_y + gravity_z * gravity_z);
                    float cosine = (accelerometer_x * gravity_x + accelerometer_y * gravity_y + accelerometer_z * gravity_z) / (gravity_norm * accelerometer_norm);
                    verticalAcceleration = accelerometer_norm * Math.abs(cosine);
                    float horizontalAcceleration = (float) Math.sqrt(accelerometer_norm * accelerometer_norm - verticalAcceleration * verticalAcceleration);

                    if (Float.isNaN(verticalAcceleration)) {
                        verticalAcceleration = 0;
                    }

                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                        mGravity = event.values;
                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                        mGeomagnetic = event.values;
                    if (mGravity != null && mGeomagnetic != null) {
                        R = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                        if (success) {
                        }
                    }

                    accelerometerData.add(verticalAcceleration);
                    timeData.add(currentTime - startTime);
                    rotationMatrixData.add(R);

                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void onStopButton() {
        if (isDataRecording) {
            isDataRecording = false;
            mSensorManager.unregisterListener(sensorEventListener);
            new TimeDataStop(this).execute();

            String root = Environment.getExternalStorageDirectory().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentDateAndTime = sdf.format(new Date());
            File myDir = new File(root + "/BulletEffectMSD/" + currentDateAndTime);
            myDir.mkdirs();

            GeneralData = new File(myDir, "GeneralData.txt");
            VerticalAccelerationData = new File(myDir, "VerticalAccelerationData.txt");
            rotationMatrixFile = new File(myDir, "rotationMatrixFile.txt");

            for (int i = 0; i < accelerometerData.size(); i++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(VerticalAccelerationData, true), 1024);
                    String entry = accelerometerData.get(i) + " " + timeData.get(i) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < rotationMatrixData.size(); i++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(rotationMatrixFile, true), 1024);
                    String entry = "";
                    if (rotationMatrixData.get(i) == null) {
                        continue;
                    }
                    for (int j = 0; j < rotationMatrixData.get(i).length; j++) {
                        entry += rotationMatrixData.get(i)[j] + " ";
                    }
                    entry += timeData.get(i) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            int startOfJumpIndex = 0, endOfJumpIndex = 0;

            float min = accelerometerData.get(0), max = accelerometerData.get(0), maxIntegration = accelerometerData.get(0);
            int min_index = 0, max_index = 0, maxIntegrationIndex = 0;

            for (int i = 0; i < accelerometerData.size(); i++) {
                if (accelerometerData.get(i) > max) {
                    max = accelerometerData.get(i);
                    max_index = i;
                }
            }

            long maxTime = timeData.get(max_index);
            int trimStartIndex = 0, trimEndIndex = 0;

            for (int i = max_index; i >= 0; i--) {
                if (maxTime - timeData.get(i) >= TRIM_THRESHOLD) {
                    trimStartIndex = i;
                    break;
                }
            }

            for (int i = max_index; i < accelerometerData.size(); i++) {
                if (timeData.get(i) - maxTime >= TRIM_THRESHOLD) {
                    trimEndIndex = i;
                    break;
                }
            }

            ArrayList<Float> accelerometerDataNew = new ArrayList<>(accelerometerData.subList(trimStartIndex, trimEndIndex));
            ArrayList<Long> timeDataNew = new ArrayList<>(timeData.subList(trimStartIndex, trimEndIndex));

//
//        for (int i = 0; i < max_index - 10; i++) {
//            if (accelerometerData.get(i) > min) {
//                min = accelerometerData.get(i);
//                min_index = i;
//            }
//        }


            // Get an array of peaks
            ArrayList<Integer> peaks = new ArrayList<Integer>();
            peaks = detectPeaks(accelerometerDataNew, PEAK_THRESHOLD);


            // Find a peak with maximum cumulative sum from the left
            float maxValueLeft = calculateCumulativeSumFromLeft(accelerometerDataNew, peaks.get(0));
            int maxCumulativeSumIndexLeft = peaks.get(0);
            for (int i : peaks) {
                if (maxValueLeft < calculateCumulativeSumFromLeft(accelerometerDataNew, i)) {
                    maxValueLeft = calculateCumulativeSumFromLeft(accelerometerDataNew, i);
                    maxCumulativeSumIndexLeft = i;
                }
            }


            // Find a start of jump index by looking for a maximum in the subarray of accelerometerData ([:maxCumulativeSumIndexLeft + 1])
            float helpMax = accelerometerDataNew.get(0);
            int helpMaxIndex = 0;
            for (int i = 0; i < maxCumulativeSumIndexLeft + 1; i++) {
                if (accelerometerDataNew.get(i) > helpMax) {
                    helpMax = accelerometerDataNew.get(i);
                    helpMaxIndex = i;
                }
            }
            startOfJumpIndex = helpMaxIndex;


            // Find a max point (which is the endOfJumpIndex) before the cumulative point from the left
            float helpMaxEnd = accelerometerDataNew.get(helpMaxIndex + 1);
            int helpMaxIndexEnd = helpMaxIndex + 1;
            for (int i = helpMaxIndex + 1; i < accelerometerDataNew.size(); i++) {
                if (helpMaxEnd < accelerometerDataNew.get(i)) {
                    helpMaxEnd = accelerometerDataNew.get(i);
                    helpMaxIndexEnd = i;
                }
            }
            endOfJumpIndex = helpMaxIndexEnd;


//            // Find a peak with maximum cumulative sum from the right
//            float maxValueRight = calculateCumulativeSumFromRight(accelerometerData, peaks.get(0));
//            int maxCumulativeSumIndexRight = peaks.get(0);
//            for (int i : peaks) {
//                if (maxValueRight < calculateCumulativeSumFromRight(accelerometerData, i)) {
//                    maxValueRight = calculateCumulativeSumFromRight(accelerometerData, i);
//                    maxCumulativeSumIndexRight = i;
//                }
//            }
//
//            // Find a start of jump index by looking for a maximum in the subarray of accelerometerData ([maxCumulativeSumIndexRight:])
//            helpMax = accelerometerData.get(accelerometerData.size() - 1);
//            helpMaxIndex = accelerometerData.size() - 1;
//            for (int i = accelerometerData.size() - 1; i >= maxCumulativeSumIndexRight; i--) {
//                if (accelerometerData.get(i) > helpMax) {
//                    helpMax = accelerometerData.get(i);
//                    helpMaxIndex = i;
//                }
//            }
//            endOfJumpIndex = helpMaxIndex;


            startOfJump = timeDataNew.get(startOfJumpIndex);
            endOfJump = timeDataNew.get(endOfJumpIndex);

            long middle = (startOfJump + endOfJump) / 2;
            timeToSend = dataStartTimeInMillis + middle;

            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(GeneralData, true), 1024);
                String entry = "Start: " + dataStartTimeInMillis + " Stop: " + dataStopTimeInMillis + " Difference: " + (dataStopTimeInMillis - dataStartTimeInMillis) + " Highest Point: " + timeToSend;
                out.write(entry);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Toast.makeText(getApplicationContext(), "Data is saved!", Toast.LENGTH_LONG).show();

            startButton.setText("Blocked");
            startButton.setEnabled(false);

            Intent intent = new Intent(getApplicationContext(), NumberOfFoldersActivity.class);
            intent.putExtra("Activity", "Data");
            startActivity(intent);

//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    new TimeClient(getApplicationContext(), "143.248.229.39").execute();
//                    new TimeClient(getApplicationContext(), "143.248.229.42").execute();
//                }
//            }, 1500);
        }
    }

    private ArrayList<Integer> detectPeaks(ArrayList<Float> data, Float threshold) {

        ArrayList<Integer> indices = new ArrayList<Integer>();

        for (Float d: data) {
            if (threshold >= 0) {
                if (d >= threshold) {
                    indices.add(data.indexOf(d));
                }
            } else {
                if (d <= threshold) {
                    indices.add(data.indexOf(d));
                }
            }
        }
        return indices;
    }

    private float calculateCumulativeSumFromLeft(ArrayList<Float> data, int ind) {

        float sum = 0;

        for (int i = 0; i <= ind; i++) {
            sum += data.get(i);
        }

        return sum;
    }

    private float calculateCumulativeSumFromRight(ArrayList<Float> data, int ind) {

        float sum = 0;

        for (int i = data.size() - 1; i >= ind; i--) {
            sum += data.get(i);
        }

        return sum;
    }

    public void turnOnScreen(){
        // turn on screen
        Log.v("ProximityActivity", "ON!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
        mWakeLock.acquire();
        isScreenOn = true;
    }

    public void turnOffScreen(){
        // turn off screen
        Log.v("ProximityActivity", "OFF!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
        mWakeLock.acquire();
        isScreenOn = false;
    }
}
