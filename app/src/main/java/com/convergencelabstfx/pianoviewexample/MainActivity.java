package com.convergencelabstfx.pianoviewexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.convergencelabstfx.pianoview.PianoView;

/*
 * TODO:
 *  - horizontal scroll piano
 *  - width slider
 *  - height slider
 *  - color pickers
 *  - corner radius slider
 *  - width radius
 *  - onTouch || onClick
 */

public class MainActivity extends AppCompatActivity {

    private PianoView mPianoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPianoView = findViewById(R.id.piano);
        Log.d("testV", "numKeys: " + mPianoView.getNumberOfKeys());
        mPianoView.setNumberOfKeys(89);
    }
}