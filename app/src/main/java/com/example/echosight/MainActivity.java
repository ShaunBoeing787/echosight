package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.echosight.camera.CameraManager;
import com.example.echosight.camera.FrameAnalyzer;
import com.example.echosight.detection.ObjectDetector;

import com.example.echosight.detection.ObjectDetector;
import com.example.echosight.voice.SpeechOutput;
import com.example.echosight.voice.VoiceCommandManager; // NEW: Added
import com.example.echosight.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ECHO_SIGHT";

    private PreviewView previewView;
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
        Log.e(TAG, "APP STARTED");

        previewView = findViewById(R.id.previewView);

        // Initialize detector (Sector 2)
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
            Log.e(TAG, "DETECTOR INITIALIZED");

            Log.e(TAG, " ALL SYSTEMS GO: Voice and Brain are ready.");
        } catch (Exception e) {
            Log.e(TAG, "DETECTOR INIT FAILED: " + e.getMessage());
            Log.e(TAG, "SECTOR FAIL: " + e.getMessage());
            e.printStackTrace();
        }

        checkCameraPermission();
    }

    /**
     * Check camera permission
     */
    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "PERMISSION ALREADY GRANTED");
            startCamera();

        } else {

            Log.e(TAG, "REQUESTING CAMERA PERMISSION");
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Start camera + analyzer
     */
    private void startCamera() {

        Log.e(TAG, "STARTING CAMERA");

        CameraManager manager =
                new CameraManager(this, this, previewView);

        // Pass detector into analyzer
        manager.startCamera(new FrameAnalyzer(detector));
    }

    /**
     * Permission launcher
     */
    private final androidx.activity.result.ActivityResultLauncher<String>
            requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.e(TAG, "PERMISSION GRANTED BY USER");
                            startCamera();
                        } else {
                            Log.e(TAG, "PERMISSION DENIED");
                            Toast.makeText(
                                    this,
                                    "Camera permission required",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

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
