package com.example.echosight.detection;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ObjectDetector {

    private static final String TAG = "ECHO_SIGHT";
    private static final int INPUT_SIZE = 300;
    private static final int NUM_DETECTIONS = 10;

    private Interpreter interpreter;
    private List<String> labels = new ArrayList<>(); // To store the names

    public ObjectDetector(Context context) throws IOException {
        interpreter = new Interpreter(loadModel(context));
        loadLabels(context); // Load the words during initialization
        Log.e(TAG, "DETECTOR INITIALIZED WITH " + labels.size() + " LABELS");
    }

    // New method to read labelmap.txt
    private void loadLabels(Context context) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("labelmap.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
    }

    private ByteBuffer loadModel(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("detect.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(300 * 300 * 3);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[300 * 300];
        resized.getPixels(pixels, 0, 300, 0, 0, 300, 300);

        for (int pixel : pixels) {
            inputBuffer.put((byte) ((pixel >> 16) & 0xFF));
            inputBuffer.put((byte) ((pixel >> 8) & 0xFF));
            inputBuffer.put((byte) (pixel & 0xFF));
        }

        Object[] inputs = {inputBuffer};

        float[][][] boxes = new float[1][NUM_DETECTIONS][4];
        float[][] scores = new float[1][NUM_DETECTIONS];
        float[][] classes = new float[1][NUM_DETECTIONS];
        float[] num = new float[1];

        java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
        outputs.put(0, boxes);
        outputs.put(1, classes);
        outputs.put(2, scores);
        outputs.put(3, num);

        interpreter.runForMultipleInputsOutputs(inputs, outputs);

        List<DetectionResult> results = new ArrayList<>();

        for (int i = 0; i < NUM_DETECTIONS; i++) {
            float score = scores[0][i];
            if (score < 0.5f) continue;

            // FIX: Get the actual label name from our list
            int classIndex = (int) classes[0][i];
            String labelName = "Unknown";

            // Most SSD models use index + 1 for the label map
            if (classIndex + 1 < labels.size()) {
                labelName = labels.get(classIndex + 1);
            }

            float top = boxes[0][i][0] * bitmap.getHeight();
            float left = boxes[0][i][1] * bitmap.getWidth();
            float bottom = boxes[0][i][2] * bitmap.getHeight();
            float right = boxes[0][i][3] * bitmap.getWidth();

            RectF rect = new RectF(left, top, right, bottom);

            // FIX: Pass the real labelName instead of the string "object"
            results.add(new DetectionResult(rect, score, labelName));
        }

        return results;
    }
}