package com.example.echosight.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class VoiceCommandManager {
    private SpeechRecognizer speechRecognizer;
    private Intent intent;
    private CommandListener listener;

    public interface CommandListener {
        void onCommandReceived(String command) throws ExecutionException, InterruptedException;
    }

    public VoiceCommandManager(Context context, CommandListener listener) {
        this.listener = listener;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            // Inside your onResults method in VoiceCommandManager.java
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        String voiceInput = match.toLowerCase().trim();
                        Log.d("EchoSightVoice", "Heard: " + voiceInput);

                        if (voiceInput.contains("start")) {
                            try {
                                listener.onCommandReceived("START");
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        } else if (voiceInput.contains("stop") || voiceInput.contains("end")) {
                            try {
                                listener.onCommandReceived("STOP");
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        } else if (voiceInput.contains("describe") || voiceInput.contains("what")) {
                            try {
                                listener.onCommandReceived("DESCRIBE");
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
                startListening(); // Resume listening for the next command
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