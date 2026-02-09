package com.example.echosight.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

public class SpeechOutput {
    private TextToSpeech tts;
    private boolean isSpeaking = false;

    public SpeechOutput(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
                setupProgressListener();
            }
        });
    }

    private void setupProgressListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                isSpeaking = true; // Detection will pause
            }

            @Override
            public void onDone(String utteranceId) {
                isSpeaking = false; // Detection will resume
            }

            @Override
            public void onError(String utteranceId) {
                isSpeaking = false;
            }
        });
    }

    public void speak(String text) {
        // We provide a unique ID to trigger the ProgressListener
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID");
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}