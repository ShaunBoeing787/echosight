package com.example.echosight.logic;

import android.graphics.RectF;
import android.util.Log;

import com.example.echosight.detection.DetectionResult;

import java.util.LinkedList;
import java.util.List;

public class DetectionFilter {

    private static final String TAG = "ECHO_SIGHT";

    private static final int HISTORY_SIZE = 5;
    private static final LinkedList<DetectionResult> history = new LinkedList<>();

    // Bounding box smoothing
    private static RectF lastSmoothedRect = null;
    private static final float SMOOTHING_FACTOR = 0.25f;

    // 🔒 HARD RULE: Minimum confidence = 60%
    private static final float MIN_CONFIDENCE = 0.60f;

    public static DetectionResult filter(List<DetectionResult> detections) {

        if (detections == null || detections.isEmpty()) {
            Log.e(TAG, "NO DETECTIONS IN FRAME");
            lastSmoothedRect = null;
            return null;
        }

        // 1️⃣ Pick the largest object with ≥ 60% confidence
        DetectionResult best = null;
        float maxArea = 0f;

        for (DetectionResult d : detections) {

            if (d == null || d.getBoundingBox() == null) continue;

            if (d.getConfidence() < MIN_CONFIDENCE) {
                continue;
            }

            RectF box = d.getBoundingBox();
            float area = Math.abs(box.width() * box.height());

            if (area > maxArea) {
                maxArea = area;
                best = d;
            }
        }

        if (best == null) {
            Log.e(TAG, "NO OBJECT ≥ 60% CONFIDENCE");
            return null;
        }

        Log.e(TAG,
                "FILTER CANDIDATE → " + best.getLabel()
                        + " | conf=" + String.format("%.2f", best.getConfidence())
        );

        // 2️⃣ Temporal stability check
        history.add(best);
        if (history.size() > HISTORY_SIZE) history.removeFirst();

        int sameCount = 0;
        for (DetectionResult d : history) {
            if (isSame(best, d)) sameCount++;
        }

        if (sameCount < 3) {
            Log.e(TAG, "UNSTABLE OBJECT — waiting for consistency");
            return null;
        }

        // 3️⃣ Bounding box smoothing
        RectF current = best.getBoundingBox();

        if (lastSmoothedRect == null) {
            lastSmoothedRect = new RectF(current);
        } else {
            lastSmoothedRect = new RectF(
                    current.left * SMOOTHING_FACTOR + lastSmoothedRect.left * (1f - SMOOTHING_FACTOR),
                    current.top * SMOOTHING_FACTOR + lastSmoothedRect.top * (1f - SMOOTHING_FACTOR),
                    current.right * SMOOTHING_FACTOR + lastSmoothedRect.right * (1f - SMOOTHING_FACTOR),
                    current.bottom * SMOOTHING_FACTOR + lastSmoothedRect.bottom * (1f - SMOOTHING_FACTOR)
            );
        }

        Log.e(TAG, "STABLE OBJECT CONFIRMED");

        return new DetectionResult(
                lastSmoothedRect,
                best.getConfidence(),
                best.getLabel()
        );
    }

    // Checks if two detections refer to the same object
    private static boolean isSame(DetectionResult a, DetectionResult b) {
        if (a == null || b == null) return false;
        if (!a.getLabel().equals(b.getLabel())) return false;

        RectF A = a.getBoundingBox();
        RectF B = b.getBoundingBox();
        if (A == null || B == null) return false;

        return Math.abs(A.centerX() - B.centerX()) < 150;
    }
}
