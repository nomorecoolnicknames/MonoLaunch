package com.monobogdan.monolaunch;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Tasks extends ListView {

    public String PACKAGE_NAME;

    class AppTask
    {
        public String name;

        public Bitmap icon;
        public int id;
        public int memUsage;
    }

    private Launcher launcher;
    private BaseAdapter adapterImpl;
    private ActivityManager activityManager;
    private ArrayList<AppTask> tasks;

    public Tasks(Launcher launcher)
    {
        super(launcher.getApplicationContext());

        this.launcher = launcher;
        setBackgroundColor(Color.BLACK);

        tasks = new ArrayList<>();
        activityManager = (ActivityManager) launcher.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        adapterImpl = new BaseAdapter() {
            @Override
            public int getCount() {
                return tasks.size();
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
                AppTask task = tasks.get(position);

                View view = launcher.getLayoutInflater().inflate(R.layout.task, parent, false);
                ((ImageView)view.findViewById(R.id.app_icon)).setImageBitmap(task.icon);
                ((TextView)view.findViewById(R.id.app_name)).setText(task.name);

                return view;
            }
        };

        setAdapter(adapterImpl);
    }

    public void updateTaskList()
    {
        List<ActivityManager.RunningTaskInfo> tInfo = activityManager.getRunningTasks(10);
        PackageManager pacMan = getContext().getPackageManager();

        tasks.clear();

        // CT07: topActivity can be null, and the icon is not always a BitmapDrawable.
        for (ActivityManager.RunningTaskInfo rt :
             tInfo) {
            try {
                if(rt.topActivity == null)
                    continue;

                AppTask appInfo = new AppTask();
                PackageInfo pacInfo = pacMan.getPackageInfo(rt.topActivity.getPackageName(), 0);

                if(pacInfo.packageName.equals(getContext().getPackageName()) || pacInfo.packageName.equals("com.sprd.simple.launcher"))
                    continue;

                appInfo.id = rt.id;
                appInfo.icon = AppListView.iconBitmap(pacInfo.applicationInfo.loadIcon(pacMan), 48);
                appInfo.name = pacMan.getApplicationLabel(pacInfo.applicationInfo).toString();

                tasks.add(appInfo);
            }
            catch (PackageManager.NameNotFoundException e)
            {

            }
        }

        adapterImpl.notifyDataSetChanged();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            launcher.switchToHome();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            // CT07: switch to the selected task.
            int pos = getSelectedItemPosition();
            if(pos >= 0 && pos < tasks.size()) {
                try {
                    activityManager.moveTaskToFront(tasks.get(pos).id, 0);
                } catch (Exception e) {
                    Log.w("Tasks", "moveTaskToFront: " + e);
                }
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }
}
