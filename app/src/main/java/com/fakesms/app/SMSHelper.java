package com.fakesms.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

public class SMSHelper {
    private Context context;

    public SMSHelper(Context context) {
        this.context = context;
    }

    public void createFakeSMS(String phoneNumber, String messageText, long timestamp) {
        try {
            ContentValues values = new ContentValues();
            values.put("address", phoneNumber);
            values.put("body", messageText);
            values.put("date", timestamp);
            values.put("read", 1);
            values.put("type", 1);

            context.getContentResolver().insert(
                Uri.parse("content://sms/inbox"),
                values
            );

            Toast.makeText(context, "✅ پیام ایجاد شد!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "❌ خطا: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
