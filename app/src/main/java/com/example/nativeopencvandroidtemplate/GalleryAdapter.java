package com.example.nativeopencvandroidtemplate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

public class GalleryAdapter extends BaseAdapter {
    Context context;
    ArrayList<GalleryItem> arrayList;

    public GalleryAdapter(Context context, ArrayList<GalleryItem> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_crop, parent, false);
        }
        ImageView imageView;
        imageView = convertView.findViewById(R.id.image);

        File imgFile = arrayList.get(position).getFile();
        Bitmap imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        imageView.setImageBitmap(imgBitmap);

        return convertView;
    }
}