package com.example.nativeopencvandroidtemplate;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        File[] files;

        String dirName = Environment.getExternalStorageDirectory() + "/crops";
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            files = dir.listFiles();
        } else {
            return;
        }

        GridView gridView = (GridView) findViewById(R.id.gridview);
        ArrayList<GalleryItem> arrayList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            GalleryItem imagemodel = new GalleryItem();
            imagemodel.setFile(files[i]);
            //add in array list
            arrayList.add(imagemodel);
        }

        GalleryAdapter adpter= new GalleryAdapter(getApplicationContext(), arrayList);
        gridView.setAdapter(adpter);
    }
}
