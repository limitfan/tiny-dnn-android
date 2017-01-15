package com.tinydnn.android.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class FingerPaintView extends View {
  private static final float MINP = 0.25f;
  private static final float MAXP = 0.75f;
  private Bitmap  mBitmap;
  private Canvas  mCanvas;
  private Path    mPath;
  private Paint   mBitmapPaint;
  private Paint       mPaint;

  private FingerPaintView.OnDigitListener digitListener;

  public interface OnDigitListener{
    public void ondigit();
  }

  public void setDigitListener(FingerPaintView.OnDigitListener listener){
    digitListener = listener;
  }

  public FingerPaintView(Context c) {
    super(c);
    mPath = new Path();
    mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setColor(0xFFFF0000);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);
    mPaint.setStrokeWidth(10*c.getResources().getDisplayMetrics().density);
  }

  public FingerPaintView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mPath = new Path();
    mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setColor(0xFF000000);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);
    mPaint.setStrokeWidth(10*context.getResources().getDisplayMetrics().density);
  }

  public void clear(){
    mCanvas.drawColor(0xFFFFFFFF);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    mCanvas = new Canvas(mBitmap);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
    int heigthWithoutPadding = height - getPaddingTop() - getPaddingBottom();

    int maxWidth = (int) (heigthWithoutPadding * 1);
    int maxHeight = (int) (widthWithoutPadding / 1);

    if (widthWithoutPadding > maxWidth) {
      width = maxWidth + getPaddingLeft() + getPaddingRight();
    } else {
      height = maxHeight + getPaddingTop() + getPaddingBottom();
    }

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(0xFFFFFFFF);
    canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    canvas.drawPath(mPath, mPaint);
  }
  private float mX, mY;
  private static final float TOUCH_TOLERANCE = 4;
  private void touch_start(float x, float y) {
    mPath.reset();
    mPath.moveTo(x, y);
    mX = x;
    mY = y;
  }
  private void touch_move(float x, float y) {
    float dx = Math.abs(x - mX);
    float dy = Math.abs(y - mY);
    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
      mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
      mX = x;
      mY = y;
    }
  }
  private void touch_up() {
    mPath.lineTo(mX, mY);
    // commit the path to our offscreen
    mCanvas.drawPath(mPath, mPaint);
    // kill this so we don't double draw
    mPath.reset();
  }
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        touch_start(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_MOVE:
        touch_move(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_UP:
        touch_up();
        invalidate();
        if(digitListener!=null){
          digitListener.ondigit();
        }
        break;
    }
    return true;
  }
}
