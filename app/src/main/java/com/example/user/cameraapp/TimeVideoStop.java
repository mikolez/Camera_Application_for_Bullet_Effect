package com.example.user.cameraapp;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import wseemann.media.FFmpegMediaMetadataRetriever;


public class TimeVideoStop extends AsyncTask<String, Void, String> {

    public static final int TIMEOUT = 10;
    public static final String TIME_SERVER = "pool.ntp.org";
    private long timeFromServer;
    private Activity activity;

    public TimeVideoStop(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... urls) {
//        NTPUDPClient timeClient = new NTPUDPClient();
//        InetAddress inetAddress = null;
//        try {
//            inetAddress = InetAddress.getByName(TIME_SERVER);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//        TimeInfo timeInfo = null;
//        try {
//            timeInfo = timeClient.getTime(inetAddress);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //long returnTime = timeInfo.getReturnTime();   //local device time
//        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
//        timeFromServer = returnTime;
//
//        return null;

        SntpClient client = new SntpClient();

        long start = System.currentTimeMillis();
        while (!client.requestTime("1.pool.ntp.org", TIMEOUT)) {
        }
        long end = System.currentTimeMillis();
        timeFromServer = client.getNtpTime() - (end - start);
        return null;
    }

    @Override
    protected void onPostExecute(String feed) {
        VideoActivity.videoStopTimeInMillis = timeFromServer;

        final String filePath = CaptureHighSpeedVideoMode.videoFile.getPath();

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);

        long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(CaptureHighSpeedVideoMode.VideoData, true), 1024);
            String entry = "Start: " + VideoActivity.videoStartTimeInMillis + " Stop: " + VideoActivity.videoStopTimeInMillis + " Difference: " + (VideoActivity.videoStopTimeInMillis - VideoActivity.videoStartTimeInMillis) + " Duration: " + duration + " Delta: " + (VideoActivity.videoStopTimeInMillis-VideoActivity.videoStartTimeInMillis-duration);
            out.write(entry);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(activity, "Files are saved!", Toast.LENGTH_LONG).show();
    }
}