package com.example.echosight.camera;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

public class CameraManager {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private ProcessCameraProvider cameraProvider; // Store this to unbind later

    public CameraManager(Context context, LifecycleOwner owner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = owner;
        this.previewView = previewView;
    }
    @androidx.camera.core.ExperimentalGetImage
    public void startCamera(FrameAnalyzer analyzer) {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(context);

        future.addListener(() -> {
            try {
                cameraProvider = future.get(); // Assign to class variable

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(640, 480))
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        analyzer
                );

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        analysis
                );

                Log.d("ECHO_SIGHT", "Camera Bound to Lifecycle");

            } catch (Exception e) {
                Log.e("ECHO_SIGHT", "Camera Start Failed: " + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(context));
    }

    // NEW: The "Off Switch" for your voice command
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.d("ECHO_SIGHT", "Camera Unbound (Stopped)");
        }
    }
}