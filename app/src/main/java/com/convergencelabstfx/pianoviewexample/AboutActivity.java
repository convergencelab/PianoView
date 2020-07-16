package com.convergencelabstfx.pianoviewexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private TextView mAboutText;
    private TextView mVersionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mAboutText = findViewById(R.id.about_text);
        mVersionText = findViewById(R.id.version_text);

        mAboutText.setText(
                "Custom piano view library for android projects.\n" +
                "Source code found here: \nhttps://github.com/convergencelab/PianoView");

        String version = getVersionName();
        if (version != null) {
            mVersionText.setText("PianoView version used in this app: v" + version);
        }
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}