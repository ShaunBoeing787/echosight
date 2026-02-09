package com.example.echosight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.echosight.camera.CameraManager;
import com.example.echosight.camera.FrameAnalyzer;
import com.example.echosight.detection.ObjectDetector;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ECHO_SIGHT";

    private PreviewView previewView;
    private ObjectDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "APP STARTED");

        previewView = findViewById(R.id.previewView);

        // Initialize detector (Sector 2)
        try {
            detector = new ObjectDetector(this);
            Log.e(TAG, "DETECTOR INITIALIZED");
        } catch (Exception e) {
            Log.e(TAG, "DETECTOR INIT FAILED: " + e.getMessage());
            e.printStackTrace();
        }

        checkCameraPermission();
    }

    /**
     * Check camera permission
     */
    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "PERMISSION ALREADY GRANTED");
            startCamera();

        } else {

            Log.e(TAG, "REQUESTING CAMERA PERMISSION");
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Start camera + analyzer
     */
    private void startCamera() {

        Log.e(TAG, "STARTING CAMERA");

        CameraManager manager =
                new CameraManager(this, this, previewView);

        // Pass detector into analyzer
        manager.startCamera(new FrameAnalyzer(detector));
    }

    /**
     * Permission launcher
     */
    private final androidx.activity.result.ActivityResultLauncher<String>
            requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.e(TAG, "PERMISSION GRANTED BY USER");
                            startCamera();
                        } else {
                            Log.e(TAG, "PERMISSION DENIED");
                            Toast.makeText(
                                    this,
                                    "Camera permission required",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
}
