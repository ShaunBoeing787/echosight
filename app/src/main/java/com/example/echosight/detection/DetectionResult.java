package com.example.echosight.detection;

import android.graphics.RectF;

public class DetectionResult {
    private final String title;
    private final float confidence;
    private final RectF location;

    public DetectionResult(RectF location, float confidence, String title) {
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public String getLabel() { return title; }
    public float getConfidence() { return confidence; }
    public RectF getBoundingBox() { return location; }
}