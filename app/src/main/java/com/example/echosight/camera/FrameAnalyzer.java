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
import com.example.echosight.logic.SemanticMapper;
import com.example.echosight.logic.ObstacleDecision;
import com.example.echosight.voice.SpeechOutput;

import java.util.List;

@ExperimentalGetImage
public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ECHO_SIGHT";

    private final ObjectDetector detector;
    private final SpeechOutput speechOutput;
    private final OverlayView overlayView;
    private final FeedbackController feedbackController;

    private long lastSpeechTime = 0;
    private static final long SPEECH_COOLDOWN = 3000;
    private String lastSpokenMessage = "";

    public FrameAnalyzer(
            ObjectDetector detector,
            SpeechOutput speechOutput,
            OverlayView overlayView,
            FeedbackController feedbackController
    ) {
        this.detector = detector;
        this.speechOutput = speechOutput;
        this.overlayView = overlayView;
        this.feedbackController = feedbackController;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        try {
            Image image = imageProxy.getImage();
            if (image == null) return;

            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) return;

            // ---------- DETECTION ----------
            List<DetectionResult> detections = detector.detect(bitmap);
            if (detections == null || detections.isEmpty()) return;

            // ---------- OVERLAY ----------
            if (overlayView != null) {
                overlayView.post(() -> overlayView.setResults(detections));
            }

            // ---------- STABILITY ----------
            DetectionResult stable = DetectionFilter.filter(detections);
            if (stable == null) return;

            Log.e(TAG, "STABLE → " + stable.getLabel()
                    + " (" + stable.getConfidence() + ")");

            // ---------- DIRECTION & PROXIMITY ----------
            DirectionEstimator.Direction direction =
                    DirectionEstimator.estimateDirection(
                            stable, bitmap.getWidth());

            ProximityEstimator.Proximity proximity =
                    ProximityEstimator.estimateProximity(
                            stable, bitmap.getWidth(),bitmap.getHeight());

            Log.e(TAG, "PROXIMITY → " + proximity);
            Log.e(TAG, "DIRECTION → " + direction);

            // ---------- HAPTICS (OPTIONAL, KEEP FOR ACCESSIBILITY) ----------
            try {
                FeedbackController.ProximityLevel level =
                        FeedbackController.ProximityLevel.valueOf(proximity.name());
                feedbackController.handleProximity(level);
            } catch (Exception e) {
                Log.e(TAG, "Feedback error", e);
            }

            // ---------- SEMANTIC FILTER ----------
            SemanticMapper.SemanticType semanticType =
                    SemanticMapper.classify(stable.getLabel());

            if (semanticType == SemanticMapper.SemanticType.IGNORE) {
                Log.e(TAG, "IGNORED → " + stable.getLabel());
                return;
            }

            // ---------- OBSTACLE DECISION ----------
            boolean blocking = ObstacleDecision.isBlocking(
                    stable,
                    bitmap.getWidth(),
                    bitmap.getHeight()
            );

            if (!blocking) {
                Log.e(TAG, "FREE SPACE → " + stable.getLabel());
                return;
            }

            // ---------- SPEECH (PROXIMITY SENTENCE) ----------
            long now = System.currentTimeMillis();
            String message = buildProximityMessage(
                    stable.getLabel(),
                    proximity
            );

            if (now - lastSpeechTime > SPEECH_COOLDOWN
                    || !message.equals(lastSpokenMessage)) {

                Log.e(TAG, "SPEAKING → " + message);
                speechOutput.speak(message);

                lastSpeechTime = now;
                lastSpokenMessage = message;
            }

        } catch (Exception e) {
            Log.e(TAG, "Analyzer error", e);
        } finally {
            imageProxy.close();
        }
    }

    // ---------- PROXIMITY SPEECH BUILDER ----------
    private String buildProximityMessage(
            String label,
            ProximityEstimator.Proximity proximity
    ) {
        switch (proximity) {
            case FAR:
                return label + " is far ahead";
            case MID:
                return label + " is some steps ahead";
            case NEAR:
                return label + " is very near you";
            default:
                return label + " ahead";
        }
    }
}
