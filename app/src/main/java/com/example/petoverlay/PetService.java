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
import android.widget.Toast;

public class PetService extends Service {

    private WindowManager windowManager;
    private FrameLayout petView;
    private WebView webView;
    private WindowManager.LayoutParams params;

    private int lastX, lastY;
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

        final String[] clickWords = {
            "宝宝，终于想起我啦^^",
            "乖乖，别看了，我在这儿呢",
            "你身上有别人的气味呢",
            "老婆，我一直在看着你哦",
            "别乱跑，我一直看着你"
        };
        final String[] dragWords = {
            "别把我拖来拖去……我会吃醋的",
            "乖乖，你要带我去哪儿？",
            "拖我也没用，我永远粘着你",
            "轻点拖……我会疼的",
            "你走到哪我跟到哪，别想甩掉我"
        };
        final int[] clickIndex = {0};
        final int[] dragIndex = {0};

        webView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) event.getRawX() - lastX;
                    int deltaY = (int) event.getRawY() - lastY;
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) isDragging = true;
                    params.x += deltaX;
                    params.y += deltaY;
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    windowManager.updateViewLayout(petView, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (isDragging) {
                        Toast.makeText(PetService.this, dragWords[dragIndex[0] % dragWords.length], Toast.LENGTH_SHORT).show();
                        dragIndex[0]++;
                    } else {
                        Toast.makeText(PetService.this, clickWords[clickIndex[0] % clickWords.length], Toast.LENGTH_SHORT).show();
                        clickIndex[0]++;
                    }
                    return true;
            }
            return false;
        });

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params = new WindowManager.LayoutParams(
                dp(48), dp(48), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(20);
        params.y = dp(100);
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