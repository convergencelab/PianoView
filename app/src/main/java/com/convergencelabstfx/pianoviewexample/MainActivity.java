package com.convergencelabstfx.pianoviewexample;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
    private Button mCurSelectedButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        init();
        setCurSelectedButton(mBinding.whiteKeyColorToggle, mBinding.piano.getWhiteKeyColor());
    }

    private void init() {
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

        /* Key Color Buttons */

        mBinding.whiteKeyColorToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurSelectedButton(mBinding.whiteKeyColorToggle, mBinding.piano.getWhiteKeyColor());
            }
        });

        mBinding.blackKeyColorToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurSelectedButton(mBinding.blackKeyColorToggle, mBinding.piano.getBlackKeyColor());
            }
        });

        mBinding.pressedKeyColorToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurSelectedButton(mBinding.pressedKeyColorToggle, mBinding.piano.getPressedKeyColor());
            }
        });

        mBinding.keyStrokeColorToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurSelectedButton(mBinding.keyStrokeColorToggle, mBinding.piano.getKeyStrokeColor());
            }
        });


        /* Key Color Sliders */

        mBinding.redSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                updateCurKeyColor();            }
        });

        mBinding.greenSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                updateCurKeyColor();
            }
        });

        mBinding.blueSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                updateCurKeyColor();
            }
        });

    }

    private void setCurSelectedButton(Button button, int color) {
        if (mCurSelectedButton != button) {
            if (mCurSelectedButton != null) {
                mCurSelectedButton.setBackgroundColor(getResources().getColor(R.color.inactiveButtonTint));
            }
            button.setBackgroundColor(Color.RED);
            button.setBackgroundColor(getResources().getColor(R.color.activeButtonTint));

            mCurSelectedButton = button;
            loadColorIntoSliders(color);
        }
    }

    private void updateCurKeyColor() {
        if (mCurSelectedButton == mBinding.whiteKeyColorToggle) {
            mBinding.piano.setWhiteKeyColor(getSliderColor());
        }
        else if (mCurSelectedButton == mBinding.blackKeyColorToggle) {
            mBinding.piano.setBlackKeyColor(getSliderColor());
        }
        else if (mCurSelectedButton == mBinding.pressedKeyColorToggle) {
            mBinding.piano.setPressedKeyColor(getSliderColor());
        }
        else if (mCurSelectedButton == mBinding.keyStrokeColorToggle) {
            mBinding.piano.setKeyStrokeColor(getSliderColor());
        }
        else {
            throw new IllegalArgumentException("Illegal button passed as parameter");
        }
    }

    private void loadColorIntoSliders(int colorVal) {
        mBinding.redSlider.setValue((colorVal >> 16) & 255);
        mBinding.greenSlider.setValue((colorVal >> 8) & 255);
        mBinding.blueSlider.setValue(colorVal & 255);
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