package com.example.echosight.feedback;

import android.util.Log;
import android.util.Log;


public class FeedbackController {

    private static final String TAG = "FeedbackController";

    public enum ProximityLevel {
        FAR,
        MID,
        NEAR
    }

    private final HapticManager hapticManager;
    private final AudioFeedback audioFeedback;

    private ProximityLevel lastLevel = null;

    public FeedbackController(HapticManager hapticManager,
                              AudioFeedback audioFeedback) {
        this.hapticManager = hapticManager;
        this.audioFeedback = audioFeedback;
    }

    public void handleProximity(ProximityLevel level) {

        // Debounce: ignore repeated values
        if (level == lastLevel) {
            return;
        }

        lastLevel = level;

        Log.d(TAG, "Proximity: " + level);

        switch (level) {
            case FAR:
                hapticManager.vibrateFar();
                audioFeedback.playFar();
                break;

            case MID:
                hapticManager.vibrateMid();
                audioFeedback.playMid();
                break;

            case NEAR:
                hapticManager.vibrateNear();
                audioFeedback.playNear();
                break;
        }
    }
}