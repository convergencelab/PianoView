package com.convergencelabstfx.pianoviewexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.convergencelabstfx.pianoview.PianoView;

import java.util.Random;

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
    private Button mKeysButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPianoView = findViewById(R.id.piano);
        mKeysButton = findViewById(R.id.testButton);
        mKeysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random random = new Random();
                final int newNum = random.nextInt(24) + 1;
                Log.d("testV", "num keys: " + newNum);
                mPianoView.setNumberOfKeys(newNum);
                mPianoView.setBlackKeyWidthScale(Math.max(0.05f, random.nextFloat()));
                mPianoView.setBlackKeyHeightScale(Math.max(0.05f, random.nextFloat()));
            }
        });
        Log.d("testV", "numKeys: " + mPianoView.getNumberOfKeys());

    }
}