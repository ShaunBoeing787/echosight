package com.example.echosight.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.example.echosight.detection.DetectionResult;
import java.util.List;

public class OverlayView extends View {
    private List<DetectionResult> results;
    private final Paint paint = new Paint();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8.0f);
        paint.setTextSize(50.0f);
    }

    public void setResults(List<DetectionResult> results) {
        this.results = results;
        invalidate(); // Force redraw on the UI thread
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null) return;

        // Inside OverlayView.java -> onDraw()
        for (DetectionResult res : results) {
            // FIXED: Using getBoundingBox()
            canvas.drawRect(res.getBoundingBox(), paint);

            // FIXED: Using getLabel() and getBoundingBox()
            canvas.drawText(res.getLabel() + " " + Math.round(res.getConfidence() * 100) + "%",
                    res.getBoundingBox().left, res.getBoundingBox().top, paint);
        }
    }
}