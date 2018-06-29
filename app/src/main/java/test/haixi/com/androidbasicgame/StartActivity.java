package test.haixi.com.androidbasicgame;

import android.os.Bundle;

import test.haixi.com.androidbasicgame.Components.BasicActivity;
import test.haixi.com.library.MapImageView;

public class StartActivity extends BasicActivity {
    MapImageView mapImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mapImageView = findViewById(R.id.mapImageView);
    }
}
