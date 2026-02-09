package com.example.echosight.logic;

import android.graphics.RectF;

import com.example.echosight.detection.DetectionResult;

/**
 * Estimates proximity (distance category) of a detected object
 * using a HYBRID heuristic:
 *
 * 1. Bounding box AREA ratio (object size)
 * 2. Bottom-of-frame ratio (ground-plane approximation)
 *
 * This is more stable than height-only estimation.
 */
public class ProximityEstimator {

    /**
     * Proximity zones.
     */
    public enum Proximity {
        FAR,
        MID,
        NEAR
    }

    /**
     * Hybrid proximity estimation.
     *
     * @param detection    Detection result
     * @param imageWidth   Width of camera frame (px)
     * @param imageHeight  Height of camera frame (px)
     * @return Proximity category
     */
    public static Proximity estimateProximity(
            DetectionResult detection,
            int imageWidth,
            int imageHeight
    ) {
        // ---------- SAFETY ----------
        if (detection == null
                || detection.getBoundingBox() == null
                || imageWidth <= 0
                || imageHeight <= 0) {
            return Proximity.FAR;
        }

        RectF box = detection.getBoundingBox();

        // ---------- AREA RATIO ----------
        float boxWidth = Math.max(0, box.right - box.left);
        float boxHeight = Math.max(0, box.bottom - box.top);

        float boxArea = boxWidth * boxHeight;
        float imageArea = imageWidth * imageHeight;

        float areaRatio = boxArea / imageArea;   // 0.0 → 1.0

        // ---------- BOTTOM POSITION ----------
        float bottomRatio = box.bottom / imageHeight; // 0.0 (top) → 1.0 (bottom)

        // ---------- HYBRID SCORE ----------
        // Area = size cue
        // Bottom = distance cue (ground plane)
        float score = (0.6f * areaRatio) + (0.4f * bottomRatio);

        // ---------- THRESHOLDS ----------
        if (score < 0.15f) {
            return Proximity.FAR;
        } else if (score < 0.35f) {
            return Proximity.MID;
        } else {
            return Proximity.NEAR;
        }
    }
}
