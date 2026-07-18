package com.fakesms.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

public class DefaultSmsHelper {

    public static boolean isDefaultSmsApp(Activity activity) {
        String defaultPackage = Telephony.Sms.getDefaultSmsPackage(activity);
        return activity.getPackageName().equals(defaultPackage);
    }

    public static void requestDefaultSmsApp(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
