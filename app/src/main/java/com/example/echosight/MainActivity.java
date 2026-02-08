package com.example.echosight;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.echosight.detection.ObjectDetector;
import com.example.echosight.voice.SpeechOutput;
import com.example.echosight.voice.VoiceCommandManager; // NEW: Added
import com.example.echosight.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ECHO_SIGHT";
    private ObjectDetector detector;
    private SpeechOutput speechOutput;
    private VoiceCommandManager voiceManager; // NEW: Added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (PermissionUtils.hasPermissions(this)) {
            startSector2();
        } else {
            PermissionUtils.ask(this);
        }
    }

    private void startSector2() {
        Log.e(TAG, "!!! ECHO-SIGHT STARTING: INITIALIZING SECTOR 5 & 2 !!!");

        try {
            // 1. Initialize Voice Output (The Mouth)
            speechOutput = new SpeechOutput(this);

            // 2. NEW: Initialize Voice Commands (The Ears)
            voiceManager = new VoiceCommandManager(this, command -> {
                Log.d("ECHO_SIGHT", "MainActivity received: " + command); // Add this log!
                if (command.equals("START")) {
                    speechOutput.speak("Navigation started.");
                }
                if (command.equals("STOP")) {
                    Log.d("ECHO_SIGHT", "Executing Stop Logic...");
                    speechOutput.speak("Navigation stopped.");
                }
            });

            // 3. Start listening immediately if initialized
            voiceManager.startListening();
            // 4. Initialize Detector
            detector = new ObjectDetector(this);

            Log.e(TAG, " ALL SYSTEMS GO: Voice and Brain are ready.");
        } catch (Exception e) {
            Log.e(TAG, "SECTOR FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_CODE) {
            if (PermissionUtils.hasPermissions(this)) {
                startSector2();
            } else {
                Toast.makeText(this, "Permissions required for Echo Sight.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechOutput != null) speechOutput.shutdown();
        if (voiceManager != null) voiceManager.stop(); // NEW: Stop listening on exit
    }
}