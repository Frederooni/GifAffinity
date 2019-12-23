package com.example.gifaffinity;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class GifViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gif_fullscreen_layout);
        ImageView gifImageView = findViewById(R.id.gif_image);
        Uri gifUri = getIntent().getData();
        if (gifUri == null) finish();
        Glide.with(this).load(gifUri).into(gifImageView);
        String gifTitle = getIntent().getStringExtra("title");
        if (gifTitle == null) finish();
        setTitle(gifTitle);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Go back to where we were before
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
