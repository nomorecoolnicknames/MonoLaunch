package com.monobogdan.monolaunch;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

/**
 * CT07: hotkey targets. The first candidate intent that resolves is started,
 * otherwise a toast is shown.
 */
final class AppIntents {
    private static final String TAG = "AppIntents";

    private AppIntents() {}

    /** Start the first candidate that resolves; false (and a toast) if none does. */
    static boolean launch(Context ctx, int whatStringId, Intent... candidates) {
        return launch(ctx, ctx.getString(whatStringId), candidates);
    }

    static boolean launch(Context ctx, CharSequence what, Intent... candidates) {
        PackageManager pm = ctx.getPackageManager();
        for (Intent intent : candidates) {
            if (intent == null || intent.resolveActivity(pm) == null)
                continue;
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                // resolveActivity said yes but the activity refused: try the next one.
                Log.w(TAG, "launch: " + intent + " failed: " + e);
            }
        }
        Toast.makeText(ctx, ctx.getString(R.string.no_app, what), Toast.LENGTH_SHORT).show();
        return false;
    }

    /** Launch intent of a package, or null. */
    static Intent launcherFor(Context ctx, String packageName) {
        if (packageName == null)
            return null;
        return ctx.getPackageManager().getLaunchIntentForPackage(packageName);
    }

    /** Phone application (call log / keypad). */
    static boolean dialer(Context ctx) {
        return launch(ctx, R.string.app_dialer,
                new Intent(Intent.ACTION_DIAL),
                launcherFor(ctx, "com.android.dialer"));
    }

    /** Dialer pre-filled with the key; "#" needs Uri.fromParts. */
    static boolean dial(Context ctx, String digits) {
        return launch(ctx, R.string.app_dialer,
                new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", digits, null)),
                new Intent(Intent.ACTION_DIAL));
    }

    static boolean contacts(Context ctx) {
        return launch(ctx, R.string.app_contacts,
                new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI),
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS),
                launcherFor(ctx, "com.android.contacts"));
    }

    /** The default SMS app, then any messaging app. */
    static boolean messages(Context ctx) {
        String defaultSms = null;
        try {
            defaultSms = Telephony.Sms.getDefaultSmsPackage(ctx);
        } catch (Exception e) {
            Log.w(TAG, "getDefaultSmsPackage: " + e);
        }
        return launch(ctx, R.string.app_messages,
                launcherFor(ctx, defaultSms),
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
                launcherFor(ctx, "com.android.messaging"));
    }

    /** DocumentsUI ("Files"), or its BROWSE action. */
    static boolean files(Context ctx) {
        Intent browse = new Intent("android.provider.action.BROWSE");
        browse.setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                "vnd.android.document/root");
        return launch(ctx, R.string.files,
                launcherFor(ctx, "com.android.documentsui"),
                browse,
                new Intent("android.intent.action.VIEW_DOWNLOADS"));
    }

    static boolean calendar(Context ctx) {
        return launch(ctx, R.string.app_calendar,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
                new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI),
                launcherFor(ctx, "com.android.calendar"));
    }

    /** Sound settings: the phone has no volume keys. */
    static boolean sound(Context ctx) {
        return launch(ctx, R.string.app_sound,
                new Intent(Settings.ACTION_SOUND_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS));
    }

    static boolean camera(Context ctx) {
        return launch(ctx, R.string.app_camera,
                new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                launcherFor(ctx, "com.android.camera2"));
    }
}
