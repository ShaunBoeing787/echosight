package com.example.echosight.detection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import com.example.echosight.utils.Constants;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetector {
    private final Interpreter tflite;
    private final List<String> labels = new ArrayList<>();
    private final int NUM_DETECTIONS = 10;

    public ObjectDetector(Context context) throws Exception {
        tflite = new Interpreter(TFLiteModel.loadModelFile(context.getAssets(), Constants.MODEL_PATH));
        loadLabels(context);
    }

    private void loadLabels(Context context) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(Constants.LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        // Prepare input: Resize image to 300x300
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, Constants.INPUT_SIZE, Constants.INPUT_SIZE, false);

        // Prepare output buffers
        float[][][] outputLocations = new float[1][NUM_DETECTIONS][4];
        float[][] outputClasses = new float[1][NUM_DETECTIONS];
        float[][] outputScores = new float[1][NUM_DETECTIONS];
        float[] numDetections = new float[1];

        Object[] inputs = {resizedBitmap};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputLocations);
        outputs.put(1, outputClasses);
        outputs.put(2, outputScores);
        outputs.put(3, numDetections);

        // Run Inference
        tflite.runForMultipleInputsOutputs(inputs, outputs);

        // Pack results
        List<DetectionResult> results = new ArrayList<>();
        for (int i = 0; i < NUM_DETECTIONS; i++) {
            if (outputScores[0][i] > Constants.CONFIDENCE_THRESHOLD) {
                int classIndex = (int) outputClasses[0][i];
                String label = (classIndex < labels.size()) ? labels.get(classIndex) : "Unknown";

                results.add(new DetectionResult(
                        new RectF(outputLocations[0][i][1], outputLocations[0][i][0],
                                outputLocations[0][i][3], outputLocations[0][i][2]),
                        outputScores[0][i],
                        label
                ));
            }
        }
        return results;
    }
}