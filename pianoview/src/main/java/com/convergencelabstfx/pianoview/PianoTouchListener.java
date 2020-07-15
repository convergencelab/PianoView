package com.convergencelabstfx.pianoview;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Touch listener interface for PianoView.
 * Can listen for {@link #onKeyDown(PianoView, int)}, {@link #onKeyUp(PianoView, int)}
 * and {@link #onKeyClick(PianoView, int)} events.
 */
public interface PianoTouchListener {

    /**
     * Called when a key enters the 'down' state,
     * i.e. it was touched.
     * @param piano The PianoView currently touched.
     * @param key Index of the key that was touched.
     */
    void onKeyDown(@NonNull PianoView piano, int key);

    /**
     * Called when a key enters the 'up' state,
     * i.e. it was released.
     * @param piano The PianoView currently touched.
     * @param key Index of the key that was released.
     */
    void onKeyUp(@NonNull PianoView piano, int key);

    /**
     * Called when a key was clicked.
     * A key is considered 'clicked' if the user touched and
     * released the key without touching any other keys in between
     * those two events.
     * @param piano The PianoView currently clicked.
     * @param key Index of the key that was clicked.
     */
    void onKeyClick(@NonNull PianoView piano, int key);

}
