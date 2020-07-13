/*
 * Android PianoView by Travis MacDonald, July 2020.
 * Made while doing research for Convergence Lab at St. Francis Xavier University,
 * established by Dr. James Hughes.
 */

package com.convergencelabstfx.pianoview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/*
 * TODO:
 *   ------------------------------------------------------------------
 *   (HIGH PRIORITY aka BEFORE DEPLOYING)
 *   - implement some logical ordering for functions in this file
 *   - function documentation
 *   - remove log calls
 *   - remove commented out code
 *   - give library a version
 *   ------------------------------------------------------------------
 *   (LOW PRIORITY)
 *   - write some unit tests
 *   - allow option for off-center keys (like a real piano)
 *   - standard constructor ( PianoView(context) )
 *   - the other constructor ( PianoView(context, attrs, defStyleInt) )
 *   - allow for padding
 *   - find better solution for 1 extra pixel on rightmost key
 *   - display note names on piano keys (allow text size, ..., yadada)
 *   ------------------------------------------------------------------
 *   (MAYBE)
 *   - a list for both white and black keys; would make for easier iteration
 *   ------------------------------------------------------------------
 *   (KNOWN BUGS)
 *   - when multi touch (and maybe single?) is enabled and highlight on key down:
 *       - pressing keys while rotating screen will cause the keys to remain held
 *
 */

public class PianoView extends View {

    public final static int HIGHLIGHT_ON_KEY_DOWN = 0;
    public final static int HIGHLIGHT_ON_KEY_CLICK = 1;
    public final static int HIGHLIGHT_OFF = 2;

    final public float SCALE_MAX = 1f;
    final public float SCALE_MIN = 0.05f;

    final public int MAX_NUMBER_OF_KEYS = 88;
    final public int MIN_NUMBER_OF_KEYS = 1;

    final public int NOTES_PER_OCTAVE = 12;

    final private int[] whiteKeyIxs = new int[]{0, 2, 4, 5, 7, 9, 11};
    final private int[] blackKeyIxs = new int[]{1, 3, 6, 8, 10};

    private final boolean[] isWhiteKey = new boolean[]{
            true, false, true, false, true, true,
            false, true, false, true, false, true,
    };

    private List<PianoTouchListener> mListeners = new ArrayList<>();
    private List<GradientDrawable> mPianoKeys = new ArrayList<>(MAX_NUMBER_OF_KEYS);
    private GradientDrawable mPianoBackground = new GradientDrawable();
    private Set<Integer> mPressedKeys = new HashSet<>(MAX_NUMBER_OF_KEYS);
    // todo: may change this later; although this is technically the max, it's unlikely it will get this high

    // todo: can change these sizes when the number of keys changes; using max num for now
    private SparseIntArray mActivePointerKeys = new SparseIntArray(MAX_NUMBER_OF_KEYS);
    private SparseBooleanArray mActivePointerHasMovedOffInitKey = new SparseBooleanArray(MAX_NUMBER_OF_KEYS);
    private int[] mActivePointerKeyTouchCount = new int[MAX_NUMBER_OF_KEYS];

    private int mWidth;
    private int mHeight;
    private int mViewWidthRemainder;

    private int mWhiteKeyWidth;
    private int mWhiteKeyHeight;
    private int mBlackKeyWidth;
    private int mBlackKeyHeight;

    private float mBlackKeyWidthScale;
    private float mBlackKeyHeightScale;

    private int mNumberOfKeys;
    private int mPrevNumberOfKeys;
    private int mNumberOfBlackKeys;
    private int mNumberOfWhiteKeys;

    private int mWhiteKeyColor;
    private int mBlackKeyColor;
    private int mPressedKeyColor;

    private int mKeyStrokeColor;
    private int mKeyStrokeWidth;
    private int mKeyCornerRadius;

    private int mLastTouchedKey;

    // todo: can probably remove these attrs will be parsed, and these are the default values there
    private int mShowPressMode = HIGHLIGHT_ON_KEY_DOWN;
    private boolean mEnableMultiKeyHighlighting = true;

    private boolean mHasMovedOffInitKey = false;

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSaveEnabled(true);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PianoView,
                0, 0
        );
        mPianoBackground.setShape(GradientDrawable.RECTANGLE);
        parseAttrs(a);
        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        findNumberOfWhiteAndBlackKeys(mNumberOfKeys);
        calculatePianoKeyDimensions();
        constructPianoKeyLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Have to draw the black keys on top of the white keys
        drawBackground(canvas);
        drawWhiteKeys(canvas);
        drawBlackKeys(canvas);
    }


    // todo: override performClick?
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEnableMultiKeyHighlighting) {
            handleTouchEventMulti(event);
        }
        else {
            handleTouchEventSingle(event);
        }
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState myState = new SavedState(superState);

        myState.mPressedKeys = new int[this.mPressedKeys.size()];
        int i = 0;
        for (Integer pressedKey : mPressedKeys) {
            myState.mPressedKeys[i] = pressedKey;
            i++;
        }

        myState.mShowPressMode = this.mShowPressMode;
        myState.mEnableMultiKeyHighlighting = this.mEnableMultiKeyHighlighting ? 1 : 0;

        myState.mNumberOfKeys = this.mNumberOfKeys;
        myState.mWhiteKeyColor = this.mWhiteKeyColor;
        myState.mBlackKeyColor = this.mBlackKeyColor;
        myState.mPressedKeyColor = this.mPressedKeyColor;
        myState.mKeyStrokeColor = this.mKeyStrokeColor;

        myState.mKeyCornerRadius = this.mKeyCornerRadius;
        myState.mKeyStrokeWidth = this.mKeyStrokeWidth;

        myState.mBlackKeyWidthScale = this.mBlackKeyWidthScale;
        myState.mBlackKeyHeightScale = this.mBlackKeyHeightScale;

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        for (int i = 0; i < savedState.mPressedKeys.length; i++) {
            this.mPressedKeys.add(savedState.mPressedKeys[i]);
        }

        this.mShowPressMode = savedState.mShowPressMode;
        this.mEnableMultiKeyHighlighting = (savedState.mEnableMultiKeyHighlighting == 1);

        this.mNumberOfKeys = savedState.mNumberOfKeys;
        this.mWhiteKeyColor = savedState.mWhiteKeyColor;
        this.mBlackKeyColor = savedState.mBlackKeyColor;
        this.mPressedKeyColor = savedState.mPressedKeyColor;
        this.mKeyStrokeColor = savedState.mKeyStrokeColor;

        this.mKeyCornerRadius = savedState.mKeyCornerRadius;
        this.mKeyStrokeWidth = savedState.mKeyStrokeWidth;

        this.mBlackKeyWidthScale = savedState.mBlackKeyWidthScale;
        this.mBlackKeyHeightScale = savedState.mBlackKeyHeightScale;

        // todo: i think not calling these is fine; just here in case weird stuff starts happening
//        calculatePianoKeyDimensions();
//        constructPianoKeyLayout();
        invalidate();
    }

    public int getShowPressMode() {
        return mShowPressMode;
    }

    public void setShowPressMode(int showPressMode) {
        // todo: coming back here later when bugs happen
        mShowPressMode = showPressMode;
        // todo: unhighlight if multiple pressed keys pressed keys
    }

    public boolean isMultiKeyHighlightingEnabled() {
        return mEnableMultiKeyHighlighting;
    }

    // todo: check back here later for bugs
    public void setEnableMultiKeyHighlighting(boolean enableMultiKeyHighlighting) {
        mEnableMultiKeyHighlighting = enableMultiKeyHighlighting;
//        if (mShowPressMode == HIGHLIGHT_ON_KEY_CLICK && mPressedKeys.size() > 1) {
//            Integer min = MAX_NUMBER_OF_KEYS + 1;
//            for (Integer keyIx : mPressedKeys) {
//                if (keyIx < min) {
//                    min = keyIx;
//                }
//            }
//            showKeyPressed(min);
//        }
    }

    public int getNumberOfKeys() {
        return mNumberOfKeys;
    }

    public void setNumberOfKeys(int numberOfKeys) {
        if (numberOfKeys < MIN_NUMBER_OF_KEYS || numberOfKeys > MAX_NUMBER_OF_KEYS) {
            throw new IllegalArgumentException(
                    "numberOfKeys must be between "
                            + (MIN_NUMBER_OF_KEYS) +
                            " and "
                            + (MAX_NUMBER_OF_KEYS) +
                            " (both inclusive). Actual numberOfKeys: " + numberOfKeys);
        }
        if (numberOfKeys == this.mNumberOfKeys) {
            return;
        }
        mPrevNumberOfKeys = this.mNumberOfKeys;
        this.mNumberOfKeys = numberOfKeys;
        if (!mPianoKeys.isEmpty()) {
            findNumberOfWhiteAndBlackKeys(numberOfKeys);
            calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public int getNumberOfBlackKeys() {
        return mNumberOfBlackKeys;
    }

    public int getNumberOfWhiteKeys() {
        return mNumberOfWhiteKeys;
    }

    public float getBlackKeyWidthScale() {
        return mBlackKeyWidthScale;
    }

    public void setBlackKeyWidthScale(float scale) {
        if (scale > SCALE_MAX || scale < SCALE_MIN) {
            throw new IllegalArgumentException(
                    "blackKeyWidthScale must be between "
                            + (SCALE_MIN) +
                            " and "
                            + (SCALE_MAX) +
                            " (both inclusive). Actual blackKeyWidthScale: " + mBlackKeyWidthScale);
        }
        mBlackKeyWidthScale = scale;
        if (!mPianoKeys.isEmpty()) {
            calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public float getBlackKeyHeightScale() {
        return mBlackKeyHeightScale;
    }

    public void setBlackKeyHeightScale(float scale) {
        if (scale > SCALE_MAX || scale < SCALE_MIN) {
            throw new IllegalArgumentException(
                    "blackKeyHeightScale must be between "
                            + (SCALE_MIN) +
                            " and "
                            + (SCALE_MAX) +
                            " (both inclusive). Actual blackKeyHeightScale: " + mBlackKeyWidthScale);
        }
        mBlackKeyHeightScale = scale;
        if (!mPianoKeys.isEmpty()) {
            calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public int getWhiteKeyColor() {
        return mWhiteKeyColor;
    }

    public void setWhiteKeyColor(int color) {
        if (color == mWhiteKeyColor) {
            return;
        }
        mWhiteKeyColor = color;
        if (!mPianoKeys.isEmpty()) {
            for (int i = 0; i < mNumberOfWhiteKeys; i++) {
                final int ix = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
                mPianoKeys.get(ix).setColor(color);
            }
            invalidate();
        }
    }

    public int getBlackKeyColor() {
        return mBlackKeyColor;
    }

    public void setBlackKeyColor(int color) {
        if (color == mBlackKeyColor) {
            return;
        }
        mBlackKeyColor = color;
        if (!mPianoKeys.isEmpty()) {
            for (int i = 0; i < mNumberOfBlackKeys; i++) {
                final int ix = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
                mPianoKeys.get(ix).setColor(color);
            }
            invalidate();
        }
    }

    public int getPressedKeyColor() {
        return mPressedKeyColor;
    }

    public void setPressedKeyColor(int color) {
        if (color == mPressedKeyColor) {
            return;
        }
        mPressedKeyColor = color;
        if (!mPianoKeys.isEmpty()) {
            for (int i = 0; i < mNumberOfKeys; i++) {
                if (keyIsPressed(i)) {
                    mPianoKeys.get(i).setColor(color);
                }
            }
            invalidate();
        }
    }

    public int getKeyStrokeColor() {
        return mKeyStrokeColor;
    }

    public void setKeyStrokeColor(int color) {
        if (color == mKeyStrokeColor) {
            return;
        }
        mKeyStrokeColor = color;
        if (!mPianoKeys.isEmpty()) {
            mPianoBackground.setColor(mKeyStrokeColor);
            for (GradientDrawable pianoKey : mPianoKeys) {
                pianoKey.setStroke(mKeyStrokeWidth, mKeyStrokeColor);
            }
            invalidate();
        }
    }

    public int getKeyStrokeWidth() {
        return mKeyStrokeWidth;
    }

    public void setKeyStrokeWidth(int width) {
        if (width == mKeyStrokeWidth) {
            return;
        }
        mKeyStrokeWidth = width;
        if (!mPianoKeys.isEmpty()) {
            for (GradientDrawable pianoKey : mPianoKeys) {
                pianoKey.setStroke(mKeyStrokeWidth, mKeyStrokeColor);
            }
            // The stroke of the keys overlap, so it requires recalculation
            calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public int getKeyCornerRadius() {
        return mKeyCornerRadius;
    }

    public void setKeyCornerRadius(int radius) {
        if (radius == mKeyCornerRadius) {
            return;
        }
        mKeyCornerRadius = radius;
        if (!mPianoKeys.isEmpty()) {
            mPianoBackground.setCornerRadius(mKeyCornerRadius);
            for (GradientDrawable pianoKey : mPianoKeys) {
                pianoKey.setCornerRadius(mKeyCornerRadius);
            }
            invalidate();
        }
    }

    public Rect getBoundsForKey(int keyIx) {
        return mPianoKeys.get(keyIx).getBounds();
    }

    public void addPianoTouchListener(PianoTouchListener listener) {
        mListeners.add(listener);
    }

    public void removePianoTouchListener(PianoTouchListener listener) {
        mListeners.remove(listener);
    }

    public void showKeyPressed(int ix) {
        if (!mPressedKeys.contains(ix)) {
            if (!mEnableMultiKeyHighlighting && !mPressedKeys.isEmpty()) {
                for (Integer keyIx : mPressedKeys) {
                    showKeyNotPressed(keyIx);
                    mPressedKeys.remove(keyIx);
                }
            }
            mPressedKeys.add(ix);
            GradientDrawable pianoKey = mPianoKeys.get(ix);
            pianoKey.setColor(mPressedKeyColor);
            invalidate();
        }
    }
    public void showKeyNotPressed(int ix) {
        if (mPressedKeys.contains(ix)) {
            GradientDrawable pianoKey = mPianoKeys.get(ix);
            mPressedKeys.remove(ix);
            if (isWhiteKey(ix)) {
                pianoKey.setColor(mWhiteKeyColor);
            } else {
                pianoKey.setColor(mBlackKeyColor);
            }
            invalidate();
        }
    }

    public boolean keyIsPressed(int ix) {
        return mPressedKeys.contains(ix);
    }


    private int getTouchedKey(int x, int y) {
        // Check black keys first
        for (int i = 0; i < mNumberOfBlackKeys; i++) {
            final int ix = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
            final Rect bounds = mPianoKeys.get(ix).getBounds();
            if (coordsAreInBounds(x, y, bounds.left, bounds.top, bounds.right, bounds.bottom)) {
                return ix;
            }
        }

        // Check white keys
        if (mNumberOfWhiteKeys == 1) {
            final Rect bounds = mPianoKeys.get(0).getBounds();
            // put comment here;
            if (coordsAreInBounds(x, y, bounds.left, bounds.top, bounds.right, bounds.bottom)) {
                return 0;
            }
        } else {
            for (int i = 0; i < mNumberOfWhiteKeys - 1; i++) {
                final int ix = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
                final Rect bounds = mPianoKeys.get(ix).getBounds();
                // todo: put comment here;
                final int adjustedRight = bounds.right - (mKeyStrokeWidth / 2);
                if (coordsAreInBounds(x, y, bounds.left, bounds.top, adjustedRight, bounds.bottom)) {
                    return ix;
                }
            }
            final int ix = whiteKeyIxs[(mNumberOfWhiteKeys - 1) % whiteKeyIxs.length] + ((mNumberOfWhiteKeys - 1) / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
            final Rect bounds = mPianoKeys.get(ix).getBounds();
            if (coordsAreInBounds(x, y, bounds.left, bounds.top, bounds.right, bounds.bottom)) {
                return ix;
            }
        }
        return -1;
    }

    // todo: handle moving outside view
    // todo: notify listeners
    // todo: conditional highlighting
    private void handleTouchEventMulti(MotionEvent event) {
        final int maskedAction = event.getActionMasked();
        int curPointerIndex;
        int curPointerId;
        int curTouchedKey;

        switch (maskedAction) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                curPointerIndex = event.getActionIndex();
                curPointerId = event.getPointerId(curPointerIndex);
                curTouchedKey = getTouchedKey(
                        Math.round(event.getX(curPointerIndex)),
                        Math.round(event.getY(curPointerIndex)));
                mActivePointerHasMovedOffInitKey.put(curPointerId, false);
                mActivePointerKeys.put(curPointerId, curTouchedKey);
                if (curTouchedKey != -1) {
                    mActivePointerKeyTouchCount[curTouchedKey]++;
                    if (!keyIsPressed(curTouchedKey)) {
                        for (PianoTouchListener listener : mListeners) {
                            listener.onKeyDown(this, curTouchedKey);
                        }
                        if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                            showKeyPressed(curTouchedKey);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    curPointerId = mActivePointerKeys.keyAt(i);
                    curPointerIndex = event.findPointerIndex(curPointerId);
                    curTouchedKey = getTouchedKey(
                            Math.round(event.getX(curPointerIndex)),
                            Math.round(event.getY(curPointerIndex)));
                    if (curTouchedKey != mActivePointerKeys.get(curPointerId)) {
                        if (mActivePointerKeys.get(curPointerId) != -1) {
                            mActivePointerKeyTouchCount[mActivePointerKeys.get(curPointerId)]--;
                            if (mActivePointerKeyTouchCount[mActivePointerKeys.get(curPointerId)] == 0) {
                                for (PianoTouchListener listener : mListeners) {
                                    listener.onKeyUp(this, mActivePointerKeys.get(curPointerId));
                                }
                                if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                                    showKeyNotPressed(mActivePointerKeys.get(curPointerId));
                                }

                            }
                        }
                        mActivePointerKeys.put(curPointerId, curTouchedKey);
                        mActivePointerHasMovedOffInitKey.put(curPointerId, true);
                        if (curTouchedKey != -1) {
                            mActivePointerKeyTouchCount[curTouchedKey]++;
                            if (!keyIsPressed(curTouchedKey)) {
                                for (PianoTouchListener listener : mListeners) {
                                    listener.onKeyDown(this, curTouchedKey);
                                }
                                if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                                    showKeyPressed(curTouchedKey);
                                }
                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                curPointerIndex = event.getActionIndex();
                curPointerId = event.getPointerId(curPointerIndex);
                curTouchedKey = getTouchedKey(
                        Math.round(event.getX(curPointerIndex)),
                        Math.round(event.getY(curPointerIndex)));
                if (curTouchedKey != -1) {
                    mActivePointerKeyTouchCount[curTouchedKey]--;
                    if (mActivePointerKeyTouchCount[curTouchedKey] == 0) {
                        for (PianoTouchListener listener : mListeners) {
                            listener.onKeyUp(this, curTouchedKey);
                        }
                        if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                            showKeyNotPressed(curTouchedKey);
                        }
                    }
                    if (!mActivePointerHasMovedOffInitKey.get(curPointerId)) {
                        for (PianoTouchListener listener : mListeners) {
                            listener.onKeyClick(this, curTouchedKey);
                        }
                        if (mShowPressMode == HIGHLIGHT_ON_KEY_CLICK) {
                            if (!keyIsPressed(curTouchedKey)) {
                                showKeyPressed(curTouchedKey);
                            }
                            else {
                                showKeyNotPressed(curTouchedKey);
                            }
                        }
                    }
                }
                mActivePointerKeys.delete(curPointerId);
                break;
        }
    }

    private void handleTouchEventSingle(MotionEvent event) {
        int curTouchedKey = getTouchedKey(Math.round(event.getX()), Math.round(event.getY()));

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                    showKeyPressed(curTouchedKey);
                }
                for (PianoTouchListener listener : mListeners) {
                    listener.onKeyDown(this, curTouchedKey);
                }
                mLastTouchedKey = curTouchedKey;
                mHasMovedOffInitKey = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mLastTouchedKey != curTouchedKey) {
                    if (mLastTouchedKey != -1) {
                        if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                            showKeyNotPressed(mLastTouchedKey);
                        }
                        for (PianoTouchListener listener : mListeners) {
                            listener.onKeyUp(this, mLastTouchedKey);
                        }
                    }
                    if (curTouchedKey != -1) {
                        if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
                            showKeyPressed(curTouchedKey);
                        }
                        for (PianoTouchListener listener : mListeners) {
                            listener.onKeyDown(this, curTouchedKey);
                        }
                    }
                    mLastTouchedKey = curTouchedKey;
                    mHasMovedOffInitKey = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN && curTouchedKey != -1) {
                    showKeyNotPressed(curTouchedKey);
                    for (PianoTouchListener listener : mListeners) {
                        listener.onKeyUp(this, curTouchedKey);
                    }
                }
                if (!mHasMovedOffInitKey) {
                    if (mShowPressMode == HIGHLIGHT_ON_KEY_CLICK) {
                        if (!keyIsPressed(curTouchedKey)) {
                            showKeyPressed(curTouchedKey);
                        }
                        else {
                            showKeyNotPressed(curTouchedKey);
                        }
                    }
                    for (PianoTouchListener listener : mListeners) {
                        listener.onKeyClick(this, curTouchedKey);
                    }
                }
                break;
        }
    }

    private boolean coordsAreInBounds(
            int x,
            int y,
            int left,
            int top,
            int right,
            int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private void drawBackground(Canvas canvas) {
        mPianoBackground.draw(canvas);
    }

    private void drawWhiteKeys(Canvas canvas) {
        for (int i = 0; i < mNumberOfWhiteKeys; i++) {
            final int keyIx = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length * NOTES_PER_OCTAVE);
            mPianoKeys.get(keyIx).draw(canvas);
        }
    }

    private void drawBlackKeys(Canvas canvas) {
        for (int i = 0; i < mNumberOfBlackKeys; i++) {
            final int keyIx = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length * NOTES_PER_OCTAVE);
            mPianoKeys.get(keyIx).draw(canvas);
        }
    }

    private GradientDrawable makePianoKey(
            int fillColor,
            int strokeWidth,
            int strokeColor,
            int cornerRadius
    ) {
        final GradientDrawable pianoKey = new GradientDrawable();
        pianoKey.setShape(GradientDrawable.RECTANGLE);
        pianoKey.setColor(fillColor);
        pianoKey.setStroke(strokeWidth, strokeColor);
        pianoKey.setCornerRadius(cornerRadius);
        return pianoKey;
    }

    // todo: use setters instead of directly setting ? maybe
    private void parseAttrs(TypedArray attrs) {
        mKeyCornerRadius = Math.round(attrs.getDimension(
                R.styleable.PianoView_keyCornerRadius,
                getResources().getDimension(R.dimen.keyCornerRadius)
        ));
        mBlackKeyColor = attrs.getColor(
                R.styleable.PianoView_blackKeyColor,
                getResources().getColor(R.color.blackKeyColor)
        );
        mWhiteKeyColor = attrs.getColor(
                R.styleable.PianoView_whiteKeyColor,
                getResources().getColor(R.color.whiteKeyColor)
        );
        mPressedKeyColor = attrs.getColor(
                R.styleable.PianoView_keyPressedColor,
                getResources().getColor(R.color.keyPressedColor)
        );
        mBlackKeyHeightScale = Math.min(1, attrs.getFloat(
                R.styleable.PianoView_blackKeyHeightScale,
                ResourcesCompat.getFloat(getResources(), R.dimen.blackKeyHeightScale))
        );
        mBlackKeyWidthScale = Math.min(1, attrs.getFloat(
                R.styleable.PianoView_blackKeyWidthScale,
                ResourcesCompat.getFloat(getResources(), R.dimen.blackKeyWidthScale))
        );
        mKeyStrokeColor = attrs.getColor(
                R.styleable.PianoView_keyStrokeColor,
                getResources().getColor(R.color.keyStrokeColor)
        );
        mKeyStrokeWidth = Math.round(attrs.getDimension(
                R.styleable.PianoView_keyStrokeWidth,
                getResources().getDimension(R.dimen.keyStrokeWidth)
        ));
        setNumberOfKeys(attrs.getInt(
                R.styleable.PianoView_numberOfKeys,
                getResources().getInteger(R.integer.numberOfKeys))
        );
        mShowPressMode = attrs.getInt(
                R.styleable.PianoView_showPressMode,
                HIGHLIGHT_ON_KEY_DOWN
        );
        mEnableMultiKeyHighlighting = attrs.getBoolean(
                R.styleable.PianoView_enableMultiKeyHighlighting,
                true
        );
    }

    private void findNumberOfWhiteAndBlackKeys(int numberOfKeys) {
        mNumberOfWhiteKeys = 0;
        mNumberOfBlackKeys = 0;
        for (int i = 0; i < numberOfKeys; i++) {
            if (isWhiteKey(i)) {
                mNumberOfWhiteKeys++;
            } else {
                mNumberOfBlackKeys++;
            }
        }
    }

    private boolean isWhiteKey(int ix) {
        return isWhiteKey[ix % NOTES_PER_OCTAVE];
    }

    private boolean rightMostKeyIsWhite() {
        return isWhiteKey(mNumberOfKeys - 1);
    }

    // todo: may be worth recalculating these dimensions so that the borders are drawn better
    private void calculatePianoKeyDimensions() {
        // The rightmost key is white
        if (rightMostKeyIsWhite()) {
            mWhiteKeyWidth =
                    (mWidth + (mNumberOfWhiteKeys - 1) * mKeyStrokeWidth) / mNumberOfWhiteKeys;
            mBlackKeyWidth =
                    Math.round(mWhiteKeyWidth * mBlackKeyWidthScale);
            mViewWidthRemainder =
                    mWidth - (mWhiteKeyWidth * mNumberOfWhiteKeys - mKeyStrokeWidth * (mNumberOfWhiteKeys - 1));
        }
        // The rightmost key is black
        else {
            // todo: explain the math
            // some math, but it works
            float ans = (((2 * mWidth) + (2 * mNumberOfWhiteKeys * mKeyStrokeWidth) - mKeyStrokeWidth) / (2 * mNumberOfWhiteKeys + mBlackKeyWidthScale));
            mWhiteKeyWidth = (int) ans;
            mBlackKeyWidth =
                    Math.round(mWhiteKeyWidth * mBlackKeyWidthScale);
            mViewWidthRemainder =
                    mWidth - ((mWhiteKeyWidth * mNumberOfWhiteKeys - (mKeyStrokeWidth * (mNumberOfWhiteKeys - 1))) + ((mBlackKeyWidth / 2) - mKeyStrokeWidth / 2));
        }
        mWhiteKeyHeight = mHeight;
        mBlackKeyHeight = Math.round(mWhiteKeyHeight * mBlackKeyHeightScale);
    }

    private void constructPianoKeyLayout() {
        mPianoKeys.clear();
        // todo: might be a better way of doing this
        for (int i = 0; i < mNumberOfKeys; i++) {
            mPianoKeys.add(null);
        }
        for (int i = mNumberOfKeys; i < mPrevNumberOfKeys; i++) {
            mPressedKeys.remove(i);
        }
        mPianoBackground.setCornerRadius(mKeyCornerRadius);
        mPianoBackground.setColor(mKeyStrokeColor);
        mPianoBackground.setBounds(0, 0, getWidth(), getHeight());

        int left = 0;
        // todo: update the math in this comment
        // This view divides it's width by 7. So if the width isn't divisible by 7
        // there would be unused space in the view.
        // For this reason, whiteKeyWidth has 1 added to it, and 1 removed from it after the
        // remainder has been added in.

        mWhiteKeyWidth++;
        for (int i = 0; i < mNumberOfWhiteKeys; i++) {
            if (i == mViewWidthRemainder) {
                mWhiteKeyWidth--;
            }
            final int keyIx = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
            final int keyFillColor;
            if (keyIsPressed(keyIx)) {
                keyFillColor = mPressedKeyColor;
            } else {
                keyFillColor = mWhiteKeyColor;
            }
            final GradientDrawable pianoKey = makePianoKey(keyFillColor, mKeyStrokeWidth, mKeyStrokeColor, mKeyCornerRadius);
            pianoKey.setBounds(left, 0, left + mWhiteKeyWidth, mWhiteKeyHeight);
            mPianoKeys.set(keyIx, pianoKey);
            left += mWhiteKeyWidth - mKeyStrokeWidth;
        }

        for (int i = 0; i < mNumberOfBlackKeys; i++) {
            final int keyIx = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
            GradientDrawable whiteKey = mPianoKeys.get(keyIx - 1);
            left = whiteKey.getBounds().right - (mBlackKeyWidth / 2) - (mKeyStrokeWidth / 2);
            final int keyFillColor;
            if (keyIsPressed(keyIx)) {
                keyFillColor = mPressedKeyColor;
            } else {
                keyFillColor = mBlackKeyColor;
            }
            final GradientDrawable pianoKey = makePianoKey(keyFillColor, mKeyStrokeWidth, mKeyStrokeColor, mKeyCornerRadius);
            pianoKey.setBounds(left, 0, left + mBlackKeyWidth, mBlackKeyHeight);
            mPianoKeys.set(keyIx, pianoKey);
        }
        // Sometimes there is 1 extra pixel on the end, and I have no idea why.
        // This will clip the rightmost keys it doesn't go over the bounds
        mPianoKeys.get(mNumberOfKeys - 1).getBounds().right =
                Math.min(mPianoKeys.get(mNumberOfKeys - 1).getBounds().right, mWidth);
    }

    // todo: add field for enableMultiHighlight
    private static class SavedState extends BaseSavedState {

        int[] mPressedKeys;
        int mShowPressMode;
        int mEnableMultiKeyHighlighting;

        int mNumberOfKeys;
        int mWhiteKeyColor;
        int mBlackKeyColor;
        int mPressedKeyColor;
        int mKeyStrokeColor;

        int mKeyCornerRadius;
        int mKeyStrokeWidth;

        float mBlackKeyWidthScale;
        float mBlackKeyHeightScale;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);

            in.readIntArray(mPressedKeys);
            in.readInt();    // mShowPressMode
            in.readInt();    // mEnabledMultiKeyHighlighting

            in.readInt();    // mNumberOfKeys
            in.readInt();    // mWhiteKeyColor
            in.readInt();    // mBlackKeyColor
            in.readInt();    // mKeyPressedColor
            in.readInt();    // mKeyStrokeColor

            in.readInt();    // mKeyCornerRadius
            in.readInt();    // mKeyStrokeWidth

            in.readFloat();  // mBlackKeyWidthScale
            in.readFloat();  // mBlackKeyHeightScale
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeIntArray(mPressedKeys);
            out.writeInt(mShowPressMode);
            out.writeInt(mEnableMultiKeyHighlighting);

            out.writeInt(mNumberOfKeys);
            out.writeInt(mWhiteKeyColor);
            out.writeInt(mBlackKeyColor);
            out.writeInt(mPressedKeyColor);
            out.writeInt(mKeyStrokeColor);

            out.writeInt(mKeyCornerRadius);
            out.writeInt(mKeyStrokeWidth);

            out.writeFloat(mBlackKeyWidthScale);
            out.writeFloat(mBlackKeyHeightScale);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
