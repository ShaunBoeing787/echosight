package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.example.echosight.EnvironmentNarrator;
import com.example.echosight.voice.SpeechOutput;
import com.example.echosight.voice.VoiceCommandManager;
import com.example.echosight.utils.PermissionUtils;

import java.util.concurrent.Executors; // Only one import needed

@ExperimentalGetImage
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ECHO_SIGHT";

    private PreviewView previewView;
    private OverlayView overlayView;
    private ImageView loadingGif;
    private Button btnToggle;

    private ObjectDetector detector;
    private SpeechOutput speechOutput;
    private VoiceCommandManager voiceManager;
    private CameraManager cameraManager;
    private EnvironmentNarrator environmentNarrator;
    private FeedbackController feedbackController;
    private AudioFeedback audioFeedback; // Declare here so it is accessible everywhere
    private HapticManager hapticManager;

    private boolean isRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) allGranted = false;
                }
                if (allGranted) initializeEchoSight();
                else Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        loadingGif = findViewById(R.id.loadingGif);
        btnToggle = findViewById(R.id.btnToggleScan);
        ImageButton btnHelp = findViewById(R.id.btnHelp);

        Glide.with(this).asGif().load(R.drawable.loading).into(loadingGif);

        showStartAfterDelay(2000);

        btnToggle.setOnClickListener(v -> {
            if (!isRunning) startSystem();
            else stopSystem();
        });

        btnHelp.setOnClickListener(v -> {
            if (speechOutput != null) speechOutput.speak("Say start to begin navigation. Say stop to end.");
        });

        if (PermissionUtils.hasPermissions(this)) {
            initializeEchoSight();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }
    }

    private void initializeEchoSight() {
        Log.d(TAG, "SYSTEM: Starting initialization...");
        try {
            speechOutput = new SpeechOutput(this);
            detector = new ObjectDetector(this);

            // FIX: Initialize the class-level variables, don't re-declare them
            audioFeedback = new AudioFeedback();
            hapticManager = new HapticManager(this);
            feedbackController = new FeedbackController(hapticManager, audioFeedback);

            try {
                String myKey = BuildConfig.GEMINI_API_KEY;
                environmentNarrator = new EnvironmentNarrator(myKey);
                Log.d(TAG, "GEMINI: Narrator initialized.");
            } catch (Exception aiEx) {
                Log.e(TAG, "GEMINI ERROR: " + aiEx.getMessage());
            }

            cameraManager = new CameraManager(this, this, previewView);
            voiceManager = new VoiceCommandManager(this, command -> {
                if (command.equals("START")) startSystem();
                else if (command.equals("STOP")) stopSystem();
                else if (command.equals("DESCRIBE")) handleDescribeEnvironment();
            });

            voiceManager.startListening();
            speechOutput.speak("Systems ready.");

        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR during init: " + e.getMessage());
        }
    }

    private void startSystem() {
        isRunning = true;
        previewView.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.VISIBLE);
        loadingGif.setVisibility(View.GONE);

        btnToggle.setText("STOP");
        btnToggle.setTextColor(0xFFFFCDD2);
        btnToggle.setBackground(ContextCompat.getDrawable(this, R.drawable.stop_button_bg));

        handleStartNavigation();
    }

    private void stopSystem() {
        isRunning = false;
        previewView.setVisibility(View.GONE);
        overlayView.setVisibility(View.GONE);
        handleStopNavigation();
        showStartAfterDelay(1000);
    }

    private void showStartAfterDelay(int delayMs) {
        btnToggle.setVisibility(View.GONE);
        loadingGif.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            btnToggle.setVisibility(View.VISIBLE);
            btnToggle.setText("START");
            btnToggle.setTextColor(0xFFB3E5FC);
            btnToggle.setBackground(ContextCompat.getDrawable(this, R.drawable.start_button_bg));
        }, delayMs);
    }

    private void handleDescribeEnvironment() {
        if (environmentNarrator == null) {
            speechOutput.speak("AI is not ready yet.");
            return;
        }

        speechOutput.speak("Analyzing the room. Please hold still.");
        final Bitmap fullFrame = previewView.getBitmap();

        if (fullFrame == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Bitmap tinyBitmap = Bitmap.createScaledBitmap(fullFrame, 640, 480, true);
                environmentNarrator.describeScene(tinyBitmap, new EnvironmentNarrator.DescriptionCallback() {
                    @Override
                    public void onDescriptionReady(String description) {
                        runOnUiThread(() -> speechOutput.speak(description));
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> speechOutput.speak("Analysis failed."));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "AI Error: " + e.getMessage());
            }
        });
    }

    private void handleStartNavigation() {
        speechOutput.speak("Navigation started");
        if (cameraManager != null) {
            cameraManager.startCamera(new FrameAnalyzer(detector, speechOutput, overlayView, feedbackController));
        }
    }

    private void handleStopNavigation() {
        speechOutput.speak("Navigation stopped");
        if (cameraManager != null) cameraManager.stopCamera();
        if (overlayView != null) overlayView.setResults(null);
        if (hapticManager != null) hapticManager.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechOutput != null) speechOutput.shutdown();
        if (voiceManager != null) voiceManager.stop();
        if (cameraManager != null) cameraManager.stopCamera();
        if (audioFeedback != null) audioFeedback.release();
    }
}