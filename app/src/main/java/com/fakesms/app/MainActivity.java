package com.fakesms.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button btnRequest, btnCheckStatus, btnConfirm;
    private TextView tvStatus;
    private SMSHelper smsHelper;

    private String currentPhone = null;
    private JSONArray currentMessages = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRequest = findViewById(R.id.btnRequest);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        btnConfirm = findViewById(R.id.btnConfirm);
        tvStatus = findViewById(R.id.tvStatus);

        smsHelper = new SMSHelper(this);

        requestPermissions();

        btnRequest.setOnClickListener(v -> sendRequest());
        btnCheckStatus.setOnClickListener(v -> checkStatus());
        btnConfirm.setOnClickListener(v -> confirmAndCreate());

        btnConfirm.setEnabled(false);
    }

    private void requestPermissions() {
        String[] perms = {
                Manifest.permission.READ_SMS,
                Manifest.permission.WRITE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS
        };
        boolean needRequest = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            ActivityCompat.requestPermissions(this, perms, 100);
        }
    }

    private void sendRequest() {
        tvStatus.setText("در حال ارسال درخواست...");
        ApiClient.post("/request", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    tvStatus.setText("درخواست ارسال شد. منتظر تایید در تلگرام باشید.");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("خطا: " + error);
                    Toast.makeText(MainActivity.this, "خطا در ارسال درخواست", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkStatus() {
        tvStatus.setText("در حال بررسی وضعیت...");
        ApiClient.get("/check-status", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.getString("status");

                        switch (status) {
                            case "idle":
                                tvStatus.setText("وضعیت: آماده برای درخواست جدید");
                                break;
                            case "pending":
                                tvStatus.setText("وضعیت: منتظر تایید ادمین در تلگرام");
                                break;
                            case "waiting_phone":
                                tvStatus.setText("وضعیت: منتظر ثبت شماره در تلگرام");
                                break;
                            case "collecting":
                                tvStatus.setText("وضعیت: در حال دریافت پیام‌ها در تلگرام");
                                break;
                            case "ready":
                                tvStatus.setText("آماده است! برای دریافت پیام‌ها صبر کنید...");
                                fetchMessages();
                                break;
                        }

                    } catch (Exception e) {
                        tvStatus.setText("خطا در پردازش پاسخ");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvStatus.setText("خطا: " + error));
            }
        });
    }

    private void fetchMessages() {
        ApiClient.get("/get-messages", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getString("status").equals("ok")) {
                            currentPhone = obj.getString("phone");
                            currentMessages = obj.getJSONArray("messages");
                            tvStatus.setText("بارگذاری شد! " + currentMessages.length() + " پیام آماده است.\nشماره: " + currentPhone + "\n\nبرای ساخت پیام‌ها تایید کنید.");
                            btnConfirm.setEnabled(true);
                        }
                    } catch (Exception e) {
                        tvStatus.setText("خطا در دریافت پیام‌ها");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvStatus.setText("خطا: " + error));
            }
        });
    }

    private void confirmAndCreate() {
        if (currentPhone == null || currentMessages == null) {
            Toast.makeText(this, "ابتدا پیام‌ها را دریافت کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = smsHelper.createMessagesFromJson(currentPhone, currentMessages);
        tvStatus.setText(count + " پیام با موفقیت ساخته شد.");
        Toast.makeText(this, count + " پیام ساخته شد", Toast.LENGTH_SHORT).show();

        btnConfirm.setEnabled(false);
        currentPhone = null;
        currentMessages = null;

        ApiClient.post("/confirm-done", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {}
            @Override
            public void onError(String error) {}
        });
    }
}
