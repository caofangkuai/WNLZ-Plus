package com.wunelezi.plus.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * TextView whose glyphs are painted with a continuously cycling rainbow gradient.
 */
public class GradientTextView extends AppCompatTextView {

    private static final int[] COLORS = {
            0xFFFF007F, 0xFF7F00FF, 0xFF00D2FF, 0xFF00FF7F, 0xFFFF007F
    };

    private final Matrix matrix = new Matrix();
    private LinearGradient gradient;
    private ValueAnimator animator;
    private int viewWidth;
    private float offset;

    public GradientTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public GradientTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GradientTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        if (w > 0) {
            gradient = new LinearGradient(0, 0, w, 0, COLORS, null, Shader.TileMode.MIRROR);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (gradient != null) {
            matrix.setTranslate(offset, 0);
            gradient.setLocalMatrix(matrix);
            getPaint().setShader(gradient);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(3600);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                offset = ((float) a.getAnimatedValue()) * viewWidth * 2f;
                invalidate();
            });
        }
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}
