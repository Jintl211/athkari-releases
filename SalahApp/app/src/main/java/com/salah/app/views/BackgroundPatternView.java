package com.salah.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BackgroundPatternView extends View {
    private Paint paint;
    
    public BackgroundPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        paint = new Paint();
        paint.setColor(0x15FFD700);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        
        // نمط إسلامي بسيط
        for (int i = 0; i < w; i += 120) {
            for (int j = 0; j < h; j += 120) {
                canvas.drawCircle(i, j, 25, paint);
            }
        }
    }
}
