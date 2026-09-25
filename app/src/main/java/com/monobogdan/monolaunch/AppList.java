package com.monobogdan.monolaunch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class AppListView extends GridView
{
    // CT07: raw pixels, three 80 px columns on a 240 px wide screen.
    private static final int COLUMNS = 3;
    private static final int ICON_PX = 36;
    private static final int LABEL_PX = 11;

    class AppInfo
    {
        public Drawable icon;
        public String name;
        public Intent intent;
    }

    class PackageManagerListener extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            fetchAppList();
            rebuildUI();

            System.gc();
        }
    }

    private Launcher parent;
    private PackageManagerListener packageListener;
    private ArrayList<AppInfo> installedApps;
    private int selectedItem;
    List<View> widgetList = new ArrayList<>();

    private void fetchAppList()
    {
        installedApps.clear();

        // CT07: launchable activities in one query, without the launcher itself.
        Intent filter = new Intent();
        filter.setAction(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(filter, 0);
        String self = getContext().getPackageName();

        for (ResolveInfo info:
                apps) {
            if(info.activityInfo == null || self.equals(info.activityInfo.packageName))
                continue;

            AppInfo app = new AppInfo();
            app.name = String.valueOf(info.loadLabel(pm));
            app.icon = info.loadIcon(pm);
            app.intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(info.activityInfo.packageName, info.activityInfo.name)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            Log.i("Test", "fetchAppList: " + String.format("%s %s", app.name, app.intent));
            installedApps.add(app);
        }

        // Sorted by label in the current locale.
        final Collator collator = Collator.getInstance();
        Collections.sort(installedApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return collator.compare(a.name, b.name);
            }
        });
    }

    public AppListView(Launcher launcher)
    {
        super(launcher.getApplicationContext());

        // CT07: package broadcasts need the "package" data scheme.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        packageListener = new PackageManagerListener();
        launcher.getApplicationContext().registerReceiver(packageListener, filter);

        parent = launcher;

        installedApps = new ArrayList<>();
        fetchAppList();
        rebuildUI();
    }

    // CT07: unregister with the activity, which is recreated on a locale change.
    void release()
    {
        try {
            getContext().unregisterReceiver(packageListener);
        } catch (IllegalArgumentException e) {
            // already gone
        }
    }

    // CT07: icons are not always BitmapDrawables (vector and layer icons).
    static Bitmap iconBitmap(Drawable icon, int size)
    {
        if(icon instanceof BitmapDrawable && ((BitmapDrawable) icon).getBitmap() != null)
            return Bitmap.createScaledBitmap(((BitmapDrawable) icon).getBitmap(), size, size, true);

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if(icon != null) {
            Canvas canvas = new Canvas(bmp);
            icon.setBounds(0, 0, size, size);
            icon.draw(canvas);
        }
        return bmp;
    }

    // CT07: grid cell, icon over a one-line label.
    private View makeCell(AppInfo app)
    {
        Context ctx = getContext();

        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(2, 4, 2, 4);
        cell.setFocusable(false);

        ImageView icon = new ImageView(ctx);
        icon.setImageBitmap(iconBitmap(app.icon, ICON_PX));
        cell.addView(icon, new LinearLayout.LayoutParams(ICON_PX, ICON_PX));

        TextView label = new TextView(ctx);
        label.setText(app.name);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, LABEL_PX);
        label.setTextColor(Color.WHITE);
        label.setShadowLayer(1, 1, 1, Color.BLACK);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 2;
        cell.addView(label, lp);

        return cell;
    }

    private void rebuildUI()
    {
        setNumColumns(COLUMNS);
        setStretchMode(STRETCH_COLUMN_WIDTH);
        setClickable(true);
        // CT07: visible selector, drawn under the cell so the label stays readable.
        setSelector(R.drawable.grid_selector);
        setDrawSelectorOnTop(false);
        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = position;
                if(view != null) {
                    view.animate().scaleX(1.1f).scaleY(1.2f).setDuration(100);

                    for (View v:
                         widgetList) {
                        if(v != view)
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200);
                    }
                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        widgetList.clear();
        for(int i = 0; i < installedApps.size(); i++)
            widgetList.add(makeCell(installedApps.get(i)));

        setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return installedApps.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return widgetList.get(position);
            }
        });

        if(selectedItem >= installedApps.size())
            selectedItem = 0;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        Log.i("TAG", "onItemSelected: " + gainFocus);
        // CT07: restore the last selected cell.
        if(gainFocus && !installedApps.isEmpty())
            setSelection(selectedItem);
    }

    private int clamp(int a, int min, int max)
    {
        return a < min ? min : (a > max ? max : a);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            int pos = getSelectedItemPosition();
            if(pos >= 0 && pos < installedApps.size()) {
                AppInfo app = installedApps.get(pos);
                AppIntents.launch(getContext(), app.name, app.intent);
            }
            return true;
        }

        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            parent.switchToHome();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }
}
