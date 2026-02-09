package com.example.echosight.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Handles all verbal communication from the app.
 */
public class SpeechOutput {
    private TextToSpeech tts;
    private boolean isReady = false;

    public SpeechOutput(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isReady = true;
                // Initial test message
                speak("Echo Sight initialized");
            }
        });
    }

    public void speak(String text) {
        if (isReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}