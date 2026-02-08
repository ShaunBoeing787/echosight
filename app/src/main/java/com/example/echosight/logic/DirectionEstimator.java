package com.example.echosight.logic;

import android.graphics.RectF;

import com.example.echosight.detection.DetectionResult;

/**
 * Estimates the horizontal direction of a detected object
 * relative to the camera view.
 *
 * Output zones:
 * LEFT | CENTER | RIGHT
 */
public class DirectionEstimator {

    /**
     * Enum representing direction zones.
     */
    public enum Direction {
        LEFT,
        CENTER,
        RIGHT
    }

    /**
     * Estimates direction based on bounding box position.
     *
     * @param detection    Single detection result
     * @param imageWidth   Width of the camera frame in pixels
     * @return Direction (LEFT, CENTER, RIGHT)
     */
    public static Direction estimateDirection(
            DetectionResult detection,
            int imageWidth
    ) {
        // Safety check
        if (detection == null || detection.getBoundingBox() == null) {
            return Direction.CENTER;
        }

        RectF box = detection.getBoundingBox();

        // Find center X of bounding box
        float boxCenterX = (box.left + box.right) / 2f;

        // Divide screen into 3 equal vertical zones
        float leftBoundary = imageWidth / 3f;
        float rightBoundary = 2f * imageWidth / 3f;

        if (boxCenterX < leftBoundary) {
            return Direction.LEFT;
        } else if (boxCenterX > rightBoundary) {
            return Direction.RIGHT;
        } else {
            return Direction.CENTER;
        }
    }
}
