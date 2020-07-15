package com.convergencelabstfx.pianoviewexample;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.convergencelabstfx.pianoview.PianoTouchListener;
import com.convergencelabstfx.pianoview.PianoView;
import com.convergencelabstfx.pianoviewexample.databinding.ActivityMainBinding;
import com.google.android.material.slider.Slider;

import java.util.Random;


/**
 * This app is meant to demonstrate the core functionality of the PianoView class.
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
        mBinding.numKeysSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setNumberOfKeys((int) value);
            }
        });

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

        mBinding.blackKeyWidthSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setBlackKeyWidthScale(value);
            }
        });

        mBinding.blackKeyHeightSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setBlackKeyHeightScale(value);
            }
        });

        mBinding.cornerRadiusSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setKeyCornerRadius((int) convertDpToPixel(value, getApplicationContext()));
            }
        });

        mBinding.strokeWidthSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                mBinding.piano.setKeyStrokeWidth((int) convertDpToPixel(value, getApplicationContext()));
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

        /*
         * These widgets let you control the interactive functionality
         * of the piano.
         */
        mBinding.highlightModeDrop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // The logic here is redundant,
                // but it (hopefully) makes it obvious how this functionality works.
                switch (i) {

                    case PianoView.HIGHLIGHT_ON_KEY_DOWN:
                        mBinding.piano.setShowPressMode(PianoView.HIGHLIGHT_ON_KEY_DOWN);
                        break;

                    case PianoView.HIGHLIGHT_ON_KEY_CLICK:
                        mBinding.piano.setShowPressMode(PianoView.HIGHLIGHT_ON_KEY_CLICK);
                        break;

                    case PianoView.HIGHLIGHT_OFF:
                        mBinding.piano.setShowPressMode(PianoView.HIGHLIGHT_OFF);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mBinding.enableMultiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mBinding.piano.setEnableMultiKeyHighlighting(b);
            }
        });


        /*
         * This button will randomize most of the features of the piano.
         */
        mBinding.randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random r = new Random();

                final float scaleMin = Float.parseFloat(getResources().getString(R.string.blackKeyScale_min));
                final float scaleMax = Float.parseFloat(getResources().getString(R.string.blackKeyScale_max));
                mBinding.blackKeyWidthSlider.setValue(scaleMin + r.nextFloat() * (scaleMax - scaleMin));
                mBinding.blackKeyHeightSlider.setValue(scaleMin + r.nextFloat() * (scaleMax - scaleMin));

                final Button temp = mCurSelectedButton;
                mBinding.piano.setWhiteKeyColor(Color.argb(
                        255,
                        r.nextInt(256),
                        r.nextInt(256),
                        r.nextInt(256)
                ));
                mBinding.piano.setBlackKeyColor(Color.argb(
                        255,
                        r.nextInt(256),
                        r.nextInt(256),
                        r.nextInt(256)
                ));
                mBinding.piano.setPressedKeyColor(Color.argb(
                        255,
                        r.nextInt(256),
                        r.nextInt(256),
                        r.nextInt(256)
                ));
                mBinding.piano.setKeyStrokeColor(Color.argb(
                        255,
                        r.nextInt(256),
                        r.nextInt(256),
                        r.nextInt(256)
                ));
                if (mCurSelectedButton == mBinding.whiteKeyColorToggle) {
                    loadColorIntoSliders(mBinding.piano.getWhiteKeyColor());
                }
                else if (mCurSelectedButton == mBinding.blackKeyColorToggle) {
                    loadColorIntoSliders(mBinding.piano.getBlackKeyColor());
                }
                else if (mCurSelectedButton == mBinding.pressedKeyColorToggle) {
                    loadColorIntoSliders(mBinding.piano.getPressedKeyColor());
                }
                else if (mCurSelectedButton == mBinding.keyStrokeColorToggle) {
                    loadColorIntoSliders(mBinding.piano.getKeyStrokeColor());
                }
                else {
                    throw new IllegalArgumentException("Illegal button passed as parameter");
                }

                final int minStrokeWidth = Integer.parseInt(getResources().getString(R.string.strokeWidth_min));
                final int maxStrokeWidth = Integer.parseInt(getResources().getString(R.string.strokeWidth_max));
                mBinding.strokeWidthSlider.setValue(r.nextInt(maxStrokeWidth - minStrokeWidth) + minStrokeWidth);

                final int minCornerRadius = Integer.parseInt(getResources().getString(R.string.cornerRadius_min));
                final int maxCornerRadius = Integer.parseInt(getResources().getString(R.string.cornerRadius_max));
                mBinding.strokeWidthSlider.setValue(r.nextInt(maxCornerRadius - minCornerRadius) + minCornerRadius);
            }
        });
    }

    private void loadDefaults() {
        setCurSelectedButton(mBinding.whiteKeyColorToggle, mBinding.piano.getWhiteKeyColor());

        // Load default values from PianoView object
        mBinding.numKeysSlider.setValue(mBinding.piano.getNumberOfKeys());
        mBinding.blackKeyWidthSlider.setValue(mBinding.piano.getBlackKeyWidthScale());
        mBinding.blackKeyHeightSlider.setValue(mBinding.piano.getBlackKeyHeightScale());

        mBinding.cornerRadiusSlider.setValue((int) convertPixelsToDp(mBinding.piano.getKeyCornerRadius(), getApplicationContext()));
        mBinding.strokeWidthSlider.setValue((int) convertPixelsToDp(mBinding.piano.getKeyStrokeWidth(), getApplicationContext()));
        mBinding.enableMultiSwitch.setChecked(mBinding.piano.isMultiKeyHighlightingEnabled());
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

    public static float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}