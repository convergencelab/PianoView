package com.convergencelabstfx.pianoview;

import java.util.List;

public interface PianoTouchListener {

    // todo: possibly pass the pianoview itself as a parameter
    void onPianoTouch(PianoView piano, List<Integer> key);

    void onPianoClick(PianoView piano, int key);

}
