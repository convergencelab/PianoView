# PianoView
Custom piano view for Android applications with customizable look and feel.

[Demo app available on playstore](https://play.google.com/store/apps/details?id=com.convergencelabstfx.pianoviewexample)


## Screenshot
![PianoView Demo](pianoview2.gif)

## Usage

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.convergencelab:PianoView:v0.1'
	}

### XML
                
```xml
<com.convergencelabstfx.pianoview.PianoView
    ...
    app:showPressMode="onKeyDown" (or "onKeyClick" and "off")
    app:enableMultiKeyHighlighting="true"
    app:blackKeyColor="@color/blackKeyColor"
    app:whiteKeyColor="@color/whiteKeyColor"
    app:keyPressedColor="@color/pressedKeyColor"
    app:blackKeyHeightScale="0.7"
    app:blackKeyWidthScale="0.5"
    app:keyCornerRadius="2dp"
    app:keyStrokeWidth="1dp"
    app:keyStrokeColor="@color/keyStrokeColor"
    app:numberOfKeys="24"
    ...
/>
```

### Java
**Check out the example app**

### Touch Listener
```Java
// Touch interface for PianoView
mPianoView.addPianoTouchListener(new PianoTouchListener() {
    @Override
    public void onKeyDown(@NonNull PianoView piano, int key) {
	// Do something on key down
    }

    @Override
    public void onKeyUp(@NonNull PianoView piano, int key) {
	// Do something on key up
    }

    @Override
    public void onKeyClick(@NonNull PianoView piano, int key) {
	// Do something on key click
    }
});
```
