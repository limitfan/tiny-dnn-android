package com.tinydnn.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PixelGridView extends View {
    private int numColumns, numRows;
    private int cellWidth, cellHeight;
    private Paint blackPaint = new Paint();
    private boolean[][] cellChecked;
    private OnDigitListener digitListener;

    public interface OnDigitListener{
        public void ondigit();
    }

    public PixelGridView(Context context) {
        this(context, null);
        numColumns = 32;
        numRows = 32;
    }

    public PixelGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        numColumns = 32;
        numRows = 32;
    }

    public void setDigitListener(OnDigitListener listener){
        digitListener = listener;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        calculateDimensions();
    }

    public int getNumRows() {
        return numRows;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }

        int minv = Math.min(getWidth(), getHeight());
        cellWidth = minv / numColumns;
        cellHeight = minv / numRows;

        cellChecked = new boolean[numColumns][numRows];

        invalidate();
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
        canvas.drawColor(Color.WHITE);

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int width = Math.min(getWidth(), getHeight());
        int height = Math.min(getWidth(), getHeight());

        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                if (cellChecked[i][j]) {

                    canvas.drawRect(i * cellWidth, j * cellHeight,
                                    (i + 1) * cellWidth, (j + 1) * cellHeight,
                                    blackPaint);
                }
            }
        }

        for (int i = 1; i < numColumns; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, blackPaint);
        }

        for (int i = 1; i < numRows; i++) {
            canvas.drawLine(0, i * cellHeight, width, i * cellHeight, blackPaint);
        }
    }

    private boolean checkSanity(int mcol, int mrow){
        if(mcol>=0&&mcol<numColumns&&mrow>=0&&mrow<numRows){
            return true;
        }
        return false;
    }

    public void clear(){
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                cellChecked[i][j]= false;
            }
        }
        invalidate();
    }

    public float[] getDigitsFloat(){
        float[] arr = new float[numColumns*numRows];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if(cellChecked[i][j]){
                    arr[i*numRows+j]=-1.0f;
                }
                else
                    arr[i*numRows+j]=1.0f;
            }
        }
        return arr;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            int column = (int)(event.getX() / cellWidth);
            int row = (int)(event.getY() / cellHeight);
            if(checkSanity(column, row)) {
                cellChecked[column][row] = true;

            }
            invalidate();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            if(digitListener!=null){
                digitListener.ondigit();
            }
        }

        return true;
    }
}