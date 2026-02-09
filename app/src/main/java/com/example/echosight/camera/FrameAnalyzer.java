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
import com.example.echosight.logic.DetectionFilter;
import com.example.echosight.logic.DirectionEstimator;
import com.example.echosight.logic.ProximityEstimator;
import com.example.echosight.voice.SpeechOutput; // Required

import java.util.List;

@androidx.camera.core.ExperimentalGetImage
public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ECHO_SIGHT";
    private final ObjectDetector detector;
    private final SpeechOutput speechOutput; // The "Mouth"

    // Throttle detection (Prevents CPU overload)
    private long lastRunTime = 0;
    private static final long DETECT_INTERVAL = 250; // ms

    // Throttle speech (Prevents annoying repetition)
    private long lastSpeechTime = 0;
    private static final long SPEECH_COOLDOWN = 3000; // 3 seconds
    private String lastSpokenObject = "";

    public FrameAnalyzer(ObjectDetector detector, SpeechOutput speechOutput) {
        this.detector = detector;
        this.speechOutput = speechOutput;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            long now = System.currentTimeMillis();

            // 1. Throttle detection frequency
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

            // 2. Convert frame to Bitmap
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            // 3. Run TFLite Detection
            List<DetectionResult> detections = detector.detect(bitmap);

            // 4. Filter for stability
            DetectionResult stable = DetectionFilter.filter(detections);

            if (stable != null) {
                // 5. Calculate Direction and Proximity
                DirectionEstimator.Direction direction =
                        DirectionEstimator.estimateDirection(stable, bitmap.getWidth());

                ProximityEstimator.Proximity proximity =
                        ProximityEstimator.estimateProximity(stable, bitmap.getHeight());

                String currentObject = stable.getLabel();

                // 6. INTELLIGENT SPEECH LOGIC
                // Only speak if: 3 seconds have passed OR it's a brand new object
                if (now - lastSpeechTime > SPEECH_COOLDOWN || !currentObject.equals(lastSpokenObject)) {

                    String feedback = currentObject + " " + direction + " is " + proximity;
                    speechOutput.speak(feedback);

                    lastSpeechTime = now;
                    lastSpokenObject = currentObject;

                    Log.e(TAG, "ANNOUNCING: " + feedback);
                }

                Log.d(TAG, "STABLE: " + currentObject + " | " + direction + " | " + proximity);
            }

        } catch (Exception e) {
            Log.e(TAG, "Analyzer error", e);
        } finally {
            imageProxy.close();
        }
    }
}