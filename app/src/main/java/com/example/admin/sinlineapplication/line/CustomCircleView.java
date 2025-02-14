package com.example.admin.sinlineapplication.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.admin.sinlineapplication.R;

public class CustomCircleView extends View {
    private Paint circlePaint;
    private Paint linePaint;
    
    private Paint textPaint;
    private int radius;

    public CustomCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLACK); // 圆内为黑色
        circlePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE); // 文字和刻度为白色
        textPaint.setTextSize(30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        radius = Math.min(w, h) / 2 - 50; // 留出边距
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 绘制圆形
        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // 绘制外边界
        linePaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius, linePaint);

        // 绘制三条线
        drawDividingLines(canvas, centerX, centerY);

        // 绘制刻度值
        drawScale(canvas, centerX, centerY);    }

    private void drawDividingLines(Canvas canvas, int centerX, int centerY) {
        // 定义线条颜色
        int[] colors = {Color.parseColor("#FF0000"),Color.parseColor("#FFFF00") ,Color.parseColor("#7FFFD4")};
        float[] angles = {90, 210, 330};

        for (int i = 0; i < 3; i++) {
            linePaint.setColor(colors[i]);
            float startX = centerX + radius * (float) Math.cos(Math.toRadians(-angles[i]));
            float startY = centerY + radius * (float) Math.sin(Math.toRadians(-angles[i]));
            canvas.drawLine(centerX, centerY, startX, startY, linePaint);
        }
    }
    private void drawScale(Canvas canvas, int centerX, int centerY) {
        linePaint.setColor(Color.WHITE); // 设置竖线颜色
        linePaint.setStrokeWidth(2); // 设置竖线宽度

        int scaleCount = 12; // 刻度数量
        float lineLength = 15; // 竖线长度
        float distanceFromCircle = 5; // 与圆的距离
        float scaleOffset = 5; // 刻度与竖线之间的距离

        for (int i = 0; i < scaleCount; i++) {
            float angle = (float) (i * 30); // 每个竖线的角度
            float angler = (float) (i*40);

            // 计算竖线的起点和终点
            float lineStartX = centerX + (radius + distanceFromCircle) * (float) Math.cos(Math.toRadians(angle - 90));
            float lineStartY = centerY + (radius + distanceFromCircle) * (float) Math.sin(Math.toRadians(angle - 90));

            float lineEndX = centerX + (radius + distanceFromCircle + lineLength) * (float) Math.cos(Math.toRadians(angle - 90));
            float lineEndY = centerY + (radius + distanceFromCircle + lineLength) * (float) Math.sin(Math.toRadians(angle - 90));

            // 绘制竖线
            canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, linePaint);

            int  cruuents1 = i*30;
            // 绘制刻度
            int current = i*30;

            String scaleValue = String.valueOf(i * 30); // 刻度值
            float scaleX = lineEndX +  (float) Math.cos(Math.toRadians(angle - 90));
            float scaleY;
//            if(current ==0 ||current==30||current==60||current ==330 ||current==300){
//                scaleY = lineEndY + (float) Math.sin(Math.toRadians(angle - 90))-scaleOffset;
//            }else if(current == 90){
//                scaleX = lineEndX +  (float) Math.cos(Math.toRadians(angle - 90))+20;
//                scaleY = lineEndY + (float) Math.sin(Math.toRadians(angle - 90))+10;
//            }else if(current == 270){
//                scaleX = lineEndX +  (float) Math.cos(Math.toRadians(angle - 90))-20;
//                scaleY = lineEndY + (float) Math.sin(Math.toRadians(angle - 90))+10;
//            }else{
//                scaleY = lineEndY + (float) Math.sin(Math.toRadians(angle - 90))+30 ;
//            }
//
//            canvas.drawText(scaleValue, scaleX - textPaint.measureText(scaleValue) / 2, scaleY, textPaint);
        }
    }
}
