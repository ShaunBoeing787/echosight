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
import java.util.Random;

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
    private final Random random = new Random();

    private static final String[] FAR_PHRASES = {
            "%s is far ahead",
            "There is a %s ahead",
            "%s detected at a distance"
    };

    private static final String[] MID_PHRASES = {
            "%s is some steps ahead",
            "%s is in front of you",
            "You are approaching a %s"
    };

    private static final String[] NEAR_PHRASES = {
            "%s is very near you",
            "Careful, %s is right in front of you",
            "%s is extremely close",
            "Watch out, %s nearby"
    };

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
        // ---------- GATEKEEPER: SPEECH SUPPRESSION ----------
        // If the app is currently speaking (e.g., a lively description),
        // we skip processing entirely to avoid audio clutter.
        if (speechOutput.isSpeaking()) {
            imageProxy.close();
            return;
        }

        try {
            Image image = imageProxy.getImage();
            if (image == null) return;

            // Use your utility to convert the camera frame to a Bitmap
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) return;

            // ---------- DETECTION ----------
            List<DetectionResult> detections = detector.detect(bitmap);
            if (detections == null || detections.isEmpty()) {
                // Clear overlay if nothing is detected
                if (overlayView != null) overlayView.post(() -> overlayView.setResults(null));
                return;
            }

            // ---------- OVERLAY ----------
            if (overlayView != null) {
                overlayView.post(() -> overlayView.setResults(detections));
            }

            // ---------- STABILITY FILTER ----------
            DetectionResult stable = DetectionFilter.filter(detections);
            if (stable == null) return;

            // ---------- DIRECTION & PROXIMITY ----------
            DirectionEstimator.Direction direction =
                    DirectionEstimator.estimateDirection(stable, bitmap.getWidth());

            ProximityEstimator.Proximity proximity =
                    ProximityEstimator.estimateProximity(stable, bitmap.getWidth(), bitmap.getHeight());

            // ---------- ACCESSIBILITY FEEDBACK ----------
            try {
                FeedbackController.ProximityLevel level =
                        FeedbackController.ProximityLevel.valueOf(proximity.name());
                feedbackController.handleProximity(level);
            } catch (Exception e) {
                Log.e(TAG, "Feedback error", e);
            }

            // ---------- SEMANTIC & OBSTACLE LOGIC ----------
            SemanticMapper.SemanticType semanticType = SemanticMapper.classify(stable.getLabel());
            if (semanticType == SemanticMapper.SemanticType.IGNORE) return;

            if (!ObstacleDecision.isBlocking(stable, bitmap.getWidth(), bitmap.getHeight())) {
                return;
            }

            // ---------- SPEECH OUTPUT ----------
            long now = System.currentTimeMillis();
            String message = buildProximityMessage(stable.getLabel(), proximity);

            // Double-check isSpeaking right before calling tts.speak
            // to catch any sudden voice commands.
            if (!speechOutput.isSpeaking() && (now - lastSpeechTime > SPEECH_COOLDOWN || !message.equals(lastSpokenMessage))) {
                Log.d(TAG, "LOCAL DETECTION SPEAKING → " + message);
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

    private String buildProximityMessage(
            String label,
            ProximityEstimator.Proximity proximity
    ) {
        String[] phrases;

        switch (proximity) {
            case FAR:
                phrases = FAR_PHRASES;
                break;

            case MID:
                phrases = MID_PHRASES;
                break;

            case NEAR:
                phrases = NEAR_PHRASES;
                break;

            default:
                return label + " ahead";
        }

        String template = phrases[random.nextInt(phrases.length)];
        return String.format(template, label);
    }

}