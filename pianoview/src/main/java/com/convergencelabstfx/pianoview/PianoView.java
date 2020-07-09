package com.convergencelabstfx.pianoview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;


/*
 * TODO:
 *   ------------------------------------------------------------------
 *   (HIGH PRIORITY)
 *   - save state on lifecycle changes
 *   - implement multi-touch functionality
 *   - implement some logical ordering for functions in this file
 *   - function documentation
 *   - remove log calls
 *   - remove commented out code
 *   ------------------------------------------------------------------
 *   (LOW PRIORITY)
 *   - allow option for off-center keys (like a real piano)
 *   - standard constructor ( PianoView(context) )
 *   - the other constructor ( PianoView(context, attrs, defStyleInt) )
 *   - allow for padding
 *   - find better solution for 1 extra pixel on rightmost key
 *   ------------------------------------------------------------------
 *   (MAYBE)
 *   - a list for both white and black keys; would make for easier iteration
 *   ------------------------------------------------------------------
 */

public class PianoView extends View {

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
    private List<Boolean> mKeyIsPressed = new ArrayList<>(MAX_NUMBER_OF_KEYS);

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

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PianoView,
                0, 0
        );
        parseAttrs(a);
        for (int i = 0; i < MAX_NUMBER_OF_KEYS; i++) {
            mKeyIsPressed.add(false);
        }
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
        super.onDraw(canvas);
        // Have to draw the black keys on top of the white keys
        drawWhiteKeys(canvas);
        drawBlackKeys(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int curTouchedKey = getTouchedKey(Math.round(event.getX()), Math.round(event.getY()));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchedKey = curTouchedKey;
                for (PianoTouchListener listener : mListeners) {
                    listener.onPianoTouch(this, curTouchedKey);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mLastTouchedKey != curTouchedKey) {
                    for (PianoTouchListener listener : mListeners) {
                        listener.onPianoTouch(this, curTouchedKey);
                    }
                    mLastTouchedKey = curTouchedKey;
                }
                break;

            case MotionEvent.ACTION_UP:
                for (PianoTouchListener listener : mListeners) {
                    listener.onPianoTouch(this, -1);
                    if (curTouchedKey != -1) {
                        listener.onPianoClick(this, curTouchedKey);
                    }
                }
                break;
        }
        return true;
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
            this.calculatePianoKeyDimensions();
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
            this.calculatePianoKeyDimensions();
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
        if (!mKeyIsPressed.get(ix)) {
            mKeyIsPressed.set(ix, true);
            GradientDrawable pianoKey = mPianoKeys.get(ix);
            pianoKey.setColor(mPressedKeyColor);
            invalidate();
        }
    }

    public void showKeyNotPressed(int ix) {
        if (mKeyIsPressed.get(ix)) {
            GradientDrawable pianoKey = mPianoKeys.get(ix);
            mKeyIsPressed.set(ix, false);
            if (isWhiteKey(ix)) {
                pianoKey.setColor(mWhiteKeyColor);
            } else {
                pianoKey.setColor(mBlackKeyColor);
            }
            invalidate();
        }
    }

    public boolean keyIsPressed(int ix) {
        return mKeyIsPressed.get(ix);
    }


    // todo: i think i fixed the left key bias;
    //       do some testing to make sure
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
        }
        else {
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

    private boolean coordsAreInBounds(
            int x,
            int y,
            int left,
            int top,
            int right,
            int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
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
        attrs.recycle();
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
        Log.d("testV", "w: " + mWhiteKeyWidth);
    }

    private void constructPianoKeyLayout() {
        Log.d("testV", "vwr: " + mViewWidthRemainder);
        mPianoKeys.clear();
        // todo: might be a better way of doing this
        for (int i = 0; i < mNumberOfKeys; i++) {
            mPianoKeys.add(null);
        }
        for (int i = mNumberOfKeys; i < mPrevNumberOfKeys; i++) {
            mKeyIsPressed.set(i, false);
        }

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

}

