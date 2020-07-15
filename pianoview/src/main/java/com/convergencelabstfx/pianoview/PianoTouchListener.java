package com.convergencelabstfx.pianoview;

import androidx.annotation.NonNull;

import java.util.List;

public interface PianoTouchListener {

    // todo: possibly pass the pianoview itself as a parameter
    void onKeyDown(@NonNull PianoView piano, int key);

    void onKeyUp(@NonNull PianoView piano, int key);

    void onKeyClick(@NonNull PianoView piano, int key);

}
