package com.convergencelabstfx.pianoviewexample;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.convergencelabstfx.pianoview.PianoTouchListener;
import com.convergencelabstfx.pianoview.PianoView;
import com.convergencelabstfx.pianoviewexample.databinding.ActivityMainBinding;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.piano.addPianoTouchListener(new PianoTouchListener() {
            @Override
            public void onKeyDown(PianoView piano, int key) {
                Log.d("touchTest", "key down:  " + key);
            }

            @Override
            public void onKeyUp(PianoView piano, int key) {
                Log.d("touchTest", "key up:    " + key);
            }

            @Override
            public void onKeyClick(PianoView piano, int key) {
                Log.d("touchTest", "key click: " + key);
            }
        });

    }
}