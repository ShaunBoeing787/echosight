package com.example.echosight.logic;

import android.graphics.RectF;
import com.example.echosight.detection.DetectionResult;
import java.util.LinkedList;
import java.util.List;

public class DetectionFilter {

    private static final int HISTORY_SIZE = 5;
    private static final LinkedList<DetectionResult> history = new LinkedList<>();

    // NEW: Variable to store the smoothed position across frames
    private static RectF lastSmoothedRect = null;
    // NEW: Smoothing factor (0.1 = very slow/stable, 0.3 = smooth, 1.0 = raw/shaky)
    private static final float SMOOTHING_FACTOR = 0.25f;

    public static DetectionResult filter(List<DetectionResult> detections) {

        if (detections == null || detections.isEmpty()) {
            lastSmoothedRect = null; // Reset if nothing is seen
            return null;
        }

        // 1. Pick largest confident object
        DetectionResult best = null;
        float maxArea = 0;

        for (DetectionResult d : detections) {
            if (d.getConfidence() < 0.4f) continue;

            RectF b = d.getBoundingBox();
            float area = Math.abs(b.width() * b.height());

            if (area > maxArea) {
                maxArea = area;
                best = d;
            }
        }

        if (best == null) return null;

        // 2. History Check (for flickering suppression)
        history.add(best);
        if (history.size() > HISTORY_SIZE) history.removeFirst();

        int sameCount = 0;
        for (DetectionResult d : history) {
            if (isSame(best, d)) sameCount++;
        }

        // 3. APPLY TEMPORAL SMOOTHING
        if (sameCount >= 3) {
            RectF currentRect = best.getBoundingBox();

            if (lastSmoothedRect == null) {
                lastSmoothedRect = currentRect;
            } else {
                // Exponential Moving Average Math:
                // NewPos = (Current * Factor) + (Last * (1 - Factor))
                lastSmoothedRect = new RectF(
                        (currentRect.left * SMOOTHING_FACTOR) + (lastSmoothedRect.left * (1 - SMOOTHING_FACTOR)),
                        (currentRect.top * SMOOTHING_FACTOR) + (lastSmoothedRect.top * (1 - SMOOTHING_FACTOR)),
                        (currentRect.right * SMOOTHING_FACTOR) + (lastSmoothedRect.right * (1 - SMOOTHING_FACTOR)),
                        (currentRect.bottom * SMOOTHING_FACTOR) + (lastSmoothedRect.bottom * (1 - SMOOTHING_FACTOR))
                );
            }

            // Return a new result with the smoothed coordinates
            return new DetectionResult(lastSmoothedRect, best.getConfidence(), best.getLabel());
        }

        return null;
    }

    private static boolean isSame(DetectionResult a, DetectionResult b) {
        if (!a.getLabel().equals(b.getLabel())) return false;

        RectF A = a.getBoundingBox();
        RectF B = b.getBoundingBox();

        // Check if the centers are relatively close to consider it the "same" object
        return Math.abs(A.centerX() - B.centerX()) < 150;
    }
}