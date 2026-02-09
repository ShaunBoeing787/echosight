package com.example.echosight.feedback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Vibrator;
import android.os.Vibrator;

@SuppressWarnings("deprecation")
@SuppressLint("MissingPermission")
public class HapticManager {

    private final Vibrator vibrator;

    public HapticManager(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void vibrateFar() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(150);
    }

    public void vibrateMid() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 300, 400, 300};
        vibrator.vibrate(pattern, -1);
    }

    public void vibrateNear() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 150, 100, 150, 100, 150};
        vibrator.vibrate(pattern, -1);
    }

    public void stop() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}