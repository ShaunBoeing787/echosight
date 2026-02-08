package com.example.echosight;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.echosight.detection.ObjectDetector; // Unified package
import com.example.echosight.detection.DetectionResult;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ECHO_SIGHT";
    private ObjectDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Final Sector 2 Integration Test
        Log.e(TAG, "!!! ECHO-SIGHT STARTING: INITIALIZING SECTOR 2 !!!");

        try {
            // This now initializes the Interpreter AND loads the labels
            detector = new ObjectDetector(this);

            Log.e(TAG, "✅ SECTOR 2 SUCCESS: Brain is ready for camera feed.");
        } catch (Exception e) {
            Log.e(TAG, "❌ SECTOR 2 CRITICAL FAIL: " + e.getMessage());
            // This will tell you if labelmap.txt or detect.tflite is missing
            e.printStackTrace();
        }
        // ===== FINAL SECTOR 2 → 3 PIPELINE TEST =====
        DetectionResult fake = new DetectionResult(
                new android.graphics.RectF(100, 100, 400, 800), // big object
                0.9f,
                "person"
        );

        java.util.List<DetectionResult> fakeList =
                java.util.Collections.singletonList(fake);

        DetectionResult stable =
                com.example.echosight.logic.DetectionFilter.filter(fakeList);

        if (stable != null) {
            com.example.echosight.logic.ProximityEstimator.Proximity p =
                    com.example.echosight.logic.ProximityEstimator.estimateProximity(
                            stable, 1000
                    );

            com.example.echosight.logic.DirectionEstimator.Direction d =
                    com.example.echosight.logic.DirectionEstimator.estimateDirection(
                            stable, 1000
                    );

            Log.e(TAG, "🧠 FINAL LOGIC OUTPUT → Proximity: " + p + ", Direction: " + d);
        } else {
            Log.e(TAG, "❌ Detection filtered out");
        }
// ===== END TEST =====

    }
}