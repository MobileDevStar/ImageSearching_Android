package com.anna.picturematching;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class GalleryActivity extends Activity {

    RecyclerView    recyclerViewVertical;
    VerticalAdapter verticalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        recyclerViewVertical = (RecyclerView) findViewById(R.id.pic_gallery);
        recyclerViewVertical.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        if (CameraActivity.prevNames != null) {
            verticalAdapter = new VerticalAdapter(GalleryActivity.this);
            recyclerViewVertical.setAdapter(verticalAdapter);
        }
    }
}
