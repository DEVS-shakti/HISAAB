package com.shakti.hisaab.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.shakti.hisaab.R;

import java.util.ArrayList;
import java.util.List;

public class MiniTrendChartView extends View {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> points = new ArrayList<>();

    public MiniTrendChartView(Context context) {
        super(context);
        init();
    }

    public MiniTrendChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniTrendChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        fillPaint.setAlpha(45);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_light));

        dotFillPaint.setStyle(Paint.Style.FILL);
        dotFillPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_card));

        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(dp(2));
        dotStrokePaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
    }

    public void setPoints(List<Float> values) {
        points.clear();
        if (values != null) {
            points.addAll(values);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.size() < 2) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float padding = dp(10);
        float drawableWidth = width - (padding * 2f);
        float drawableHeight = height - (padding * 2f);

        float min = points.get(0);
        float max = points.get(0);
        for (Float value : points) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        if (max == min) {
            max += 1f;
        }

        float stepX = drawableWidth / (points.size() - 1);
        Path linePath = new Path();
        Path fillPath = new Path();
        float[] xs = new float[points.size()];
        float[] ys = new float[points.size()];

        for (int i = 0; i < points.size(); i++) {
            float x = padding + (stepX * i);
            float y = padding + drawableHeight - (((points.get(i) - min) / (max - min)) * drawableHeight);
            xs[i] = x;
            ys[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, height - padding);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(xs[points.size() - 1], height - padding);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        float radius = dp(4);
        for (int i = 0; i < xs.length; i++) {
            canvas.drawCircle(xs[i], ys[i], radius, dotFillPaint);
            canvas.drawCircle(xs[i], ys[i], radius, dotStrokePaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
