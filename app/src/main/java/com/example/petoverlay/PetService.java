package com.example.petoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class PetService extends Service {

    private WindowManager windowManager;
    private FrameLayout petView;
    private WebView webView;
    private WindowManager.LayoutParams params;

    private int lastRawX, lastRawY;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(1, buildNotification());
        createPetOverlay();
    }

    private void createPetOverlay() {
        petView = new FrameLayout(this);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        webView.loadUrl("file:///android_asset/pet.html");
        petView.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        int winW = dp(100);
        int winH = dp(120);
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params = new WindowManager.LayoutParams(
                winW, winH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        params.x = (screenW - winW) / 2;
        params.y = screenH - winH - dp(20);

        webView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastRawX = (int) event.getRawX();
                    lastRawY = (int) event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) event.getRawX() - lastRawX;
                    int deltaY = (int) event.getRawY() - lastRawY;
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true;
                    }
                    if (isDragging) {
                        params.x += deltaX;
                        params.y += deltaY;
                        if (params.x < -winW + dp(40)) params.x = -winW + dp(40);
                        if (params.y < 0) params.y = 0;
                        if (params.x > getResources().getDisplayMetrics().widthPixels - dp(40)) params.x = getResources().getDisplayMetrics().widthPixels - dp(40);
                        if (params.y > getResources().getDisplayMetrics().heightPixels - dp(40)) params.y = getResources().getDisplayMetrics().heightPixels - dp(40);
                        windowManager.updateViewLayout(petView, params);
                    }
                    lastRawX = (int) event.getRawX();
                    lastRawY = (int) event.getRawY();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        float density = getResources().getDisplayMetrics().density;
                        webView.evaluateJavascript("handleTap(" + (event.getX() / density) + "," + (event.getY() / density) + ");", null);
                    }
                    return true;
            }
            return false;
        });

        if (Settings.canDrawOverlays(this)) {
            windowManager.addView(petView, params);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "pet_channel", "桌宠", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, "pet_channel")
                .setContentTitle("桌宠运行中")
                .setContentText("我在这里陪着你")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (petView != null && windowManager != null) {
            try {
                windowManager.removeView(petView);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
