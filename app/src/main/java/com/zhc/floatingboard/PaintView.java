package com.zhc.floatingboard;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.ColorInt;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

@SuppressWarnings("ALL")
public class PaintView extends View {
    private Paint mPaint;
    private Path mPath;
    private Paint eraserPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private float mLastX, mLastY;//上次的坐标
    private Paint mBitmapPaint;
    //使用LinkedList 模拟栈，来保存 Path
    private LinkedList<PathBean> undoList;
    private LinkedList<PathBean> redoList;
    boolean isEraserMode;


    /*public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }*/

    public PaintView(Context context, int width, int height) {
        super(context);
        init(width, height);
    }

    /***
     * 初始化
     */
    private void init(int width, int height) {
        //关闭硬件加速
        //否则橡皮擦模式下，设置的 PorterDuff.Mode.CLEAR ，实时绘制的轨迹是黑色
//        setBackgroundColor(Color.WHITE);//设置白色背景
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        //保存签名的画布
        post(new Runnable() {//拿到控件的宽和高
            @Override
            public void run() {
                //获取PaintView的宽和高
                //由于橡皮擦使用的是 Color.TRANSPARENT ,不能使用RGB-565
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                mCanvas = new Canvas(mBitmap);
                //抗锯齿
                mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                //背景色
//                mCanvas.drawColor(Color.WHITE);
            }
        });

        undoList = new LinkedList<>();
        redoList = new LinkedList<>();
    }

    /**
     * 绘制
     */
    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);//将mBitmap绘制在canvas上,最终的显示
            if (null != mPath) {//显示实时正在绘制的path轨迹
                if (isEraserMode) canvas.drawPath(mPath, eraserPaint);
                else canvas.drawPath(mPath, mPaint);
            }
        }
    }

    void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
        if (eraserPaint != null) eraserPaint.setStrokeWidth(width);
    }

    float getStrokeWidth() {
        return this.mPaint.getStrokeWidth();
    }

    int getColor() {
        return this.mPaint.getColor();
    }

    /**
     * 撤销操作
     */
    public void undo() {
        if (!undoList.isEmpty()) {
            clearPaint();//清除之前绘制内容
            PathBean lastPb = undoList.removeLast();//将最后一个移除
            redoList.add(lastPb);//加入 恢复操作
            //遍历，将Path重新绘制到 mCanvas
            for (PathBean pb : undoList) {
                mCanvas.drawPath(pb.path, pb.paint);
            }
            invalidate();
        }
    }

    /**
     * 恢复操作
     */
    public void redo() {
        if (!redoList.isEmpty()) {
            PathBean pathBean = redoList.removeLast();
            mCanvas.drawPath(pathBean.path, pathBean.paint);
            invalidate();
            undoList.add(pathBean);
        }
    }


    /**
     * 设置画笔颜色
     */
    public void setPaintColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * 清空，包括撤销和恢复操作列表
     */
    public void clearAll() {
        clearPaint();
        mLastY = 0f;
        //清空 撤销 ，恢复 操作列表
        redoList.clear();
        undoList.clear();
    }

    /**
     * 设置橡皮擦模式
     */
    public void setEraserMode(boolean isEraserMode) {
        this.isEraserMode = isEraserMode;
        if (eraserPaint == null) {
            eraserPaint = new Paint(mPaint);
            eraserPaint.setColor(Color.TRANSPARENT);
            eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
    }

    /**
     * 保存到指定的文件夹中
     */
    public boolean saveImg(String filePath, String imgName) {
        boolean isCanSave = mBitmap != null && mLastY != 0f && !undoList.isEmpty();
        if (isCanSave) {//空白板时，就不保存
            //保存图片
            File file = new File(filePath + File.separator + imgName);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                if (mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)) {
                    fileOutputStream.flush();
                    return true;
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                closeStream(fileOutputStream);
            }
        }
        return false;
    }

    /**
     * 是否可以撤销
     */
    public boolean isCanUndo() {
        return undoList.isEmpty();
    }

    /**
     * 是否可以恢复
     */
    public boolean isCanRedo() {
        return redoList.isEmpty();
    }

    /**
     * 清除绘制内容
     * 直接绘制白色背景
     */
    private void clearPaint() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        invalidate();
    }


    /**
     * 触摸事件 触摸绘制
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEraserMode) {
            eraserTouchEvent(event);
        } else {
            commonTouchEvent(event);
        }
        invalidate();
        return true;
    }

    /**
     * 橡皮擦事件
     */
    private void eraserTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mLastX);
                float dy = Math.abs(y - mLastY);
                if (dx >= 0 || dy >= 0) {//绘制的最小距离 0px
                    //利用二阶贝塞尔曲线，使绘制路径更加圆滑
                    try {
                        mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
                    } catch (NullPointerException ignored) {
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                mCanvas.drawPath(mPath, eraserPaint);//将路径绘制在mBitmap上
                Path path = new Path(mPath);//复制出一份mPath
                Paint paint = new Paint(eraserPaint);
                PathBean pb = new PathBean(path, paint);
                undoList.add(pb);//将路径对象存入集合
                mPath.reset();
                mPath = null;
                break;
        }
    }

    /**
     * 普通画笔事件
     */
    private void commonTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mLastX);
                float dy = Math.abs(y - mLastY);
                if (dx >= 0 || dy >= 0) {//绘制的最小距离 0px
                    //利用二阶贝塞尔曲线，使绘制路径更加圆滑
                    try {
                        mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
                    } catch (NullPointerException ignored) {
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                mCanvas.drawPath(mPath, mPaint);//将路径绘制在mBitmap上
                Path path = new Path(mPath);//复制出一份mPath
                Paint paint = new Paint(mPaint);
                PathBean pb = new PathBean(path, paint);
                undoList.add(pb);//将路径对象存入集合
                mPath.reset();
                mPath = null;
                break;
        }
    }

    /**
     * 关闭流
     *
     * @param closeable
     */
    private void closeStream(FileOutputStream closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 测量
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (wSpecMode == MeasureSpec.EXACTLY && hSpecMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else if (wSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(200, hSpecSize);
        } else if (hSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(wSpecSize, 200);
        }
    }

    /**
     * 路径对象
     */
    class PathBean {
        Path path;
        Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

}