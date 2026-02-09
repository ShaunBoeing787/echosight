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
import com.example.echosight.camera.OverlayView;
import com.example.echosight.detection.ObjectDetector;
import com.example.echosight.feedback.AudioFeedback;
import com.example.echosight.feedback.FeedbackController;
import com.example.echosight.feedback.HapticManager;
import com.example.echosight.voice.SpeechOutput;
import com.example.echosight.voice.VoiceCommandManager;
import com.example.echosight.utils.PermissionUtils;

@ExperimentalGetImage
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ECHO_SIGHT";

    private PreviewView previewView;
    private OverlayView overlayView;
    private ObjectDetector detector;
    private SpeechOutput speechOutput;
    private VoiceCommandManager voiceManager;
    private CameraManager cameraManager;

    // Feedback Systems
    private FeedbackController feedbackController;
    private AudioFeedback audioFeedback;
    private HapticManager hapticManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);

        if (PermissionUtils.hasPermissions(this)) {
            initializeEchoSight();
        } else {
            PermissionUtils.ask(this);
        }
    }

    private void initializeEchoSight() {
        Log.d(TAG, "!!! INITIALIZING ECHO-SIGHT SYSTEMS !!!");

        try {
            speechOutput = new SpeechOutput(this);
            detector = new ObjectDetector(this);

            // Initialize Sensory Feedback Trio
            audioFeedback = new AudioFeedback();
            hapticManager = new HapticManager(this);
            feedbackController = new FeedbackController(hapticManager, audioFeedback);

            cameraManager = new CameraManager(this, this, previewView);

            voiceManager = new VoiceCommandManager(this, command -> {
                if (command.equals("START")) {
                    handleStartNavigation();
                } else if (command.equals("STOP")) {
                    handleStopNavigation();
                }
            });

            voiceManager.startListening();
            Log.i(TAG, "ALL SYSTEMS READY: Awaiting 'Start' command.");

        } catch (Exception e) {
            Log.e(TAG, "INIT FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStartNavigation() {
        speechOutput.speak("Navigation started. Scanning.");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraHardware();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleStopNavigation() {
        speechOutput.speak("Navigation stopped.");
        if (cameraManager != null) cameraManager.stopCamera();
        if (overlayView != null) overlayView.setResults(null);
        if (hapticManager != null) hapticManager.stop();
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCameraHardware() {
        // Pass detector, speech, overlay, AND feedbackController to the analyzer
        cameraManager.startCamera(new FrameAnalyzer(detector, speechOutput, overlayView, feedbackController));
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCameraHardware();
            });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechOutput != null) speechOutput.shutdown();
        if (voiceManager != null) voiceManager.stop();
        if (cameraManager != null) cameraManager.stopCamera();
        if (audioFeedback != null) audioFeedback.release();
    }
}