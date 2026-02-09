package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.echosight.camera.CameraManager;
import com.example.echosight.camera.FrameAnalyzer;
import com.example.echosight.detection.ObjectDetector;
import com.example.echosight.voice.SpeechOutput;
import com.example.echosight.voice.VoiceCommandManager;
import com.example.echosight.utils.PermissionUtils;

@ExperimentalGetImage
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ECHO_SIGHT";

    private PreviewView previewView;
    private ObjectDetector detector;
    private SpeechOutput speechOutput;
    private VoiceCommandManager voiceManager;
    private CameraManager cameraManager; // Moved to class level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        // Standard permission check on startup
        if (PermissionUtils.hasPermissions(this)) {
            initializeEchoSight();
        } else {
            PermissionUtils.ask(this);
        }
    }

    private void initializeEchoSight() {
        Log.d(TAG, "!!! INITIALIZING ECHO-SIGHT SYSTEMS !!!");

        try {
            // 1. Initialize Mouth (Speech Output)
            speechOutput = new SpeechOutput(this);

            // 2. Initialize Brain (Object Detector)
            detector = new ObjectDetector(this);

            // 3. Initialize Camera Controller (The Eyes)
            cameraManager = new CameraManager(this, this, previewView);

            // 4. Initialize Ears (Voice Command Manager)
            voiceManager = new VoiceCommandManager(this, command -> {
                Log.d(TAG, "MainActivity received command: " + command);

                if (command.equals("START")) {
                    handleStartNavigation();
                } else if (command.equals("STOP")) {
                    handleStopNavigation();
                }
            });

            // Start listening for voice triggers immediately
            voiceManager.startListening();

            Log.i(TAG, "ALL SYSTEMS READY: Awaiting 'Start' command.");

        } catch (Exception e) {
            Log.e(TAG, "INIT FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStartNavigation() {
        speechOutput.speak("Navigation started. Scanning for objects.");

        // Double check permissions before opening hardware
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraHardware();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleStopNavigation() {
        Log.d(TAG, "Stopping hardware feed...");
        speechOutput.speak("Navigation stopped. Standing by.");

        if (cameraManager != null) {
            cameraManager.stopCamera(); // Uses the stopCamera method we added earlier
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCameraHardware() {
        Log.e(TAG, "STARTING CAMERA FEED");
        // Pass both detector and speechOutput to the analyzer for verbal feedback
        cameraManager.startCamera(new FrameAnalyzer(detector, speechOutput));
    }

    // --- Permission Handling ---

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCameraHardware();
                } else {
                    Toast.makeText(this, "Camera permission required for navigation.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_CODE) {
            if (PermissionUtils.hasPermissions(this)) {
                initializeEchoSight();
            } else {
                Toast.makeText(this, "Permissions required for Echo Sight.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- Cleanup ---

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechOutput != null) speechOutput.shutdown();
        if (voiceManager != null) voiceManager.stop();
        if (cameraManager != null) cameraManager.stopCamera();
    }
}