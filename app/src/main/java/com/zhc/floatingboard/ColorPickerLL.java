package com.zhc.floatingboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
class ColorPickerLL extends LinearLayout {
    private Button doneBtn;
    private int gradientWidth;
    private int dialogHeight;
    private int dialogWidth;
    private int pickedColor;
    private int alpha = 255;
    private final int initializeColor;

    ColorPickerLL(Context context, int dialogWidth, int dialogHeight, int initializeColor) {
        super(context);
        this.dialogHeight = dialogHeight;
        this.dialogWidth = dialogWidth;
        this.gradientWidth = dialogWidth / 6;
        this.initializeColor = initializeColor;
        init(context);
    }

    private void init(Context context) {
        pickedColor = initializeColor;
        doneBtn = new Button(context);
        doneBtn.setOnClickListener(v -> onDoneBtnPressed(pickedColor));
        doneBtn.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutParams dialog_ll_lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        dialog_ll_lp.setMargins(10, 10, 10, 10);
        alpha = initializeColor >>> 24;
        setLayoutParams(dialog_ll_lp);
        setOrientation(LinearLayout.HORIZONTAL);
        int[] colors = {0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
                0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
        LinearGradient lg = new LinearGradient(0F, 0F, ((float) (gradientWidth)), ((float) dialogHeight), colors, null, LinearGradient.TileMode.MIRROR);
        Paint paint = new Paint();
        paint.setShader(lg);
        Canvas canvas = new Canvas();
        canvas.drawRect(0F, 0F, ((float) (gradientWidth)), ((float) dialogHeight), paint);
        BrightLG brightLG = new BrightLG(context, dialogWidth, dialogHeight, gradientWidth) {
            @Override
            void onPickedAction(int color) {
//                super.onPickedAction(color);
                pickedColor = color;
                setBtnColor();
            }
        };
        ColorPickerLeftGradient pickerView = new ColorPickerLeftGradient(context, gradientWidth, dialogHeight) {
            @Override
            void onPickedAction(int color) {
                pickedColor = color;
                brightLG.setIntermediateColor(color);
                setBtnColor();
            }
        };
        brightLG.setIntermediateColor(pickedColor);
        setBtnColor();
        addView(pickerView);
        LinearLayout dialog_ll_right = new LinearLayout(context);
        dialog_ll_right.setLayoutParams(dialog_ll_lp);
        dialog_ll_right.setOrientation(LinearLayout.VERTICAL);
        dialog_ll_right.addView(brightLG);
        addView(dialog_ll_right);
        TextView alpha_tv = new TextView(context);
        alpha_tv.setText(R.string.alpha);
        alpha_tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog_ll_right.addView(alpha_tv);
        SeekBar sb = new SeekBar(context);
        sb.setMax(255);
        sb.setProgress(alpha);
        doneBtn.setText(R.string.done);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            doneBtn.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        } else doneBtn.setTextSize(25F);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alpha = progress;
                setBtnColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog_ll_right.addView(sb);
        dialog_ll_right.addView(doneBtn);
    }

    void onDoneBtnPressed(int pickedColor) {
    }

    private void setBtnColor() {
        RGB rgb = GradientUtil.parseRGB(pickedColor);
        pickedColor = Color.argb(alpha, rgb.r, rgb.g, rgb.b);
        doneBtn.setBackgroundColor(pickedColor);
        doneBtn.setBackgroundColor(pickedColor);
        onPickedAction(pickedColor);
    }

    void onPickedAction(int color) {
    }

    class BrightLG extends View {
        private int[] colors1;
        private LinearGradient lg;
        private Paint mPaint = new Paint();
        private int dialogHeight;
        int rightWidth;

        BrightLG(Context context, int dialogWidth, int dialogHeight, int gradientWidth) {
            super(context);
            this.dialogHeight = dialogHeight;
            rightWidth = dialogWidth - gradientWidth;
            colors1 = new int[]{0xFF000000, 0xFFFFFFFF};
            lg = new LinearGradient(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), colors1, null, Shader.TileMode.CLAMP);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
//                                    super.onDraw(canvas);
            mPaint.setShader(lg);
            canvas.drawRect(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), mPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//                                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(rightWidth, dialogHeight / 10);
        }

        void setIntermediateColor(int colorInt) {
            this.colors1 = new int[]{0xFF000000, colorInt, 0xFFFFFFFF};
            this.lg = new LinearGradient(0F, 0F, ((float) (rightWidth)), ((float) (dialogHeight / 10)), colors1, null, Shader.TileMode.CLAMP);
            invalidate();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                onPickedAction(new GradientUtil(colors1, 0F, rightWidth).getColor(event.getX()));
            } catch (Exception ignored) {
//                e.printStackTrace();
            }
            return true;
        }

        void onPickedAction(int color) {
        }
    }
}
