package com.anna.picturematching;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


public class MainActivity extends Activity{
    private static final String TAG = "PictureMatching::Activity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    Button mButUser1;
    Button mButUser2;

    boolean blGranted = true;

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mButUser1 = (Button) findViewById(R.id.but_user1);

        mButUser1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (blGranted) {
                    Intent newIntent = new Intent(MainActivity.this, CameraActivity.class);
                    newIntent.putExtra("taking", true);
                    startActivity(newIntent);
                }
            }
        });

        mButUser2 = (Button) findViewById(R.id.but_user2);

        mButUser2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blGranted) {
                    Intent newIntent = new Intent(MainActivity.this, GalleryActivity.class);
                    startActivity(newIntent);
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            blGranted = false;
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                blGranted = true;
            } else {
                blGranted = false;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
