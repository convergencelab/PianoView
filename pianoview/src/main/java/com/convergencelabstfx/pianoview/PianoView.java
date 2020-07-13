/*
 * Android PianoView by Travis MacDonald, July 2020.
 * Made while doing research for Convergence Lab at St. Francis Xavier University,
 * established by Dr. James Hughes.
 */

package com.convergencelabstfx.pianoview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
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
 *   - cancel onClick stuff if pointer moves offscreen
 *   - allow option for off-center keys (like a real piano)
 *   - standard constructor ( PianoView(context) )
 *   - the other constructor ( PianoView(context, attrs, defStyleInt) )
 *   - showKeysPressed(int[] keys); showKeysNotPressed(int[] keys)
 *   - allow for padding
 *   - find better solution for 1 extra pixel on rightmost key
 *   - test left key bias click detection
 *   - optimize multi-touch functionality
 *   - display note names on piano keys (allow text size, ..., yadada)
 *   ------------------------------------------------------------------
 *   (MAYBE)
 *   - a list for both white and black keys; would make for easier iteration
 *   ------------------------------------------------------------------
 */

public class PianoView extends View {

//    public enum ShowPressMode {
//        ON_TOUCH,
//        ON_CLICK,
//        OFF
//    }

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
    // todo: maybe use a some type of set instead
//    private List<Boolean> mKeyIsPressed = new ArrayList<>(MAX_NUMBER_OF_KEYS);
    private Set<Integer> mPressedKeys = new HashSet<>(MAX_NUMBER_OF_KEYS); // todo: may change this later; although this is technically the max, it's unlikely it will get this high

    private SparseArray<PointF> mActivePointers = new SparseArray<>();

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
    private int mCurPressedKey = -1;

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

        /*
        for (int i = 0; i < MAX_NUMBER_OF_KEYS; i++) {
            mKeyIsPressed.add(false);
        }

         */
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

        /*
        myState.mKeysIsPressed = new boolean[this.mKeyIsPressed.size()];
        for (int i = 0; i < this.mKeyIsPressed.size(); i++) {
            myState.mKeysIsPressed[i] = this.mKeyIsPressed.get(i);
        }

         */

        myState.mShowPressMode = this.mShowPressMode;

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

/*
        for (int i = 0; i < savedState.mKeysIsPressed.length; i++) {
            this.mKeyIsPressed.set(i, savedState.mKeysIsPressed[i]);
        }

 */
        this.mShowPressMode = savedState.mShowPressMode;

        this.mNumberOfKeys = savedState.mNumberOfKeys;
        this.mWhiteKeyColor = savedState.mWhiteKeyColor;
        this.mBlackKeyColor = savedState.mBlackKeyColor;
        this.mPressedKeyColor = savedState.mPressedKeyColor;
        this.mKeyStrokeColor = savedState.mKeyStrokeColor;

        this.mKeyCornerRadius = savedState.mKeyCornerRadius;
        this.mKeyStrokeWidth = savedState.mKeyStrokeWidth;

        this.mBlackKeyWidthScale = savedState.mBlackKeyWidthScale;
        this.mBlackKeyHeightScale = savedState.mBlackKeyHeightScale;

        // todo: i think not calling these is fine; just leaving here in case
//        calculatePianoKeyDimensions();
//        constructPianoKeyLayout();
        invalidate();
    }

    public int getShowPressMode() {
        return mShowPressMode;
    }

    public void setShowPressMode(int showPressMode) {
        mShowPressMode = showPressMode;
        // todo: unhighlight pressed keys
    }

    public boolean isMultiKeyHighlightingEnabled() {
        return mEnableMultiKeyHighlighting;
    }

    public void setEnableMultiKeyHighlighting(boolean enableMultiKeyHighlighting) {
        mEnableMultiKeyHighlighting = enableMultiKeyHighlighting;
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

    public void showKeysPressed(List<Integer> keys) {
        showKeysPressed(keys, true);
    }

    // todo: this could be optimized

    public void showKeysPressed(List<Integer> keys, boolean showExclusively) {
        // todo: come back here later
        /*
        for (int i = 0; i < mKeyIsPressed.size(); i++) {
            showKeyNotPressed(i);
        }
        for (int key : keys) {
            showKeyPressed(key);
        }
        invalidate();

         */
    }
    // todo: add exclusive parameter

    public void showKeyPressed(int ix) {
//        if (!mKeyIsPressed.get(ix)) {
        if (!mPressedKeys.contains(ix)) {
//            mKeyIsPressed.set(ix, true);
            mPressedKeys.add(ix);
            GradientDrawable pianoKey = mPianoKeys.get(ix);
            pianoKey.setColor(mPressedKeyColor);
            invalidate();
        }
    }
    public void showKeyNotPressed(int ix) {
//        if (mKeyIsPressed.get(ix)) {
        if (mPressedKeys.contains(ix)) {
            GradientDrawable pianoKey = mPianoKeys.get(ix);
//            mKeyIsPressed.set(ix, false);
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
//        return mKeyIsPressed.get(ix);
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

    private void handleTouchEventMulti(MotionEvent event) {
//        final int pointerIndex = event.getActionIndex();
//        final int pointerId = event.getPointerId(pointerIndex);
//        final int maskedAction = event.getActionMasked();
//
//        switch (maskedAction) {
//
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_POINTER_DOWN:
//                final PointF newPoint = new PointF();
//                newPoint.x = event.getX(pointerIndex);
//                newPoint.y = event.getY(pointerIndex);
//                mActivePointers.put(pointerId, newPoint);
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                for (int i = 0; i < event.getPointerCount(); i++) {
//                    final PointF curPoint = mActivePointers.get(event.getPointerId(i));
//                    if (curPoint != null) {
//                        curPoint.x = event.getX(i);
//                        curPoint.y = event.getY(i);
//                    }
//                }
//                break;
//
//            case MotionEvent.ACTION_POINTER_UP:
//            case MotionEvent.ACTION_UP:
//                final PointF point = mActivePointers.get(pointerId);
//                final int keyIx = getTouchedKey(Math.round(point.x), Math.round(point.y));
//                if (keyIx != -1) {
//                    for (PianoTouchListener listener : mListeners) {
//                        listener.onPianoClick(this, keyIx);
//                    }
//                    if (mShowPressMode == HIGHLIGHT_ON_KEY_UP) {
//                        if (!keyIsPressed(keyIx)) {
//                            showKeyPressed(keyIx);
//                        } else {
//                            showKeyNotPressed(keyIx);
//                        }
//                    }
//                }
//                mActivePointers.remove(pointerId);
//                break;
//        }
//
//        final ArrayList<Integer> touchedKeys = new ArrayList<>(mActivePointers.size());
//        for (int i = 0; i < mActivePointers.size(); i++) {
//            final PointF point = mActivePointers.get(mActivePointers.keyAt(i));
//            if (point != null) {
//                final int keyIx = getTouchedKey(Math.round(point.x), Math.round(point.y));
//                if (keyIx != -1) {
//                    touchedKeys.add(keyIx);
//                }
//            }
//        }
//        for (PianoTouchListener listener : mListeners) {
//            listener.onPianoKeyDown(this, touchedKeys);
//        }
//        if (mShowPressMode == HIGHLIGHT_ON_KEY_DOWN) {
//            showKeysPressed(touchedKeys);
//        }

    }

    // todo: highlight if option enabled
    // todo: deal with user going outiside view
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
                            if (!mEnableMultiKeyHighlighting && mCurPressedKey != -1) {
                                showKeyNotPressed(mCurPressedKey);
                            }
                            mCurPressedKey = curTouchedKey;
                        }
                        else {
                            showKeyNotPressed(curTouchedKey);
                            mCurPressedKey = -1;
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
        // todo: touch mode
        // todo: multi key highlighitng
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
//            mKeyIsPressed.set(i, false);
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

    private static class SavedState extends BaseSavedState {

        // todo: fix name
        boolean[] mKeysIsPressed;
        int mShowPressMode;

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

            in.readBooleanArray(mKeysIsPressed);
            in.readInt();    // mShowPressMode

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

            out.writeBooleanArray(mKeysIsPressed);
            out.writeInt(mShowPressMode);

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
