package com.convergencelabstfx.pianoviewexample;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.convergencelabstfx.pianoview.PianoTouchListener;
import com.convergencelabstfx.pianoview.PianoView;
import com.convergencelabstfx.pianoviewexample.databinding.ActivityMainBinding;
import com.google.android.material.slider.Slider;


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

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mBinding.piano.addPianoTouchListener(new PianoTouchListener() {
            @Override
            public void onKeyDown(@NonNull PianoView piano, int key) {
                Log.d("touchTest", "key down:  " + key);
            }

            @Override
            public void onKeyUp(@NonNull PianoView piano, int key) {
                Log.d("touchTest", "key up:    " + key);
            }

            @Override
            public void onKeyClick(@NonNull PianoView piano, int key) {
                Log.d("touchTest", "key click: " + key);
            }
        });

        mBinding.redSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setWhiteKeyColor(getSliderColor());
            }
        });

        mBinding.greenSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setWhiteKeyColor(getSliderColor());
            }
        });

        mBinding.blueSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setWhiteKeyColor(getSliderColor());
            }
        });

    }

    private int getSliderColor() {
        return Color.argb(
                255,
                Math.round(mBinding.redSlider.getValue()),
                Math.round(mBinding.greenSlider.getValue()),
                Math.round(mBinding.blueSlider.getValue())
        );
    }
}