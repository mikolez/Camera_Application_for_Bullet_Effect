package com.example.user.cameraapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


public class TimeClient extends AsyncTask<String, Void, Boolean> {

    private Context context;
    private Boolean isConnected = false;
    private String ipAddress;

    private static final String TAG = "TimeClient";

    public TimeClient(Context context, String ipAddress) {
        this.context = context;
        this.ipAddress = ipAddress;
    }

    @Override
    protected Boolean doInBackground(String[] params) {

            try {
                Log.e(TAG, "Sending Data...");
                Socket socket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddress, 8800);
                socket.connect(address, 500);
                isConnected = socket.isConnected();

                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);

                bw.write(String.valueOf(DataActivity.timeToSend));
                bw.flush();

                socket.close();
        } catch (IOException e) {
                e.printStackTrace();
        }

        return isConnected;
}

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "The frame is sent!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Sending data...", Toast.LENGTH_SHORT).show();
            new TimeClient(context, ipAddress).execute();
        }
    }
}


