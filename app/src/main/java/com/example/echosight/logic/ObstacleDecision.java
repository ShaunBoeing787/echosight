package com.example.echosight.logic;

import android.graphics.RectF;
import com.example.echosight.detection.DetectionResult;

public class ObstacleDecision {

    public static boolean isBlocking(
            DetectionResult d,
            int frameWidth,
            int frameHeight
    ) {
        RectF box = d.getBoundingBox();

        float areaRatio =
                (box.width() * box.height()) /
                        (frameWidth * frameHeight);

        float centerX = box.centerX();
        float left = frameWidth * 0.3f;
        float right = frameWidth * 0.7f;

        boolean inPath = centerX > left && centerX < right;
        boolean largeEnough = areaRatio > 0.08f;

        return inPath && largeEnough;
    }
}
