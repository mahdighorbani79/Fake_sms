package com.fakesms.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

public class SMSHelper {
    private Context context;

    public SMSHelper(Context context) {
        this.context = context;
    }

    public int createMessagesFromJson(String phoneNumber, JSONArray messages) {
        int successCount = 0;
        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject msg = messages.getJSONObject(i);
                String text = msg.getString("text");
                long timestamp = msg.getLong("timestamp");

                ContentValues values = new ContentValues();
                values.put("address", phoneNumber);
                values.put("body", text);
                values.put("date", timestamp);
                values.put("read", 1);
                values.put("type", 1);

                context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
                successCount++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return successCount;
    }
}
