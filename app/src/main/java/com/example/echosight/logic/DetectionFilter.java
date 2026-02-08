package com.example.echosight.logic;

import android.graphics.RectF;

import com.example.echosight.detection.DetectionResult;

import java.util.List;

/**
 * Filters noisy detections to produce stable results.
 *
 * Rules:
 * 1. Confidence threshold
 * 2. Largest object only
 * 3. Require same result in 2 consecutive frames
 */
public class DetectionFilter {

    // Stores previous accepted detection for temporal stability
    private static DetectionResult lastDetection = null;

    /**
     * Filters a list of detections and returns a stable detection.
     *
     * @param detections List of detection results from model
     * @return Stable DetectionResult or null if filtered out
     */
    public static DetectionResult filter(List<DetectionResult> detections) {
        if (detections == null || detections.isEmpty()) {
            lastDetection = null;
            return null;
        }

        // Step 1: Confidence filter + largest object selection
        DetectionResult best = null;
        float maxArea = 0f;

        for (DetectionResult d : detections) {
            if (d == null || d.getBoundingBox() == null) {
                continue;
            }

            // Confidence gate
            if (d.getConfidence() < 0.5f) {
                continue;
            }

            RectF box = d.getBoundingBox();
            float area = (box.right - box.left) * (box.bottom - box.top);

            if (area > maxArea) {
                maxArea = area;
                best = d;
            }
        }

        if (best == null) {
            lastDetection = null;
            return null;
        }

        // Step 2: Temporal stability (2-frame consistency)
        if (lastDetection == null) {
            lastDetection = best;
            return null;
        }

        if (isSameDetection(best, lastDetection)) {
            return best;
        } else {
            lastDetection = best;
            return null;
        }
    }

    /**
     * Checks whether two detections are similar enough
     * to be considered the same object.
     */
    private static boolean isSameDetection(
            DetectionResult a,
            DetectionResult b
    ) {
        if (!a.getLabel().equals(b.getLabel())) {
            return false;
        }

        RectF boxA = a.getBoundingBox();
        RectF boxB = b.getBoundingBox();

        float centerAX = (boxA.left + boxA.right) / 2f;
        float centerBX = (boxB.left + boxB.right) / 2f;

        // Allow small horizontal movement (20% of width)
        float tolerance = (boxA.right - boxA.left) * 0.2f;

        return Math.abs(centerAX - centerBX) < tolerance;
    }
}
