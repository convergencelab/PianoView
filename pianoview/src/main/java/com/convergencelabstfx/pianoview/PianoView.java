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


// todo: onclick crash when dragging off screen
// todo: multi-touch
// todo: save state lifecycle
// todo: implement some logical ordering for functions in this file
// todo: function documentation
// todo: remove log calls
// todo: remove commented out code

// todo: rename variables to match android naming guidelines (i.e. mKeyStrokeWidth) -- later
// todo: possibly? let users choose dimens of white keys, instead of only allowing total width -- later
// todo: default constructor -- later
// todo: look into other two constructors -- later
// todo: account for padding when measuring view -- later
public class PianoView extends View {

    final public float SCALE_MAX = 1f;
    final public float SCALE_MIN = 0.05f;

    final public int MAX_NUMBER_OF_KEYS = 88;
    final public int MIN_NUMBER_OF_KEYS = 1;

    final public int NOTES_PER_OCTAVE = 12;

    final private int[] whiteKeyIxs = new int[]{0, 2, 4, 5, 7, 9, 11};
    final private int[] blackKeyIxs = new int[]{1, 3, 6, 8, 10};

    private List<PianoTouchListener> listeners = new ArrayList<>();
    private List<GradientDrawable> pianoKeys = new ArrayList<>();
    private List<Boolean> keyIsPressed = new ArrayList<>(MAX_NUMBER_OF_KEYS);

    private final boolean[] isWhiteKey = new boolean[]{
            true, false, true, false, true, true,
            false, true, false, true, false, true,
    };

    // todo: remove these, eventually
    private int numTests;
    private int numTrue;

    private int width;
    private int height;
    private int viewWidthRemainder;

    private int whiteKeyWidth;
    private int whiteKeyHeight;
    private int blackKeyWidth;
    private int blackKeyHeight;

    private float blackKeyWidthScale;
    private float blackKeyHeightScale;

    private int numberOfKeys;
    private int prevNumberOfKeys;
    private int numberOfBlackKeys;
    private int numberOfWhiteKeys;

    private int whiteKeyColor;
    private int blackKeyColor;
    private int pressedKeyColor;
    private int keyStrokeColor;

    private int keyStrokeWidth;
    private int keyCornerRadius;

    private int lastTouchedKey;


    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PianoView,
                0, 0
        );
        parseAttrs(a);
        pianoKeys = new ArrayList<>();
        for (int i = 0; i < MAX_NUMBER_OF_KEYS; i++) {
            keyIsPressed.add(false);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // todo: i don't think there needs to be a super call here
//        super.onSizeChanged(w, h, oldw, oldh);
//        Log.d("testV", "onSizeChanged called");
        width = w;
        height = h;
        findNumberOfWhiteAndBlackKeys(numberOfKeys);
        calculatePianoKeyDimensions();
        constructPianoKeyLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        Log.d("testV", "onDraw called");
        // Have to draw the black keys on top of the white keys
        drawWhiteKeys(canvas);
        drawBlackKeys(canvas);
//        Log.d("testV", "first px: " + pianoKeys.get(0).getBounds().left);
//        Log.d("testV", "last px: " + pianoKeys.get(numberOfKeys - 1).getBounds().right);
        StringBuilder sb = new StringBuilder("Debug message: \n");

        sb.append("rightBound: ");
        sb.append(pianoKeys.get(numberOfKeys - 1).getBounds().right);
        sb.append('\n');
//
//        sb.append("lastKeyBlack: ");
//        sb.append(!rightMostKeyIsWhite());
//        sb.append('\n');
//
//        sb.append("numKeys: ");
//        sb.append(numberOfKeys);
//        sb.append('\n');
//
//        sb.append("numBlackKeys: ");
//        sb.append(numberOfBlackKeys);
//        sb.append('\n');
//
//        sb.append("numWhiteKeys: ");
//        sb.append(numberOfWhiteKeys);
//        sb.append('\n');
//
//        sb.append("vwr: ");
//        sb.append(viewWidthRemainder);
//        sb.append('\n');

//        boolean prediction = numberOfKeys % 2 == 1 && !rightMostKeyIsWhite() && viewWidthRemainder % 2 == 1;
//        boolean condition = pianoKeys.get(numberOfKeys - 1).getBounds().right == 1081;
//
//        numTests++;
//        if (prediction == condition) {
//            numTrue++;
//        }
//        else {
//        }

        Log.d("dumpDbug", sb.toString());
//        if (numTests == 48) {
//            Log.d("dumpDbug", Integer.toString(numTrue) + "/" + Integer.toString(numTests));
//        }
    }

    // todo: find out how to deal with multiple touches at once
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int curTouchedKey = getTouchedKey(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchedKey = curTouchedKey;
                for (PianoTouchListener listener : listeners) {
                    listener.onPianoTouch(this, curTouchedKey);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (lastTouchedKey != curTouchedKey) {
                    for (PianoTouchListener listener : listeners) {
                        listener.onPianoTouch(this, curTouchedKey);
                    }
                    lastTouchedKey = curTouchedKey;
                }
                break;
            case MotionEvent.ACTION_UP:
                for (PianoTouchListener listener : listeners) {
                    listener.onPianoTouch(this, -1);
                    listener.onPianoClick(this, curTouchedKey);

                }
                break;
        }
        return true;
    }

    public int getNumberOfKeys() {
        return numberOfKeys;
    }

//    public void setNumberOfKeys(int numberOfKeys) {
//        setNumberOfKeys(numberOfKeys);
//    }

    // todo: there may be a bug that causes keywidths to shrink or grow 1px when maintainKeyWidths is true
    public void setNumberOfKeys(int numberOfKeys) {
        if (numberOfKeys < MIN_NUMBER_OF_KEYS || numberOfKeys > MAX_NUMBER_OF_KEYS) {
            throw new IllegalArgumentException(
                    "numberOfKeys must be between "
                            + (MIN_NUMBER_OF_KEYS) +
                            " and "
                            + (MAX_NUMBER_OF_KEYS) +
                            " (both inclusive). Actual numberOfKeys: " + numberOfKeys);
        }
        if (numberOfKeys == this.numberOfKeys) {
            return;
        }
        prevNumberOfKeys = this.numberOfKeys;
        this.numberOfKeys = numberOfKeys;
        if (!pianoKeys.isEmpty()) {
            findNumberOfWhiteAndBlackKeys(numberOfKeys);
            calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public int getNumberOfBlackKeys() {
        return numberOfBlackKeys;
    }

    public int getNumberOfWhiteKeys() {
        return numberOfWhiteKeys;
    }

    public float getBlackKeyWidthScale() {
        return blackKeyWidthScale;
    }

    public void setBlackKeyWidthScale(float scale) {
        if (scale > SCALE_MAX || scale < SCALE_MIN) {
            throw new IllegalArgumentException(
                    "blackKeyWidthScale must be between "
                            + (SCALE_MIN) +
                            " and "
                            + (SCALE_MAX) +
                            " (both inclusive). Actual blackKeyWidthScale: " + blackKeyWidthScale);
        }
        blackKeyWidthScale = scale;
        if (!pianoKeys.isEmpty()) {
            this.calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public float getBlackKeyHeightScale() {
        return blackKeyHeightScale;
    }

    public void setBlackKeyHeightScale(float scale) {
        if (scale > SCALE_MAX || scale < SCALE_MIN) {
            throw new IllegalArgumentException(
                    "blackKeyHeightScale must be between "
                            + (SCALE_MIN) +
                            " and "
                            + (SCALE_MAX) +
                            " (both inclusive). Actual blackKeyHeightScale: " + blackKeyWidthScale);
        }
        blackKeyHeightScale = scale;
        if (!pianoKeys.isEmpty()) {
            this.calculatePianoKeyDimensions();
            constructPianoKeyLayout();
            invalidate();
        }
    }

    public int getWhiteKeyColor() {
        return whiteKeyColor;
    }

    public void setWhiteKeyColor(int color) {
        if (color == whiteKeyColor) {
            return;
        }
        whiteKeyColor = color;
        if (!pianoKeys.isEmpty()) {
            for (int i = 0; i < numberOfWhiteKeys; i++) {
                final int ix = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
                pianoKeys.get(ix).setColor(color);
            }
            invalidate();
        }
    }

    public int getBlackKeyColor() {
        return blackKeyColor;
    }

    public void setBlackKeyColor(int color) {
        if (color == blackKeyColor) {
            return;
        }
        blackKeyColor = color;
        if (!pianoKeys.isEmpty()) {
            for (int i = 0; i < numberOfBlackKeys; i++) {
                final int ix = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
                pianoKeys.get(ix).setColor(color);
            }
            invalidate();
        }
    }

    public int getPressedKeyColor() {
        return pressedKeyColor;
    }

    public void setPressedKeyColor(int color) {
        if (color == pressedKeyColor) {
            return;
        }
        pressedKeyColor = color;
        if (!pianoKeys.isEmpty()) {
            for (int i = 0; i < numberOfKeys; i++) {
                if (keyIsPressed(i)) {
                    pianoKeys.get(i).setColor(color);
                }
            }
            invalidate();
        }
    }

    public int getKeyStrokeColor() {
        return keyStrokeColor;
    }

    public void setKeyStrokeColor(int color) {
        if (color == keyStrokeColor) {
            return;
        }
        keyStrokeColor = color;
        if (!pianoKeys.isEmpty()) {
            invalidate();
        }
    }

    public int getKeyStrokeWidth() {
        return keyStrokeWidth;
    }

    public void setKeyStrokeWidth(int width) {
        if (width == keyStrokeWidth) {
            return;
        }
        keyStrokeWidth = width;
        if (!pianoKeys.isEmpty()) {
            invalidate();
        }
    }

    public int getKeyCornerRadius() {
        return keyCornerRadius;
    }

    public void setKeyCornerRadius(int radius) {
        if (radius == keyCornerRadius) {
            return;
        }
        keyCornerRadius = radius;
        if (!pianoKeys.isEmpty()) {
            invalidate();
        }
    }

    public void addPianoTouchListener(PianoTouchListener listener) {
        listeners.add(listener);
    }

    public void removePianoTouchListener(PianoTouchListener listener) {
        // todo: test; make sure it works correctly
        listeners.remove(listener);
    }

    public void showKeyPressed(int ix) {
        if (!keyIsPressed.get(ix)) {
            keyIsPressed.set(ix, true);
            GradientDrawable pianoKey = pianoKeys.get(ix);
            pianoKey.setColor(pressedKeyColor);
            invalidate();
        }
    }

    public void showKeyNotPressed(int ix) {
        if (keyIsPressed.get(ix)) {
            GradientDrawable pianoKey = pianoKeys.get(ix);
            keyIsPressed.set(ix, false);
            if (isWhiteKey(ix)) {
                pianoKey.setColor(whiteKeyColor);
            } else {
                pianoKey.setColor(blackKeyColor);
            }
            invalidate();
        }
    }

    public boolean keyIsPressed(int ix) {
        return keyIsPressed.get(ix);
    }

    // todo: change these to ints
    // todo: the borders overlap on the middle white keys,
    //       so implement the correct touch geometry (stroke / 2)
    //       (currently biased towards the left keys, they get the whole border)
    private int getTouchedKey(float x, float y) {
        // todo: use a 'round' function instead of casting
        final int touchX = (int) x;
        final int touchY = (int) y;
        for (int i = 0; i < numberOfBlackKeys; i++) {
            final int ix = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
            if (coordsAreInPianoKey(touchX, touchY, pianoKeys.get(ix))) {
                return ix;
            }
        }
        for (int i = 0; i < numberOfWhiteKeys; i++) {
            final int ix = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
            if (coordsAreInPianoKey(touchX, touchY, pianoKeys.get(ix))) {
                return ix;
            }
        }
        return -1;
    }

    private boolean coordsAreInPianoKey(int x, int y, GradientDrawable key) {
        Rect keyBounds = key.getBounds();
        return x >= keyBounds.left && x <= keyBounds.right && y >= keyBounds.top && y <= keyBounds.bottom;
    }

    private void drawWhiteKeys(Canvas canvas) {
        for (int i = 0; i < numberOfWhiteKeys; i++) {
            final int keyIx = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length * NOTES_PER_OCTAVE);
            pianoKeys.get(keyIx).draw(canvas);
        }
    }

    private void drawBlackKeys(Canvas canvas) {
        for (int i = 0; i < numberOfBlackKeys; i++) {
            final int keyIx = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length * NOTES_PER_OCTAVE);
            pianoKeys.get(keyIx).draw(canvas);
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
        keyCornerRadius = Math.round(attrs.getDimension(
                R.styleable.PianoView_keyCornerRadius,
                getResources().getDimension(R.dimen.keyCornerRadius)
        ));
        blackKeyColor = attrs.getColor(
                R.styleable.PianoView_blackKeyColor,
                getResources().getColor(R.color.blackKeyColor)
        );
        whiteKeyColor = attrs.getColor(
                R.styleable.PianoView_whiteKeyColor,
                getResources().getColor(R.color.whiteKeyColor)
        );
        pressedKeyColor = attrs.getColor(
                R.styleable.PianoView_keyPressedColor,
                getResources().getColor(R.color.keyPressedColor)
        );
        blackKeyHeightScale = Math.min(1, attrs.getFloat(
                R.styleable.PianoView_blackKeyHeightScale,
                ResourcesCompat.getFloat(getResources(), R.dimen.blackKeyHeightScale))
        );
        blackKeyWidthScale = Math.min(1, attrs.getFloat(
                R.styleable.PianoView_blackKeyWidthScale,
                ResourcesCompat.getFloat(getResources(), R.dimen.blackKeyWidthScale))
        );
        keyStrokeColor = attrs.getColor(
                R.styleable.PianoView_keyStrokeColor,
                getResources().getColor(R.color.keyStrokeColor)
        );
        // todo: use a round function instead
        keyStrokeWidth = Math.round(attrs.getDimension(
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
        numberOfWhiteKeys = 0;
        numberOfBlackKeys = 0;
        for (int i = 0; i < numberOfKeys; i++) {
            if (isWhiteKey(i)) {
                numberOfWhiteKeys++;
            } else {
                numberOfBlackKeys++;
            }
        }
    }

    private boolean isWhiteKey(int ix) {
        return isWhiteKey[ix % NOTES_PER_OCTAVE];
    }

    private boolean rightMostKeyIsWhite() {
        // todo: change to number of keys
        return isWhiteKey(numberOfWhiteKeys + numberOfBlackKeys - 1);
    }

    private void calculatePianoKeyDimensions() {
        // The rightmost key is white
        if (rightMostKeyIsWhite()) {
            whiteKeyWidth =
                    (width + (numberOfWhiteKeys - 1) * keyStrokeWidth) / numberOfWhiteKeys;
            blackKeyWidth =
                    Math.round(whiteKeyWidth * blackKeyWidthScale);
            viewWidthRemainder =
                    width - (whiteKeyWidth * numberOfWhiteKeys - keyStrokeWidth * (numberOfWhiteKeys - 1));
        }
        // The rightmost key is black
        else {
            // todo: explain the math
            // some math, but it works
            float ans = (((2 * width) + (2 * numberOfWhiteKeys * keyStrokeWidth) - keyStrokeWidth) / (2 * numberOfWhiteKeys + blackKeyWidthScale));
            whiteKeyWidth = (int) ans;
            blackKeyWidth =
                    Math.round(whiteKeyWidth * blackKeyWidthScale);
            viewWidthRemainder =
                    width - ((whiteKeyWidth * numberOfWhiteKeys - (keyStrokeWidth * (numberOfWhiteKeys - 1))) + ((blackKeyWidth / 2) - keyStrokeWidth / 2));
        }
        whiteKeyHeight = height;
        blackKeyHeight = Math.round(whiteKeyHeight * blackKeyHeightScale);
        Log.d("testV", "w: " + whiteKeyWidth);
    }

    private void constructPianoKeyLayout() {
        Log.d("testV", "vwr: " + viewWidthRemainder);
        pianoKeys.clear();
        // todo: might be a better way of doing this
        for (int i = 0; i < numberOfKeys; i++) {
            pianoKeys.add(null);
        }
        for (int i = numberOfKeys; i < prevNumberOfKeys; i++) {
            keyIsPressed.set(i, false);
        }

        int left = 0;
        // todo: update the math in this comment
        // This view divides it's width by 7. So if the width isn't divisible by 7
        // there would be unused space in the view.
        // For this reason, whiteKeyWidth has 1 added to it, and 1 removed from it after the
        // remainder has been added in.

        whiteKeyWidth++;
        for (int i = 0; i < numberOfWhiteKeys; i++) {
            if (i == viewWidthRemainder) {
                whiteKeyWidth--;
            }
            final int keyIx = whiteKeyIxs[i % whiteKeyIxs.length] + (i / whiteKeyIxs.length) * NOTES_PER_OCTAVE;
            final int keyFillColor;
            if (keyIsPressed(keyIx)) {
                keyFillColor = pressedKeyColor;
            } else {
                keyFillColor = whiteKeyColor;
            }
            final GradientDrawable pianoKey = makePianoKey(keyFillColor, keyStrokeWidth, keyStrokeColor, keyCornerRadius);
            pianoKey.setBounds(left, 0, left + whiteKeyWidth, whiteKeyHeight);
            pianoKeys.set(keyIx, pianoKey);
            left += whiteKeyWidth - keyStrokeWidth;
        }

        for (int i = 0; i < numberOfBlackKeys; i++) {
            final int keyIx = blackKeyIxs[i % blackKeyIxs.length] + (i / blackKeyIxs.length) * NOTES_PER_OCTAVE;
            GradientDrawable whiteKey = pianoKeys.get(keyIx - 1);
            left = whiteKey.getBounds().right - (blackKeyWidth / 2) - (keyStrokeWidth / 2);
            final int keyFillColor;
            if (keyIsPressed(keyIx)) {
                keyFillColor = pressedKeyColor;
            } else {
                keyFillColor = blackKeyColor;
            }
            final GradientDrawable pianoKey = makePianoKey(keyFillColor, keyStrokeWidth, keyStrokeColor, keyCornerRadius);
            pianoKey.setBounds(left, 0, left + blackKeyWidth, blackKeyHeight);
            pianoKeys.set(keyIx, pianoKey);
        }
        // Sometimes there is 1 extra pixel on the end, and I have no idea why.
        // This will clip the rightmost keys it doesn't go over the bounds
        pianoKeys.get(numberOfKeys - 1).getBounds().right =
                Math.min(pianoKeys.get(numberOfKeys - 1).getBounds().right, width);
    }

}

