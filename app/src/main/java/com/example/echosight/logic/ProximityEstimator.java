package com.example.echosight.logic;

import android.graphics.RectF;

import com.example.echosight.detection.DetectionResult;

/**
 * Estimates proximity (distance category) of a detected object
 * based on bounding box height relative to image height.
 */
public class ProximityEstimator {

    /**
     * Enum representing proximity zones.
     */
    public enum Proximity {
        FAR,
        MID,
        NEAR
    }

    /**
     * Estimates proximity using bounding box height ratio.
     *
     * @param detection   Single detection result
     * @param imageHeight Height of the camera frame in pixels
     * @return Proximity (FAR, MID, NEAR)
     */
    public static Proximity estimateProximity(
            DetectionResult detection,
            int imageHeight
    ) {
        // Safety checks
        if (detection == null || detection.getBoundingBox() == null || imageHeight <= 0) {
            return Proximity.FAR;
        }

        RectF box = detection.getBoundingBox();

        // Bounding box height in pixels
        float boxHeight = box.bottom - box.top;

        // Ratio of object size to image height
        float ratio = boxHeight / (float) imageHeight;

        if (ratio < 0.25f) {
            return Proximity.FAR;
        } else if (ratio < 0.5f) {
            return Proximity.MID;
        } else {
            return Proximity.NEAR;
        }
    }
}
