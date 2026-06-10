package com.aitranslator.app.ui.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.aitranslator.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Tiny custom view that renders a 7-day XP bar chart.
 *
 * Why not a chart library?
 *   - MPAndroidChart is ~2 MB after R8 — too heavy for one chart.
 *   - We only need bars, axis labels, and a couple of values. Easy with
 *     Canvas + Paint.
 *
 * Public API:
 *   - setData(int[] xpPerDay, long[] dayTimestamps) — 7 entries each,
 *     index 0 = oldest day, index 6 = today.
 *
 * Theming: pulls colours from R.color.* so a palette change in colors.xml
 * propagates automatically.
 */
public class XpBarChartView extends View {

    /** Number of bars (always a week). */
    private static final int BARS = 7;

    private final Paint barPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barBgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private int[] xp = new int[BARS];
    private long[] dayTs = new long[BARS];

    public XpBarChartView(Context ctx) { this(ctx, null); }
    public XpBarChartView(Context ctx, @Nullable AttributeSet attrs) { this(ctx, attrs, 0); }
    public XpBarChartView(Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);

        // Filled bar colour: app primary
        barPaint.setColor(getResources().getColor(R.color.primary));
        barPaint.setStyle(Paint.Style.FILL);

        // Empty-track colour: a soft divider tint
        barBgPaint.setColor(getResources().getColor(R.color.divider));
        barBgPaint.setStyle(Paint.Style.FILL);

        axisPaint.setColor(getResources().getColor(R.color.divider));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(getResources().getColor(R.color.text_secondary));
        labelPaint.setTextSize(sp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(getResources().getColor(R.color.text_primary));
        valuePaint.setTextSize(sp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** Set the chart data. xpPerDay[6] is today, xpPerDay[0] is six days ago. */
    public void setData(int[] xpPerDay, long[] dayTimestamps) {
        if (xpPerDay == null || xpPerDay.length != BARS) return;
        if (dayTimestamps == null || dayTimestamps.length != BARS) return;
        this.xp = xpPerDay.clone();
        this.dayTs = dayTimestamps.clone();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float padH = dp(8);
        float labelArea = sp(18);                  // space at the bottom for day labels
        float valueArea = sp(16);                  // space at the top for value labels
        float chartTop = valueArea;
        float chartBottom = h - labelArea;
        float chartH = chartBottom - chartTop;

        // Determine max value for scaling. Treat zero-week as a flat empty
        // chart rather than dividing by zero.
        int maxXp = 1;
        for (int v : xp) if (v > maxXp) maxXp = v;

        float slot = (w - padH * 2f) / BARS;
        float barW = slot * 0.55f;                 // bar width inside its slot

        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < BARS; i++) {
            float cx = padH + slot * i + slot / 2f;
            float left = cx - barW / 2f;
            float right = cx + barW / 2f;

            // Background track (full-height, light)
            RectF track = new RectF(left, chartTop, right, chartBottom);
            canvas.drawRoundRect(track, dp(6), dp(6), barBgPaint);

            // Filled bar height
            float fillH = chartH * (xp[i] / (float) maxXp);
            if (xp[i] > 0) {
                RectF bar = new RectF(left, chartBottom - fillH, right, chartBottom);
                canvas.drawRoundRect(bar, dp(6), dp(6), barPaint);

                // Value above the bar
                canvas.drawText(
                        String.valueOf(xp[i]),
                        cx,
                        chartBottom - fillH - sp(3),
                        valuePaint);
            }

            // Day label below
            String label = dayFmt.format(dayTs[i]);
            // Highlight "today" by capitalising it differently? Just use
            // bold colour for today's column.
            TextPaint p = (i == BARS - 1) ? valuePaint : labelPaint;
            canvas.drawText(label, cx, h - dp(2), p);
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    /** Build today-anchored timestamps for the last 7 days (index 6 = today). */
    public static long[] last7DayTimestamps() {
        long[] out = new long[BARS];
        Calendar c = Calendar.getInstance();
        // Strip time-of-day so equal-day comparisons are reliable.
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long todayMidnight = c.getTimeInMillis();
        long oneDay = 24L * 60L * 60L * 1000L;
        for (int i = 0; i < BARS; i++) {
            out[i] = todayMidnight - oneDay * (BARS - 1 - i);
        }
        return out;
    }
}
