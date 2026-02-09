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

import java.util.List;

/**
 * FULL PIPELINE:
 *
 * Camera → Bitmap → ObjectDetector → Filter → Direction → Proximity
 *
 * Designed for shaky handheld camera (visually impaired usage)
 */
@ExperimentalGetImage
public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ECHO_SIGHT";

    private final ObjectDetector detector;

    // run detector only every X ms (prevents overload + improves stability)
    private long lastRunTime = 0;
    private static final long DETECT_INTERVAL = 250; // ms

    public FrameAnalyzer(ObjectDetector detector) {
        this.detector = detector;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        try {
            long now = System.currentTimeMillis();

            // throttle detection
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

            // Convert camera frame → bitmap
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            Log.e(TAG, "FRAME RECEIVED");

            // ==============================
            // RUN TFLITE DETECTOR
            // ==============================
            List<DetectionResult> detections = detector.detect(bitmap);
            Log.e(TAG, "RUNNING DETECTOR");

            // ==============================
            // FILTER (stability smoothing)
            // ==============================
            DetectionResult stable =
                    DetectionFilter.filter(detections);

            if (stable != null) {

                Log.e(TAG,
                        "STABLE: " + stable.getLabel()
                                + " (" + stable.getConfidence() + ")");

                // ==============================
                // DIRECTION
                // ==============================
                DirectionEstimator.Direction direction =
                        DirectionEstimator.estimateDirection(
                                stable,
                                bitmap.getWidth()
                        );

                // ==============================
                // PROXIMITY
                // ==============================
                ProximityEstimator.Proximity proximity =
                        ProximityEstimator.estimateProximity(
                                stable,
                                bitmap.getHeight()
                        );

                Log.e(TAG,
                        "RESULT → "
                                + direction
                                + " | "
                                + proximity);
            }

        } catch (Exception e) {
            Log.e(TAG, "Analyzer error", e);
        } finally {
            imageProxy.close();
        }
    }
}
