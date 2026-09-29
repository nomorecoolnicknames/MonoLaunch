package com.monobogdan.monolaunch.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockWidget {

    private View parentView;
    private Context context;
    private Paint bgPaint;
    private Paint paint;
    private Paint datePaint;

    private SimpleDateFormat timeFormat;
    private boolean timeFormat24;
    private Locale timeFormatLocale;

    public ClockWidget(View view)
    {
        parentView = view;
        Context ctx = view.getContext();
        context = ctx;

        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(99, 128, 128, 128));

        paint = new Paint();
        paint.setTextSize(35);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.MONOSPACE);

        datePaint = new Paint();
        datePaint.setTextSize(12);
        datePaint.setColor(Color.WHITE);
        datePaint.setAntiAlias(true);
        datePaint.setTypeface(Typeface.MONOSPACE);

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                view.invalidate();
                handler.postDelayed(this, 1000);
            }
        });
    }

    // DateFormat.getTimeInstance() follows a process-wide 12/24-hour flag. Android 7 sets it
    // to 12 hours in every running app when the time is set without the 24-hour extra, e.g.
    // by the network, so the clock switched to "12:34:45 AM" with the 24-hour locale. Ask
    // the system setting (or the locale when it is unset) instead.
    private SimpleDateFormat getTimeFormat()
    {
        boolean is24 = android.text.format.DateFormat.is24HourFormat(context);
        Locale locale = Locale.getDefault();
        if (timeFormat == null || is24 != timeFormat24 || !locale.equals(timeFormatLocale)) {
            timeFormat = new SimpleDateFormat(is24 ? "H:mm:ss" : "h:mm:ss a", locale);
            timeFormat24 = is24;
            timeFormatLocale = locale;
        }
        return timeFormat;
    }

    public float draw(Canvas canvas, float y)
    {
        float yRet = 0;
        Date date = new Date();
        String strDate = DateFormat.getDateInstance().format(date);
        String strTime = getTimeFormat().format(date);

        canvas.drawText(strTime, parentView.getWidth() / 2 - (paint.measureText(strTime) / 2), y + -paint.getFontMetrics().top, paint);
        yRet += -paint.getFontMetrics().top;
        canvas.drawText(strDate, parentView.getWidth() / 2 - (datePaint.measureText(strDate) / 2), y+ -datePaint.getFontMetrics().top + -paint.getFontMetrics().top, datePaint);
        yRet += -datePaint.getFontMetrics().top;

        canvas.drawRect(0, y, parentView.getWidth(), yRet + 25, bgPaint);

        return yRet + 25;
    }
}
