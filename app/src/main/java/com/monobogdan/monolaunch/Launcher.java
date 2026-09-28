package com.monobogdan.monolaunch;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.monobogdan.monolaunch.widgets.ClockWidget;
import com.monobogdan.monolaunch.widgets.PlayerWidget;
import com.monobogdan.monolaunch.widgets.StatusWidget;

public class Launcher extends Activity {

    public class LauncherView extends View
    {
        final String TAG = "LauncherView";


        private Paint defaultPaint;
        private Paint fontPaint;
        private BitmapDrawable iconMenu;

        private ClockWidget clockWidget;
        private StatusWidget statusWidget;
        private long timeSinceStart;

        private PlayerWidget playerView;

        public LauncherView(Context ctx)
        {
            super(ctx);

            clockWidget = new ClockWidget(this);
            playerView = new PlayerWidget(this);

            defaultPaint = new Paint();
            defaultPaint.setColor(Color.WHITE);

            fontPaint = new Paint();
            fontPaint.setColor(Color.WHITE);
            fontPaint.setAntiAlias(true);
            fontPaint.setTextSize(16);

            statusWidget = new StatusWidget(this);

            iconMenu = (BitmapDrawable) ctx.getResources().getDrawable(R.drawable.list);
        }

        public long getTimeSinceStart() {
            return timeSinceStart;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            event.startTracking();
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {

            Log.i(TAG, "onKeyUp: " + keyCode);
            if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                switchToMainMenu();

                return true;
            }

            // CT07: hotkeys go through AppIntents.
            Context ctx = getContext();

            if(keyCode == KeyEvent.KEYCODE_CALL)
            {
                AppIntents.dialer(ctx);

                return true;
            }

///dial. copied from https://github.com/Barracuda72/minilaunch
            if(keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
            {
                AppIntents.dial(ctx, String.valueOf(keyCode - KeyEvent.KEYCODE_0));
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_POUND)
            {
                AppIntents.dial(ctx, "#");
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_STAR)
            {
                AppIntents.dial(ctx, "*");
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_BACK)
            {
                AppIntents.contacts(ctx);
                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_MENU)
            {
                AppIntents.files(ctx);
                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
            {
                AppIntents.messages(ctx);
                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
            {
                try {
                    Object barMan = getContext().getSystemService("statusbar");
                    barMan.getClass().getMethod("expandNotificationsPanel").invoke(barMan);
                } catch (Exception e)
                {
                    Log.i(TAG, "onKeyUp: Failed to bring status");
                }

                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                AppIntents.calendar(ctx);
                return true;
            }

            if(keyCode == KeyEvent.KEYCODE_DPAD_UP)
            {
                // CT07: sound settings instead of the task list, which is
                // empty for a non-privileged app since Android 5.
                AppIntents.sound(ctx);

                return true;
            }

            // CT07: short press of the camera key.
            if(keyCode == KeyEvent.KEYCODE_CAMERA)
            {
                AppIntents.camera(ctx);
                return true;
            }

            return super.onKeyUp(keyCode, event);
        }


        private void drawBottomBar(Canvas canvas)
        {
            float metrics = fontPaint.getFontMetrics().bottom;
            float bottomLine = getHeight() - metrics - 3;
            float rightLine = getWidth() - fontPaint.measureText((getApplicationContext().getResources().getString(R.string.contacts))) - 5.0f;

            canvas.drawText(getApplicationContext().getResources().getString(R.string.files), 5.0f, bottomLine, fontPaint);
            canvas.drawText(getApplicationContext().getResources().getString(R.string.contacts), rightLine, bottomLine, fontPaint);

            float centerLine = getWidth() / 2 - (iconMenu.getMinimumWidth() / 2);

            canvas.drawBitmap(iconMenu.getBitmap(), centerLine, getHeight() - iconMenu.getMinimumHeight() - 3, defaultPaint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float baseline = 15.0f;

            baseline += clockWidget.draw(canvas, baseline);
            baseline += statusWidget.draw(canvas, baseline);
            baseline += playerView.draw(canvas, baseline);

            clientWidth = getWindow().getDecorView().getWidth();
            clientHeight = getWindow().getDecorView().getHeight(); // HACK!!!

            drawBottomBar(canvas);
        }
    }

    private Drawable cachedBackground;
    private LauncherView launcherView;
    private AppListView appList;
    private DialerView dialerView;
    private Tasks tasks;

    private int clientHeight;
    private int clientWidth;

    public Drawable getCachedBackground() {
        return cachedBackground;
    }

    public void switchToHome()
    {
        setContentView(launcherView);
        launcherView.requestFocus();

        launcherView.setAlpha(0);
        launcherView.animate().
                alpha(1.0f).
                setDuration(350);
    }

    private void switchToDialer()
    {
        setContentView(dialerView);
        dialerView.requestFocus();
        dialerView.setTranslationY(clientHeight);
        dialerView.animate().
                setDuration(250).
                translationY(0);
    }

    private void switchToMainMenu()
    {
        setContentView(appList);
        appList.requestFocus();
        appList.setTranslationY(clientHeight);
        appList.animate().
                setDuration(250).
                translationY(0);
    }

    private void switchToTasks()
    {
        tasks.updateTaskList();
        setContentView(tasks);
        tasks.requestFocus();
        tasks.setTranslationX(clientWidth);
        tasks.animate().setDuration(250).translationX(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialerView = new DialerView(getApplicationContext());
        tasks = new Tasks(this);

        // CT07: also focusable in touch mode (on after boot and after key mouse
        // gestures): MENU, BACK and CALL do not leave it and were lost.
        tasks.setFocusableInTouchMode(true);
        dialerView.setFocusableInTouchMode(true);

        launcherView = new LauncherView(getApplicationContext());
        appList = new AppListView(this);
        appList.setFocusableInTouchMode(true);
        launcherView.setFocusableInTouchMode(true);
        launcherView.requestFocus();

        cachedBackground = getWindow().getDecorView().getBackground();
        applyWallpaper(true);
        registerReceiver(wallpaperReceiver, new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));

        applyEndKeyDefault();
        switchToHome();
    }

    // CT07: the home activity lives as long as the phone is on, so the wallpaper
    // read in onCreate went stale when the user picked a new one. Reload it on
    // ACTION_WALLPAPER_CHANGED, and in onResume if the wallpaper id moved
    // (a broadcast missed while stopped). The old drawable is only dropped,
    // WallpaperManager owns the bitmap.
    private int wallpaperId = -1;

    private final BroadcastReceiver wallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            applyWallpaper(true);
        }
    };

    private void applyWallpaper(boolean force)
    {
        int id = -1;
        if(Build.VERSION.SDK_INT >= 24) {
            try {
                id = WallpaperManager.getInstance(this).getWallpaperId(WallpaperManager.FLAG_SYSTEM);
            } catch (Exception e) {
                Log.w("Launcher", "getWallpaperId: " + e);
            }
            if(!force && id == wallpaperId)
                return;
        } else if(!force) {
            return;
        }

        try {
            getWindow().setBackgroundDrawable(getWallpaper());
            wallpaperId = id;
        } catch (Exception e) {
            // CT07: keep the theme background.
            Log.w("Launcher", "getWallpaper: " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyWallpaper(false);
    }

    // CT07: the red key is ENDCALL. Default END_BUTTON_BEHAVIOR to home +
    // sleep, unless the setting already exists.
    private void applyEndKeyDefault()
    {
        // Settings.System.END_BUTTON_BEHAVIOR and its bit values are @hide.
        final String key = "end_button_behavior";
        final int homeThenSleep = 0x1 | 0x2; // END_BUTTON_BEHAVIOR_HOME | END_BUTTON_BEHAVIOR_SLEEP
        try {
            if(Settings.System.getString(getContentResolver(), key) == null)
                Settings.System.putInt(getContentResolver(), key, homeThenSleep);
        } catch (Exception e) {
            Log.w("Launcher", "END_BUTTON_BEHAVIOR default: " + e);
        }
    }

    // CT07: HOME while running (red key) returns to the clock screen.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switchToHome();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(wallpaperReceiver);
        appList.release();
        super.onDestroy();
    }
}
