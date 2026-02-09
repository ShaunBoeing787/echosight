package com.example.echosight;

import android.graphics.Bitmap;
import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EnvironmentNarrator {
    private GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public interface DescriptionCallback {
        void onDescriptionReady(String description);
        void onError(String error);
    }

    public EnvironmentNarrator(String apiKey) {
        try {
            GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", apiKey);
            this.model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("ECHO_AI", "Model Init Failed: " + e.getMessage());
        }
    }

    // Inside EnvironmentNarrator.java
    public void describeScene(Bitmap bitmap, DescriptionCallback callback) {
        String prompt =
                "You are a close, sighted friend standing next to me, speaking naturally and calmly. " +
                        "Describe what’s around the way a real person would, without dramatizing or sounding poetic. " +

                        "DO start with how the place feels overall, like whether it’s quiet, busy, focused, or relaxed. " +
                        "DO mention only socially or spatially useful information: who is nearby, how close they are, " +
                        "what they’re generally doing, and sometimes let me know whether anyone seems aware of me  " +

                        "DO NOT exaggerate, over-explain, or add decorative language. " +
                        "DO NOT list objects or describe everything you notice. " +
                        "DO NOT use phrases like 'I see', 'there is', or formal scene descriptions. " +

                        "If you mention color, keep it minimal and practical, using simple terms like warm or cool only when relevant. " +
                        "Respond in 2 to 5 short, natural sentences. " +
                        "Speak plainly, like you would to a friend who just wants to know what’s going on.";

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText(prompt)
                .build();

        // Generate content asynchronously
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                callback.onDescriptionReady(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t.getMessage());
            }
        }, Executors.newSingleThreadExecutor()); // Always use a background executor
    }
}