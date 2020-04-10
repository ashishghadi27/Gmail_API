package com.ran.voicemailclient.Activities.helper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

//import android.support.annotation.NonNull;
//import android.support.v4.content.ContextCompat;

/**
 * Created by Mustafa on 16-06-2016.
 */
public class Utils {

    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    public static boolean isNotEmpty(TextView editText) {
        return editText.getText().toString().trim().length() > 0;
    }

    @NonNull
    public static String getString(TextView editText) {
        return editText.getText().toString().trim();
    }

    public static boolean checkPermission(Context context, String permission) {
        if (isMarshmallow()) {
            int result = ContextCompat.checkSelfPermission(context, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static boolean isMarshmallow() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

}
