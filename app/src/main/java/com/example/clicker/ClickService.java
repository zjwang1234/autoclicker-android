package com.example.clicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.core.app.NotificationCompat;

public class ClickService extends Service {

    public static boolean isRunning = false;
    private WindowManager windowManager;
    private FrameLayout floatingView;
    private Handler handler;
    private Runnable clickRunnable;
    private int interval;
    private int count;
    private int currentCount = 0;
    private boolean isFloatingEnabled;
    private float targetX, targetY;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            interval = intent.getIntExtra("interval", 1000);
            count = intent.getIntExtra("count", 0);
            isFloatingEnabled = intent.getBooleanExtra("floating", true);
            currentCount = 0;
        }

        startForegroundService();

        if (isFloatingEnabled) {
            showFloatingView();
        } else {
            startClickingAtCenter();
        }

        isRunning = true;
        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "clicker_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Clicker Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("AutoClicker")
                .setContentText("点击器运行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void showFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = new FrameLayout(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 200;

        Button startBtn = new Button(this);
        startBtn.setText("点击这里开始");
        startBtn.setBackgroundColor(getResources().getColor(R.color.purple_500));
        startBtn.setTextColor(getResources().getColor(R.color.white));
        startBtn.setTextSize(18);
        startBtn.setPadding(20, 15, 20, 15);

        startBtn.setOnClickListener(v -> {
            targetX = v.getX() + v.getWidth() / 2;
            targetY = v.getY() + v.getHeight() / 2;
            startClicking();
            startBtn.setText("点击中...");
            startBtn.setEnabled(false);
        });

        floatingView.addView(startBtn);
        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void startClickingAtCenter() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        targetX = screenWidth / 2;
        targetY = screenHeight / 2;
        startClicking();
    }

    private void startClicking() {
        clickRunnable = new Runnable() {
            @Override
            public void run() {
                performClick();
                currentCount++;

                if (count == 0 || currentCount < count) {
                    handler.postDelayed(this, interval);
                } else {
                    stopSelf();
                }
            }
        };
        handler.post(clickRunnable);
    }

    private void performClick() {
        try {
            android.os.Process.sendSignal(android.os.Process.myPid(), 9);
            return;
        } catch (Exception ignored) {}

        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = serviceManagerClass.getMethod("getService", String.class);
            Object inputManager = getService.invoke(null, "input");

            Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
            java.lang.reflect.Method injectInputEvent = inputManagerClass.getMethod(
                    "injectInputEvent",
                    android.view.InputEvent.class,
                    int.class
            );

            long downTime = System.currentTimeMillis();
            android.view.MotionEvent downEvent = android.view.MotionEvent.obtain(
                    downTime, downTime,
                    android.view.MotionEvent.ACTION_DOWN,
                    targetX, targetY,
                    0
            );

            injectInputEvent.invoke(inputManager, downEvent, 0);

            android.view.MotionEvent upEvent = android.view.MotionEvent.obtain(
                    downTime + 10, downTime + 10,
                    android.view.MotionEvent.ACTION_UP,
                    targetX, targetY,
                    0
            );

            injectInputEvent.invoke(inputManager, upEvent, 0);

            downEvent.recycle();
            upEvent.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (handler != null && clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}