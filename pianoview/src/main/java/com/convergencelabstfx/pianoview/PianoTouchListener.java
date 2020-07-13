package com.convergencelabstfx.pianoview;

import java.util.List;

public interface PianoTouchListener {

    // todo: possibly pass the pianoview itself as a parameter
    void onKeyDown(PianoView piano, int key);

    void onKeyUp(PianoView piano, int key);

    void onKeyClick(PianoView piano, int key);

}
