package com.example.echosight.detection;

import android.graphics.RectF;

public class DetectionResult {
    private final String title;
    private final float confidence;
    private final RectF location;

    public DetectionResult(String title, float confidence, RectF location) {
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public String getTitle() { return title; }
    public float getConfidence() { return confidence; }
    public RectF getLocation() { return location; }
}