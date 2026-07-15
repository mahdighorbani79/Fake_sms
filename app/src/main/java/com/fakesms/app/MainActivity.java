package com.fakesms.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etPhoneNumber, etMessage, etDate, etTime;
    private Button btnCreate, btnClear;
    private SMSHelper smsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etMessage = findViewById(R.id.etMessage);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        btnCreate = findViewById(R.id.btnCreate);
        btnClear = findViewById(R.id.btnClear);

        smsHelper = new SMSHelper(this);

        btnCreate.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (phone.isEmpty() || message.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "لطفا تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = DateConverter.jalaliToMillis(date, time);
            smsHelper.createFakeSMS(phone, message, timestamp);
        });

        btnClear.setOnClickListener(v -> {
            etPhoneNumber.setText("");
            etMessage.setText("");
            etDate.setText("");
            etTime.setText("");
        });
    }
}
