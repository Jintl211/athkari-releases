package com.salah.app.views;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

public class RealisticClockView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar calendar = Calendar.getInstance();
    private final RectF arcRect = new RectF();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override public void run() {
            invalidate();
            handler.postDelayed(this, 1000); // تحديث كل ثانية بدقة
        }
    };

    // نسبة الوقت المنقضي من الفترة الحالية بين صلاتين (0..1) لرسم القوس
    private float progress = 0f;

    public RealisticClockView(Context context) { super(context); init(); }
    public RealisticClockView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public RealisticClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        shadowPaint.setColor(Color.parseColor("#40000000"));
        shadowPaint.setMaskFilter(new BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL));
    }

    /** استدعها من الأكتفتي عند كل تحديث للعد التنازلي: 0f = بداية الفترة، 1f = اقترب وقت الصلاة */
    public void setProgress(float value) {
        if (value < 0f) value = 0f;
        if (value > 1f) value = 1f;
        progress = value;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(updateRunnable);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(updateRunnable);
    }

    private float centerX() { return getWidth() / 2f; }
    private float centerY() { return getHeight() / 2f; }
    private float radius()  { return Math.min(getWidth(), getHeight()) / 2f * 0.78f; }
    private float arcRadius() { return Math.min(getWidth(), getHeight()) / 2f * 0.95f; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        calendar.setTimeInMillis(System.currentTimeMillis());
        int hours = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        drawProgressArc(canvas);
        drawClockFace(canvas);
        drawTicks(canvas);
        drawNumbers(canvas);
        drawHands(canvas, hours, minutes, seconds);
        drawCenterDot(canvas);
    }

    private void drawProgressArc(Canvas canvas) {
        float cx = centerX(), cy = centerY(), r = arcRadius();
        arcRect.set(cx - r, cy - r, cx + r, cy + r);

        paint.setShader(null);
        paint.setColor(Color.parseColor("#33ffffff"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(arcRect, -90f, 360f, false, paint);

        paint.setColor(Color.parseColor("#ffd700"));
        canvas.drawArc(arcRect, -90f, 360f * progress, false, paint);
    }

    private void drawClockFace(Canvas canvas) {
        float cx = centerX(), cy = centerY(), r = radius();

        canvas.drawCircle(cx + 6f, cy + 6f, r, shadowPaint);

        RadialGradient gradient = new RadialGradient(
                cx - r / 3, cy - r / 3, r,
                new int[]{
                        Color.parseColor("#2a3a4a"),
                        Color.parseColor("#1a2a3a"),
                        Color.parseColor("#0D1B2A")
                },
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setShader(null);
        paint.setColor(Color.parseColor("#3a4a5a"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, r - 4f, paint);
    }

    private void drawTicks(Canvas canvas) {
        float cx = centerX(), cy = centerY(), r = radius();
        for (int i = 0; i < 60; i++) {
            double angle = Math.PI * i / 30;
            boolean isHour = i % 5 == 0;
            float startR = isHour ? r - 22f : r - 13f;
            float endR = r - 6f;

            float startX = (float) (cx + Math.cos(angle) * startR);
            float startY = (float) (cy + Math.sin(angle) * startR);
            float endX = (float) (cx + Math.cos(angle) * endR);
            float endY = (float) (cy + Math.sin(angle) * endR);

            paint.setColor(isHour ? Color.WHITE : Color.parseColor("#888888"));
            paint.setStrokeWidth(isHour ? 3f : 1.5f);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }

    private void drawNumbers(Canvas canvas) {
        float cx = centerX(), cy = centerY(), r = radius();
        paint.setColor(Color.WHITE);
        paint.setTextSize(r * 0.2f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);

        String[] numbers = {"12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * (i - 3) / 6;
            float numRadius = r * 0.72f;
            float x = (float) (cx + Math.cos(angle) * numRadius);
            float y = (float) (cy + Math.sin(angle) * numRadius + paint.getTextSize() / 3);
            canvas.drawText(numbers[i], x, y, paint);
        }
    }

    private void drawHands(Canvas canvas, int hours, int minutes, int seconds) {
        double hourAngle = Math.PI * ((hours + minutes / 60.0) * 30 - 90) / 180;
        double minuteAngle = Math.PI * (minutes * 6 - 90) / 180;
        double secondAngle = Math.PI * (seconds * 6 - 90) / 180;

        drawHand(canvas, hourAngle, radius() * 0.48f, 7f, Color.WHITE);
        drawHand(canvas, minuteAngle, radius() * 0.70f, 5f, Color.WHITE);
        drawSecondHand(canvas, secondAngle, radius() * 0.80f);
    }

    private void drawHand(Canvas canvas, double angle, float length, float width, int color) {
        float cx = centerX(), cy = centerY();
        float endX = (float) (cx + Math.cos(angle) * length);
        float endY = (float) (cy + Math.sin(angle) * length);

        paint.setColor(Color.parseColor("#30000000"));
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx + 2f, cy + 2f, endX + 2f, endY + 2f, paint);

        paint.setColor(color);
        canvas.drawLine(cx, cy, endX, endY, paint);
    }

    private void drawSecondHand(Canvas canvas, double angle, float length) {
        float cx = centerX(), cy = centerY();
        float endX = (float) (cx + Math.cos(angle) * length);
        float endY = (float) (cy + Math.sin(angle) * length);

        paint.setColor(Color.parseColor("#ffd700"));
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy, endX, endY, paint);
        canvas.drawCircle(endX, endY, 3.5f, paint);
    }

    private void drawCenterDot(Canvas canvas) {
        float cx = centerX(), cy = centerY();
        paint.setColor(Color.parseColor("#30000000"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx + 2f, cy + 2f, 8f, paint);

        paint.setColor(Color.parseColor("#ffd700"));
        canvas.drawCircle(cx, cy, 7f, paint);

        paint.setColor(Color.parseColor("#1a2a3a"));
        canvas.drawCircle(cx, cy, 3f, paint);
    }
}
