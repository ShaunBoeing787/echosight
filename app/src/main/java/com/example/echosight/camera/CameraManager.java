package com.example.echosight.camera;

import android.content.Context;
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

    public CameraManager(Context context, LifecycleOwner owner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = owner;
        this.previewView = previewView;
    }

    public void startCamera(FrameAnalyzer analyzer) {



        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(context);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

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

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(context));
    }
}
