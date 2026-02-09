package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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

import android.widget.ImageButton;

@ExperimentalGetImage
public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private OverlayView overlayView;
    private ImageView loadingGif;
    private Button btnToggle;

    private ObjectDetector detector;
    private SpeechOutput speechOutput;
    private VoiceCommandManager voiceManager;
    private CameraManager cameraManager;
    private FeedbackController feedbackController;
    private AudioFeedback audioFeedback;
    private HapticManager hapticManager;

    private boolean isRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        loadingGif = findViewById(R.id.loadingGif);
        btnToggle = findViewById(R.id.btnToggleScan);

        // Load centered GIF
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(loadingGif);

        // Initial forced loading → 2 sec
        showStartAfterDelay(2000);

        btnToggle.setOnClickListener(v -> {
            if (!isRunning) {
                startSystem();
            } else {
                stopSystem();
            }
        });

        ImageButton btnHelp = findViewById(R.id.btnHelp);

        btnHelp.setOnClickListener(v -> {
            speechOutput.speak("Say start to begin navigation. Say stop to end.");
        });


        if (PermissionUtils.hasPermissions(this)) {
            initializeEchoSight();
        } else {
            PermissionUtils.ask(this);
        }
    }

    // ===============================
    // SHOW START BUTTON AFTER DELAY
    // ===============================
    private void showStartAfterDelay(int delayMs) {
        btnToggle.setVisibility(View.GONE);
        loadingGif.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            btnToggle.setVisibility(View.VISIBLE);
            btnToggle.setText("START");
            btnToggle.setTextColor(0xFFB3E5FC);
            btnToggle.setBackground(getDrawable(R.drawable.start_button_bg));
        }, delayMs);
    }


    // ===============================
    // START SYSTEM
    // ===============================
    private void startSystem() {
        isRunning = true;

        previewView.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.VISIBLE);
        loadingGif.setVisibility(View.GONE);

        btnToggle.setText("STOP");
        btnToggle.setTextColor(0xFFFFCDD2);
        btnToggle.setBackground(getDrawable(R.drawable.stop_button_bg));

        handleStartNavigation();
    }

    // ===============================
    // STOP SYSTEM
    // ===============================
    private void stopSystem() {
        isRunning = false;

        previewView.setVisibility(View.GONE);
        overlayView.setVisibility(View.GONE);

        handleStopNavigation();

        // show loading for 1 sec
        showStartAfterDelay(1000);
    }

    // ===============================
    // INIT SYSTEMS
    // ===============================
    private void initializeEchoSight() {
        try {
            speechOutput = new SpeechOutput(this);
            detector = new ObjectDetector(this);

            audioFeedback = new AudioFeedback();
            hapticManager = new HapticManager(this);
            feedbackController = new FeedbackController(hapticManager, audioFeedback);

            cameraManager = new CameraManager(this, this, previewView);

            voiceManager = new VoiceCommandManager(this, command -> {
                if (command.equals("START")) startSystem();
                if (command.equals("STOP")) stopSystem();
            });

            voiceManager.startListening();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStartNavigation() {
        speechOutput.speak("Navigation started");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraHardware();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void handleStopNavigation() {
        speechOutput.speak("Navigation stopped");

        if (cameraManager != null)
            cameraManager.stopCamera();

        if (overlayView != null)
            overlayView.setResults(null);

        if (hapticManager != null)
            hapticManager.stop();
    }

    private void startCameraHardware() {
        cameraManager.startCamera(
                new FrameAnalyzer(detector, speechOutput, overlayView, feedbackController)
        );
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
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
