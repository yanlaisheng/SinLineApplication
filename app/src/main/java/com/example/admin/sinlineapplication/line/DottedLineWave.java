package com.example.admin.sinlineapplication.line;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.view.View;

import com.example.admin.sinlineapplication.R;

import java.nio.channels.ScatteringByteChannel;

public class DottedLineWave extends View {

    private Paint bgPaint, dottedLinePaint, axisLinePaint, calibrationLinePaint;

    private Path mxPath, myPath;
    private int height = 0;
    private int width = 0;

    private Canvas mBufferCanvas;
    private Bitmap mBufferBitmap;

    public DottedLineWave(Context context) {
        super(context);
        initView();
    }

    //如果不写下面的构造函数，则会报错：custom view SineWave is not using the 2- or 3-argument View constructors

    public DottedLineWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        //背景画笔
        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);

        //轴线画笔
        axisLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisLinePaint.setStrokeWidth(2);
        axisLinePaint.setStyle(Paint.Style.STROKE);
        axisLinePaint.setColor(getResources().getColor(R.color.white));

        //刻度线画笔 calibrationLinePaint
        calibrationLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        calibrationLinePaint.setStrokeWidth(1);
        calibrationLinePaint.setStyle(Paint.Style.STROKE);
        calibrationLinePaint.setAlpha(50);
        calibrationLinePaint.setColor(getResources().getColor(R.color.white));

        //虚线画笔
        dottedLinePaint = new Paint();
        dottedLinePaint.setColor(getResources().getColor(R.color.white));
        dottedLinePaint.setStrokeWidth(0.5f);
        dottedLinePaint.setStyle(Paint.Style.STROKE);
        dottedLinePaint.setAntiAlias(true);
        dottedLinePaint.setAlpha(50);
        PathEffect effects = new DashPathEffect(new float[]{3, 2}, 0);
        dottedLinePaint.setPathEffect(effects);
        //虚线路径
        mxPath = new Path();
        myPath = new Path();

    }

    public void drawView() {
        height = this.getHeight();
        width = this.getWidth();

        if (mBufferBitmap == null) {
            mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mBufferCanvas = new Canvas(mBufferBitmap);
        }

        //绘制背景（纯黑）
        //canvas.drawColor(Color.BLACK);
        mBufferCanvas.drawRect(80, 0, width, height * 8 / 9, bgPaint);// 长方形
        mBufferCanvas.translate(80, 0);

        //绘制纵坐标轴
        mBufferCanvas.drawLine(0, 0, 0, height * 8 / 9, axisLinePaint);
        //绘制纵坐标刻线
        for (int i = 0; i < 13; i++) {
            if (i % 3 == 0) {
                mBufferCanvas.drawLine(-20, height * 8 / 9 * i / 12, 0, height * 8 / 9 * i / 12, axisLinePaint);
            } else
                mBufferCanvas.drawLine(-15, height * 8 / 9 * i / 12, 0, height * 8 / 9 * i / 12, calibrationLinePaint);
        }

        //绘制横坐标轴
        mBufferCanvas.drawLine(0, height * 8 / 9, width, height * 8 / 9, axisLinePaint);


        //绘制虚线网格
        int bound = height / 9;// 竖宽

        for (int i = 0; i < 8; i++) {

            for (int j = 0; j < 20; j++) {

                /*
                 * 画横线
                 */

                mxPath.reset();
                mxPath.moveTo(0, i * bound);
                mxPath.lineTo(width, i * bound);
                mBufferCanvas.drawPath(mxPath, dottedLinePaint);

                /*
                 * 画竖线
                 */
                myPath.reset();
                myPath.moveTo(j * width / 20, 0);
                myPath.lineTo(j * width / 20, height * 8 / 9);
                mBufferCanvas.drawPath(myPath, dottedLinePaint);
            }
        }
    }

    public void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        super.onFinishInflate();
        
        if (mBufferBitmap == null)
            drawView();
        else
            canvas.drawBitmap(mBufferBitmap, 0, 0, null);

        postInvalidate();
    }
}


