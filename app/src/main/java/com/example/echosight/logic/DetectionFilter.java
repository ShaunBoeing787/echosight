package com.example.echosight.logic;

import android.graphics.RectF;
import com.example.echosight.detection.DetectionResult;
import java.util.LinkedList;
import java.util.List;

public class DetectionFilter {

    private static final int HISTORY_SIZE = 5;
    private static final LinkedList<DetectionResult> history =
            new LinkedList<>();

    public static DetectionResult filter(List<DetectionResult> detections) {

        if (detections == null || detections.isEmpty()) {
            return null;
        }

        // pick largest confident object
        DetectionResult best = null;
        float maxArea = 0;

        for (DetectionResult d : detections) {
            if (d.getConfidence() < 0.4f) continue;

            RectF b = d.getBoundingBox();
            float area = (b.right - b.left) * (b.bottom - b.top);

            if (area > maxArea) {
                maxArea = area;
                best = d;
            }
        }

        if (best == null) return null;

        // store history
        history.add(best);
        if (history.size() > HISTORY_SIZE)
            history.removeFirst();

        // count similar detections
        int count = 0;
        for (DetectionResult d : history) {
            if (isSame(best, d)) count++;
        }

        if (count >= 3) {
            return best; // stable enough
        }

        return null;
    }

    private static boolean isSame(
            DetectionResult a,
            DetectionResult b
    ) {
        if (!a.getLabel().equals(b.getLabel()))
            return false;

        RectF A = a.getBoundingBox();
        RectF B = b.getBoundingBox();

        float centerA = (A.left + A.right) / 2f;
        float centerB = (B.left + B.right) / 2f;

        return Math.abs(centerA - centerB) < 100; // tolerance
    }
}
