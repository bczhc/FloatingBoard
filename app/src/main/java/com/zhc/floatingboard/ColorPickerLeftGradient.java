package com.zhc.floatingboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerLeftGradient extends View {
    private Paint mPaint = null;
    private int mHeight;
    private int mWidth;
    private GradientUtil colorUtil;

    ColorPickerLeftGradient(Context context, int width, int height) {
        super(context);
        setMinimumWidth(width);
        setMinimumHeight(height);
        mWidth = width;
        mHeight = height;
        init();
    }

    public ColorPickerLeftGradient(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        int[] colors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
        LinearGradient lg = new LinearGradient(0F, 0F, mWidth, mHeight, colors, null, LinearGradient.TileMode.MIRROR);
        mPaint.setShader(lg);
        colorUtil = new GradientUtil(colors, 0F, mHeight);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        canvas.drawRect(0F, 0F, mWidth, mHeight, mPaint);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            onPickedAction(colorUtil.getColor(event.getY()));
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    void onPickedAction(int color) {
    }
}