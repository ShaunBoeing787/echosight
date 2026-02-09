package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.view.PreviewView;

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

import android.widget.ImageButton;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

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

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) allGranted = false;
                }
                if (allGranted) initializeEchoSight();
                else Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            });

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
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
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
        Log.d(TAG, "SYSTEM: Starting minimal initialization...");
        try {
            speechOutput = new SpeechOutput(this);
            detector = new ObjectDetector(this);

            // Initialize feedback on Main Thread
            feedbackController = new FeedbackController(new HapticManager(this), new AudioFeedback());
            audioFeedback = new AudioFeedback();
            hapticManager = new HapticManager(this);
            feedbackController = new FeedbackController(hapticManager, audioFeedback);

            // Initialize Narrator with a TRY-CATCH block to prevent total crash
            try {
                String myKey = BuildConfig.GEMINI_API_KEY;
                environmentNarrator = new EnvironmentNarrator(myKey);
                Log.d(TAG, "GEMINI: Narrator initialized successfully.");
            } catch (Exception aiEx) {
                Log.e(TAG, "GEMINI ERROR: Failed to init Narrator: " + aiEx.getMessage());
            }

            // Initialize Hardware
            cameraManager = new CameraManager(this, this, previewView);
            voiceManager = new VoiceCommandManager(this, command -> {
                if (command.equals("START")) startSystem();
                if (command.equals("STOP")) stopSystem();
                if (command.equals("START")) handleStartNavigation();
                else if (command.equals("DESCRIBE")) handleDescribeEnvironment();
            });

            voiceManager.startListening();
            speechOutput.speak("Systems ready.");

        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR during init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDescribeEnvironment() {
        // 1. Check for initialization
        if (environmentNarrator == null) {
            speechOutput.speak("AI is not ready yet.");
            return;
        }

        // 2. Feedback (Main Thread)
        speechOutput.speak("Analyzing the room. Please hold still.");

        // 3. CAPTURE the bitmap on the Main Thread (CRITICAL FIX)
        final Bitmap fullFrame = previewView.getBitmap();

        if (fullFrame == null) {
            Log.e(TAG, "Capture failed: Bitmap is null");
            return;
        }

        // 4. Move AI WORK to Background Thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Resize (The "Pillow" logic)
                Bitmap tinyBitmap = Bitmap.createScaledBitmap(fullFrame, 640, 480, true);

                environmentNarrator.describeScene(tinyBitmap, new EnvironmentNarrator.DescriptionCallback() {
                    @Override
                    public void onDescriptionReady(String description) {
                        // 5. Back to Main Thread to speak the result
                        runOnUiThread(() -> speechOutput.speak(description));
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Gemini Error: " + error);
                        runOnUiThread(() -> speechOutput.speak("Scene analysis failed."));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Background Processing Error: " + e.getMessage());
            }
        });
    }

    private void handleStartNavigation() {
        speechOutput.speak("Navigation started");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraHardware();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        if (cameraManager != null) {
            speechOutput.speak("Navigation active.");
            cameraManager.startCamera(new FrameAnalyzer(detector, speechOutput, overlayView, feedbackController));
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
        speechOutput.speak("Navigation stopped.");
        if (cameraManager != null) cameraManager.stopCamera();
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
    }
}
