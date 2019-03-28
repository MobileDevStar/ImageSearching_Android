package com.anna.picturematching;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public  class ImageLoader extends AsyncTask<String, Void, Bitmap> {

    ImageView           mView;
    CircleImageView     mCircleView;
    boolean             mGhost;

    public ImageLoader(ImageView view, CircleImageView circle, boolean ghost) {
        mView = view;
        mCircleView = circle;
        mGhost = ghost;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        String imageName = strings[0];

        String simPath = Environment.getExternalStorageDirectory().getPath() + "/" + imageName;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(new File(simPath));
            int imgLen = fin.available();
            byte[] matchData = new byte[imgLen];
            fin.read(matchData, 0, imgLen);
            fin.close();
            Log.e("++++++++++++++", imageName);

            Bitmap image = BitmapFactory.decodeByteArray(matchData, 0, matchData.length);
            return image;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap image) {
        if (image != null) {
            if (mGhost) {
                mCircleView.setImageBitmap(image);
                mCircleView.setVisibility(View.VISIBLE);
                mCircleView.setAlpha((float)0.5);
            } else {
                mView.setImageBitmap(image);
            }
        } else {
        }
    }
}