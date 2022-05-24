package com.example.trialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import java.io.File;

public class DisplayImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_image);
        Intent intent = getIntent();
        int x = intent.getIntExtra("RECT_X",0);
        int y = intent.getIntExtra("RECT_Y",0);
        int w = intent.getIntExtra("RECT_W",0);
        int h = intent.getIntExtra("RECT_H",0);
        float rot = (float) intent.getDoubleExtra("RECT_ROT",0);
        ImageView imageView = findViewById(R.id.imageView);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String filename = "temp.png";
        File file = new File(path, filename);
        Bitmap bitmap = BitmapFactory.decodeFile(file.toString());
        Matrix matrix = new Matrix();
        matrix.postRotate(rot);
        Bitmap rotated = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        Bitmap cropped = Bitmap.createBitmap(bitmap,x,y,w,h);
        imageView.setImageBitmap(cropped);
    }
}