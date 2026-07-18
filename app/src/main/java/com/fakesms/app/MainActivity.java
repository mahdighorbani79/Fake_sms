package com.fakesms.app;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnRequest, btnCheckStatus, btnConfirm;
    private TextView tvStatus;
    private SMSHelper smsHelper;

    private String currentRequestId = null;
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
                "android.permission.READ_SMS",
                "android.permission.WRITE_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS"
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

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void sendRequest() {
        tvStatus.setText("در حال ارسال درخواست...");
        currentRequestId = null;
        btnConfirm.setEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("device", getDeviceName());

            ApiClient.post("/request", body.toString(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject obj = new JSONObject(response);
                            currentRequestId = obj.getString("request_id");
                            tvStatus.setText("درخواست ارسال شد. منتظر تایید در تلگرام باشید.");
                        } catch (Exception e) {
                            tvStatus.setText("خطا در پردازش پاسخ سرور");
                        }
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
        } catch (Exception e) {
            tvStatus.setText("خطا: " + e.getMessage());
        }
    }

    private void checkStatus() {
        if (currentRequestId == null) {
            Toast.makeText(this, "ابتدا یک درخواست جدید ارسال کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("در حال بررسی وضعیت...");
        ApiClient.get("/check-status?request_id=" + currentRequestId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        String status = obj.getString("status");

                        switch (status) {
                            case "expired":
                                tvStatus.setText("درخواست منقضی شده. دوباره درخواست بدهید.");
                                currentRequestId = null;
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
                                tvStatus.setText("آماده است! در حال دریافت پیام‌ها...");
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
        ApiClient.get("/get-messages?request_id=" + currentRequestId, new ApiClient.ApiCallback() {
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

        if (!DefaultSmsHelper.isDefaultSmsApp(this)) {
            Toast.makeText(this, "ابتدا این اپ را به عنوان اپ پیش‌فرض پیامک انتخاب کنید", Toast.LENGTH_LONG).show();
            tvStatus.setText("در حال درخواست مجوز پیش‌فرض پیامک...");
            DefaultSmsHelper.requestDefaultSmsApp(this, 200);
            return;
        }

        int count = smsHelper.createMessagesFromJson(currentPhone, currentMessages);

        if (count == 0) {
            tvStatus.setText("هیچ پیامی ساخته نشد. خطایی رخ داده است.");
            Toast.makeText(this, "خطا در ساخت پیام‌ها", Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText(count + " پیام با موفقیت ساخته شد.");
        Toast.makeText(this, count + " پیام ساخته شد", Toast.LENGTH_SHORT).show();

        btnConfirm.setEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("request_id", currentRequestId);
            ApiClient.post("/confirm-done", body.toString(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {}
                @Override
                public void onError(String error) {}
            });
        } catch (Exception e) {
            // نادیده گرفتن خطا در پاکسازی سمت سرور
        }

        currentRequestId = null;
        currentPhone = null;
        currentMessages = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "بدون مجوزهای پیامک، اپ کار نخواهد کرد", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (DefaultSmsHelper.isDefaultSmsApp(this)) {
                tvStatus.setText("اپ پیش‌فرض پیامک شد. حالا می‌توانید تایید کنید.");
            } else {
                tvStatus.setText("اپ پیش‌فرض پیامک نشد. لطفا دوباره تلاش کنید.");
            }
        }
    }
}
