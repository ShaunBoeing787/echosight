package com.example.echosight.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class VoiceCommandManager {
    private SpeechRecognizer speechRecognizer;
    private Intent intent;
    private CommandListener listener;

    public interface CommandListener {
        void onCommandReceived(String command);
    }

    public VoiceCommandManager(Context context, CommandListener listener) {
        this.listener = listener;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    // IMPORTANT: This tells us what the phone actually heard
                    Log.d("EchoSightVoice", "Full list of heard words: " + matches.toString());

                    for (String match : matches) {
                        String voiceInput = match.toLowerCase().trim();

                        // Log each individual word check
                        Log.d("EchoSightVoice", "Checking word: " + voiceInput);

                        if (voiceInput.contains("start")) {
                            Log.d("EchoSightVoice", "Confirmed: START");
                            listener.onCommandReceived("START");
                        }
                        // Using .equals or .contains to be safe
                        else if (voiceInput.contains("end") || voiceInput.contains("pause")) {
                            Log.d("EchoSightVoice", "Confirmed: STOP");
                            listener.onCommandReceived("STOP");
                        }
                    }
                }
                startListening();
            }

            @Override public void onError(int error) { startListening(); }
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening() {
        speechRecognizer.startListening(intent);
    }

    public void stop() {
        speechRecognizer.destroy();
    }
}