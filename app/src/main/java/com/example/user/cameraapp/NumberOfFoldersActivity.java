package com.example.user.cameraapp;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

public class NumberOfFoldersActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number_of_folders);

        Intent mIntent = getIntent();
        String activity = mIntent.getStringExtra("Activity");

        Log.e("Folders: ", activity);

        textView = (TextView) findViewById(R.id.textView);

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir;

        if (activity.contains("Camera")) {
            myDir = new File(root + "/BulletEffectVideoData/");
            Log.e("Folders: ", "Camera");
        } else {
            myDir = new File(root + "/BulletEffectMSD/");
            Log.e("Folders: ", "Data");
        }

        File[] files = myDir.listFiles();
        int numberOfFolders = files.length;

        File lastFolder = files[files.length - 1];
        String nameOfLastFolder = lastFolder.getName();

        File[] lastFolderFiles = lastFolder.listFiles();
        int numberOfFilesInLastFolder = lastFolderFiles.length;

        textView.setText("Number Of Folders: " + numberOfFolders + "\n" + "Name Of The Last Folder: "
                + nameOfLastFolder + "\n" + "Number Of Files In The Last Folder: " + numberOfFilesInLastFolder);

    }
}
