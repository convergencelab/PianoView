# PianoView
Custom piano view for Android applications with customizable look and feel.

[Demo app available on playstore](https://www.youtube.com/watch?v=dQw4w9WgXcQ)

(todo: add link when available)

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
Step 2. Add the dependency // todo: update 'Tag' with release version

	dependencies {
	        implementation 'com.github.convergencelab:PianoView:Tag'
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
