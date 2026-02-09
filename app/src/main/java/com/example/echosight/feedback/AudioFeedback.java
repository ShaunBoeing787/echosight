package com.example.echosight.feedback;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.util.Log;

public class AudioFeedback {

    private static final String TAG = "AudioFeedback";

    private static final int BEEP_DURATION_MS = 150;
    private static final int BEEP_GAP_MS = 200;

    private final ToneGenerator toneGenerator;
    private final Handler handler = new Handler();

    public AudioFeedback() {
        toneGenerator = new ToneGenerator(
                AudioManager.STREAM_MUSIC,
                100
        );
    }

    public void playFar() {
        Log.d(TAG, "Audio: FAR");
        playBeeps(1);
    }

    public void playMid() {
        Log.d(TAG, "Audio: MID");
        playBeeps(2);
    }

    public void playNear() {
        Log.d(TAG, "Audio: NEAR");
        playBeeps(3);
    }

    private void playBeeps(int count) {
        for (int i = 0; i < count; i++) {
            int delay = i * (BEEP_DURATION_MS + BEEP_GAP_MS);

            handler.postDelayed(() -> {
                toneGenerator.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        BEEP_DURATION_MS
                );
            }, delay);
        }
    }

    public void release() {
        toneGenerator.release();
    }
}