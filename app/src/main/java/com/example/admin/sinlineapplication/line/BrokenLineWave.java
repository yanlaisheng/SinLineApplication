package com.example.admin.sinlineapplication.line;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.admin.sinlineapplication.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.admin.sinlineapplication.MainActivity.tempDouble;
import static com.example.admin.sinlineapplication.MainActivity.tempDouble2;
public class BrokenLineWave extends View {

    private Canvas mBufferCanvas;
    private Bitmap mBufferBitmap;

    private Path path0, path1, path2;
    private Paint paint0, paint1, paint2;

    private Paint textLinePaint;
    public static List<String> times = new ArrayList<>();

    private static float extremum0 = 17000.0f;
    private static float extremum = 17000.0f;

    private static float amplifier0 = 170.0f;
    private static float amplifier = 170.0f;

    private static float amplifier_u0 = 170.0f;
    private static float amplifier_v0 = 170.0f;
    private static float amplifier_w0 = 170.0f;

    private static float amplifier_u = 170.0f;
    private static float amplifier_v = 170.0f;
    private static float amplifier_w = 170.0f;

    private static float coefficient = 1;//幅值系数

    private static float frequency0 = 2.0f;//频率
    private static float frequency = 50.0f;//频率
    private static float phase = 0.0f;//初始相位
    private static float speed = 1;//秒值
    private int height = 0;
    private int width = 0;
    private int j = 0;
    private List<Float>
            pointY1 = new ArrayList<Float>(),
            pointY2 = new ArrayList<Float>(),
            pointY3 = new ArrayList<Float>();

    public BrokenLineWave(Context context) {
        super(context);
        initView();
    }


    //如果不写下面的构造函数，则会报错：custom view SineWave is not using the 2- or 3-argument View constructors

    public BrokenLineWave(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public float GetExtremum() {
        extremum = (int)tempDouble2+20;
        return extremum;
    }

    public float getAmplifier_u() {
        amplifier_u0 = (int)tempDouble2+10;
        return amplifier_u0 ;

    }
    public float getAmplifier_v() {
        amplifier_v0 = (int)tempDouble2+10;

        return amplifier_v0 ;
    }

    public float getAmplifier_w() {
        amplifier_w0 = (int)tempDouble2+10;
        return amplifier_w0 ;
    }

    public float GetFrequency() {
        return frequency;
    }

    public float GetSpeed() {
        return speed;
    }

    public void Set(float extremum,
                    float amplifier_u,
                    float amplifier_v,
                    float amplifier_w,
                    float frequency,
                    float speed) {

        this.coefficient = extremum / this.extremum0;

        this.amplifier = this.amplifier0 / coefficient;

        this.amplifier_u0 = amplifier_u / 200;
        this.amplifier_v0 = amplifier_v / 200;
        this.amplifier_w0 = amplifier_w / 200;

        this.frequency = frequency;
        this.extremum = (float) tempDouble2+20;
        this.speed = speed;
    }

    private void initView() {

        //文字画笔
        textLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textLinePaint.setTextAlign(Paint.Align.RIGHT);
        textLinePaint.setStrokeWidth(1);
        textLinePaint.setTextSize(12f);
        textLinePaint.setStyle(Paint.Style.STROKE);
        textLinePaint.setColor(getResources().getColor(R.color.white));

        //红线路径
        path0 = new Path();
        //红线画笔
        paint0 = new Paint();
        paint0.setColor(getResources().getColor(R.color.redline));
        paint0.setStrokeWidth(4);
        paint0.setStyle(Paint.Style.STROKE);
        paint0.setAntiAlias(true);

        //蓝线路径
        path1 = new Path();
        //蓝线画笔
        paint1 = new Paint();
        paint1.setColor(getResources().getColor(R.color.blueline));
        paint1.setStrokeWidth(4);
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setAntiAlias(true);

        //黄线路径
        path2 = new Path();
        //黄线画笔
        paint2 = new Paint();
        paint2.setColor(getResources().getColor(R.color.yellowline));
        paint2.setStrokeWidth(4);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setAntiAlias(true);

    }

    public void drawView() {
        height = this.getHeight();
        width = this.getWidth();

        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);

        for (int i = 0; i < 13; i++) {
            if (i % 3 == 0) {
                if (i == 0)
                    mBufferCanvas.drawText(extremum - ((extremum / 2) * (i / 3)) + " ", 60, height * 8 / 9 * i / 12 + 10, textLinePaint);
                else
                    mBufferCanvas.drawText(extremum - ((extremum / 2) * (i / 3)) + " ", 60, height * 8 / 9 * i / 12 + 5, textLinePaint);
            }
        }

        if (times.size() > 0) {
            for (int i = 0; i < times.size(); i++) {
                mBufferCanvas.drawText(times.get(i), 80 + i * width / 10 +25+ 20, height * 8 / 9 + 12, textLinePaint);
            }
        }
    }

    public void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        drawView();
        canvas.drawBitmap(mBufferBitmap, 0, 0, null);

        //绘制正弦线
        canvas.drawPath(path0, paint0);
        canvas.drawPath(path1, paint1);
        canvas.drawPath(path2, paint2);

        amplifier_u = amplifier * (amplifier_u0 / this.amplifier0);
        amplifier_v = amplifier * (amplifier_v0 / this.amplifier0);
        amplifier_w = amplifier * (amplifier_w0 / this.amplifier0);

        amplifier_u = (amplifier_u * 2 > height * 8 / 9) ? ((height * 8 / 9) / 2) : amplifier_u+5;
        amplifier_v = (amplifier_v * 2 > height * 8 / 9) ? ((height * 8 / 9) / 2) : amplifier_v+5;
        amplifier_w = (amplifier_w * 2 > height * 8 / 9) ? ((height * 8 / 9) / 2) : amplifier_w+5;

        final float cy = (height * 8 / 9) / 2;

        postDelayed(new Runnable() {

            @Override
            public void run() {

                // TODO Auto-generated method stub

                if (j < width) {
                    pointY1.add(cy - amplifier_u * (float) (Math.sin(phase * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                    pointY2.add(cy - amplifier_v * (float) (Math.sin((phase + 120.0f) * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                    pointY3.add(cy - amplifier_w * (float) (Math.sin((phase + 240.0f) * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                } else {
                    pointY1.remove(0);
                    pointY2.remove(0);
                    pointY3.remove(0);
                    pointY1.add(cy - amplifier_u * (float) (Math.sin(phase * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                    pointY2.add(cy - amplifier_v * (float) (Math.sin((phase + 120.0f) * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                    pointY3.add(cy - amplifier_w * (float) (Math.sin((phase + 240.0f) * 2 * (float) Math.PI / 360.0f + 2 * Math.PI * (frequency0 + frequency / 10) * j / width)));
                }
                path0.reset();
                path1.reset();
                path2.reset();

                j += 5;

                for (int i = 0; i < pointY1.size(); i++) {
                    if (i == 0) {
                        //设置path的起点
                        path0.moveTo(80, pointY1.get(i));
                        path1.moveTo(80, pointY2.get(i));
                        path2.moveTo(80, pointY3.get(i));
                    } else {
                        //连线
                        path0.lineTo(i * 5 + 80, pointY1.get(i));
                        path1.lineTo(i * 5 + 80, pointY2.get(i));
                        path2.lineTo(i * 5 + 80, pointY3.get(i));
                    }
                }
                postInvalidate();
            }
        }, (long) (speed * 100));

    }

}
