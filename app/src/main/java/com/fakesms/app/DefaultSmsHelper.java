package com.fakesms.app;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.widget.Toast;

public class DefaultSmsHelper {

    public static boolean isDefaultSmsApp(Activity activity) {
        String defaultPackage = Telephony.Sms.getDefaultSmsPackage(activity);
        return activity.getPackageName().equals(defaultPackage);
    }

    public static void requestDefaultSmsApp(Activity activity, int requestCode) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) activity.getSystemService(android.content.Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        Toast.makeText(activity, "نقش پیامک از قبل گرفته شده", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    Toast.makeText(activity, "روش: RoleManager", Toast.LENGTH_SHORT).show();
                    activity.startActivityForResult(intent, requestCode);
                    return;
                } else {
                    Toast.makeText(activity, "RoleManager نقش SMS را پشتیبانی نمی‌کند", Toast.LENGTH_LONG).show();
                }
            }

            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            Toast.makeText(activity, "روش: ACTION_CHANGE_DEFAULT", Toast.LENGTH_SHORT).show();
            activity.startActivityForResult(intent, requestCode);

        } catch (Exception e) {
            Toast.makeText(activity, "خطا: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
