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
    }
}