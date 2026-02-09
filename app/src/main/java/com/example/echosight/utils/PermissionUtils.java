package com.example.echosight.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static final int REQUEST_CODE = 101;

    public static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    public static boolean hasPermissions(Activity activity) {
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void ask(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE);
    }
}