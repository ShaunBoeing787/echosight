package com.example.echosight.camera;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.echosight.detection.DetectionResult;
import com.example.echosight.detection.ObjectDetector;
import com.example.echosight.feedback.FeedbackController;
import com.example.echosight.logic.DetectionFilter;
import com.example.echosight.logic.DirectionEstimator;
import com.example.echosight.logic.ProximityEstimator;
import com.example.echosight.voice.SpeechOutput;

import java.util.List;

@androidx.camera.core.ExperimentalGetImage
public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ECHO_SIGHT";
    private final ObjectDetector detector;
    private final SpeechOutput speechOutput;
    private final OverlayView overlayView;
    private final FeedbackController feedbackController; // Added for haptics/audio

    private long lastRunTime = 0;
    private static final long DETECT_INTERVAL = 0;

    private long lastSpeechTime = 0;
    private static final long SPEECH_COOLDOWN = 3000;
    private String lastSpokenObject = "";

    public FrameAnalyzer(ObjectDetector detector,
                         SpeechOutput speechOutput,
                         OverlayView overlayView,
                         FeedbackController feedbackController) {
        this.detector = detector;
        this.speechOutput = speechOutput;
        this.overlayView = overlayView;
        this.feedbackController = feedbackController;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            long now = System.currentTimeMillis();

            if (now - lastRunTime < DETECT_INTERVAL) {
                imageProxy.close();
                return;
            }
            lastRunTime = now;

            Image image = imageProxy.getImage();
            if (image == null) {
                imageProxy.close();
                return;
            }

            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            // 1. Detection
            List<DetectionResult> detections = detector.detect(bitmap);

            // 2. Visual Feedback (Overlay)
            if (overlayView != null) {
                overlayView.post(() -> overlayView.setResults(detections));
            }

            // 3. Logic & Sensory Feedback
            DetectionResult stable = DetectionFilter.filter(detections);

            if (stable != null) {
                DirectionEstimator.Direction direction =
                        DirectionEstimator.estimateDirection(stable, bitmap.getWidth());

                ProximityEstimator.Proximity proximity =
                        ProximityEstimator.estimateProximity(stable, bitmap.getHeight());

                // TRIGGER HAPTICS AND BEEPS
                // Maps Proximity (Logic) to ProximityLevel (Feedback)
                try {
                    FeedbackController.ProximityLevel level =
                            FeedbackController.ProximityLevel.valueOf(proximity.name());
                    feedbackController.handleProximity(level);
                } catch (Exception e) {
                    Log.e(TAG, "Feedback Mapping Error: " + e.getMessage());
                }

                // SPEECH LOGIC
                String currentObject = stable.getLabel();
                if (now - lastSpeechTime > SPEECH_COOLDOWN || !currentObject.equals(lastSpokenObject)) {
                    speechOutput.speak(currentObject + " " + direction);
                    lastSpeechTime = now;
                    lastSpokenObject = currentObject;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Analyzer error", e);
        } finally {
            imageProxy.close();
        }
    }
}