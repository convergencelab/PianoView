package com.convergencelabstfx.pianoviewexample;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
 *  - width slider
 *  - height slider
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
        loadDefaults();
    }

    private void init() {

        /*
         * Checkout logcat to see how these listeners work.
         */
        mBinding.piano.addPianoTouchListener(new PianoTouchListener() {
            @Override
            public void onKeyDown(@NonNull PianoView piano, int key) {
                Log.d("pianoListener", "key down:  " + key);
            }

            @Override
            public void onKeyUp(@NonNull PianoView piano, int key) {
                Log.d("pianoListener", "key up:    " + key);
            }

            @Override
            public void onKeyClick(@NonNull PianoView piano, int key) {
                Log.d("pianoListener", "key click: " + key);
            }
        });


        /*
         * These sliders let you control the dimensions of the piano.
         */
        mBinding.pianoWidthSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                ViewGroup.LayoutParams params = mBinding.piano.getLayoutParams();
                if (params.width != 0 && value != 0.0) {
                    params.width = (int) convertDpToPixel(value, getApplicationContext());
                    mBinding.piano.setLayoutParams(params);
                }
            }
        });

        mBinding.pianoHeightSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                ViewGroup.LayoutParams params = mBinding.piano.getLayoutParams();
                if (params.height != 0 && value != 0.0) {
                    params.height = (int) convertDpToPixel(value, getApplicationContext());
                    mBinding.piano.setLayoutParams(params);
                }
            }
        });

        /*
         * These buttons control the colors of the piano keys.
         */

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


        /*
         * These sliders let you control the color of the piano keys.
         */

        mBinding.redSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                updateCurKeyColor();
            }
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

    private void loadDefaults() {
        setCurSelectedButton(mBinding.whiteKeyColorToggle, mBinding.piano.getWhiteKeyColor());
        Log.d("px", "px: " + convertPixelsToDp(mBinding.piano.getWidth(), getApplicationContext()));
    }

    /*
     * Sets the current selected button and loads its values
     * into the sliders.
     */
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

    /*
     * Takes the current selected button and applies
     * the slider color value to the buttons respective key.
     * (i.e. whiteKeyButton -> piano.whiteKeyColor; blackKeyButton -> piano.blackKeyColor; ...)
     */
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

    /*
     * Converts an int onto its respective RGB values
     * and loads them into the slider.
     */
    private void loadColorIntoSliders(int colorVal) {
        mBinding.redSlider.setValue((colorVal >> 16) & 255);
        mBinding.greenSlider.setValue((colorVal >> 8) & 255);
        mBinding.blueSlider.setValue(colorVal & 255);
    }

    /*
     * Converts the RGB values from the sliders
     * into an int color value.
     */
    private int getSliderColor() {
        return Color.argb(
                255,
                Math.round(mBinding.redSlider.getValue()),
                Math.round(mBinding.greenSlider.getValue()),
                Math.round(mBinding.blueSlider.getValue())
        );
    }

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}