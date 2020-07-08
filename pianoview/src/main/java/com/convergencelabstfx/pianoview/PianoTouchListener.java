package com.convergencelabstfx.pianoview;

public interface PianoTouchListener {

    // todo: possibly pass the pianoview itself as a parameter
    void onPianoTouch(PianoView piano, int key);

    void onPianoClick(PianoView piano, int key);

}
