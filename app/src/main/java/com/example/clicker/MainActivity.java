package com.example.clicker;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etInterval, etCount;
    private Switch swFloating;
    private Button btnStart, btnStop;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInterval = findViewById(R.id.etInterval);
        etCount = findViewById(R.id.etCount);
        swFloating = findViewById(R.id.swFloating);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        tvStatus = findViewById(R.id.tvStatus);

        btnStart.setOnClickListener(v -> startClicking());
        btnStop.setOnClickListener(v -> stopClicking());
    }

    private void startClicking() {
        if (!checkPermission()) {
            Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
            return;
        }

        String intervalStr = etInterval.getText().toString();
        String countStr = etCount.getText().toString();

        if (intervalStr.isEmpty() || countStr.isEmpty()) {
            Toast.makeText(this, "请填写完整参数", Toast.LENGTH_SHORT).show();
            return;
        }

        int interval = Integer.parseInt(intervalStr);
        int count = Integer.parseInt(countStr);

        Intent intent = new Intent(this, ClickService.class);
        intent.putExtra("interval", interval);
        intent.putExtra("count", count);
        intent.putExtra("floating", swFloating.isChecked());
        startService(intent);

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        tvStatus.setText("状态: 运行中");
    }

    private void stopClicking() {
        Intent intent = new Intent(this, ClickService.class);
        stopService(intent);

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        tvStatus.setText("状态: 已停止");
    }

    private boolean checkPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ClickService.isRunning) {
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            tvStatus.setText("状态: 运行中");
        }
    }
}