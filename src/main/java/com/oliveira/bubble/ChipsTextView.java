package com.oliveira.bubble;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class ChipsTextView extends View implements ILayoutCallback {

    public static boolean DEBUG;

    protected BubbleSpan selectedSpan;
    protected ArrayList<BubbleSpan> spans = new ArrayList<BubbleSpan>();
    protected HashMap<BubbleSpan, ArrayList<Rect>> positions = new HashMap<BubbleSpan, ArrayList<Rect>>();
    protected Spannable text;
    protected Spannable moreText;
    protected TextPaint textPaint;
    protected StaticLayout truncatedLayout;
    protected StaticLayout expandedLayout;
    protected boolean truncated;
    protected boolean animating;
    protected OnBubbleClickedListener listener;
    protected float lineSpacing = 1.25f;
    protected int maxLines = 0;

    public ChipsTextView(Context context) {
        super(context);
    }

    public ChipsTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChipsTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StaticLayout layout() {
        return truncated ? truncatedLayout : expandedLayout;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        x += getScrollX();
        y += getScrollY();

        boolean retValue = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                retValue = selectBubble(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (selectedSpan != null) {
                    selectedSpan.setPressed(spanContains(selectedSpan, x, y), (Spannable) layout().getText());
                    retValue = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (listener != null && selectedSpan != null && spanContains(selectedSpan, x, y)) {
                    listener.onBubbleClicked(this, selectedSpan);
                }

            case MotionEvent.ACTION_CANCEL:
                if (selectedSpan != null) {
                    retValue = true;
                    if ( -1 != ((Spannable)truncatedLayout.getText()).getSpanEnd(selectedSpan)) {
                        selectedSpan.setPressed(false, (Spannable)truncatedLayout.getText());
                    }
                    if ( -1 != ((Spannable)expandedLayout.getText()).getSpanEnd(selectedSpan)) {
                        selectedSpan.setPressed(false, (Spannable)expandedLayout.getText());
                    }
                }
                selectedSpan = null;
                break;

            default:
                break;
        }

        invalidate();
        return retValue;
    }

    private boolean spanContains(BubbleSpan span, int x, int y) {

        ArrayList<Rect> rects = positions.get(span);
        if (rects != null) {
            for (Rect rect : rects) {
                if (rect.contains(x, y)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean selectBubble(int x, int y) {
        for (BubbleSpan span : spans) {
            if (spanContains(span, x, y)) {
                span.setPressed(true, (Spannable)layout().getText());
                selectedSpan = span;
                return true;
            }
        }
        return false;
    }

    public void setTextPaint(TextPaint textPaint) {
        this.textPaint = textPaint;
    }

    public void setText(Spannable text) {
        this.text = text;
        if (getWidth() > 0) {
            requestLayout();
        }
    }

    private void recomputeSpans(Spannable text) {

        spans.clear();

        Collections.addAll(
                spans,
                text.getSpans(0, text.length(), BubbleSpan.class));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (layout() == null) {
            return;
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());
        layout().draw(canvas);

        if (DEBUG) {

            int n = layout().getLineCount();
            for (int i = 0; i < n; i++) {

                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);

                Rect bounds = new Rect();
                layout().getLineBounds(i, bounds);
                canvas.drawRect(bounds, paint);

                int baseLine = layout().getLineBaseline(i);
                drawLine(canvas, bounds, paint, baseLine, Color.GREEN);
                drawLine(canvas, bounds, paint, baseLine + layout().getPaint().getFontMetricsInt().descent, Color.BLUE);
            }
        }
    }

    private void drawLine(Canvas canvas, Rect line, Paint paint, int height, int color) {
        paint.setColor(color);
        canvas.drawLine(line.left, height, line.right, height, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = widthSize;
        int height = heightSize;

        if (!animating) {
            build(width);
            height = layout() == null ? 0 : layout().getHeight() + getPaddingTop() + getPaddingBottom();

            if (layout() != null && layout().getLineCount() > 0) {

                height -= (layout().getLineBottom(0) - layout().getPaint().getFontMetricsInt().descent - layout().getLineBaseline(0));

                if (layout().getLineCount() == 1 && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
                    width = (int) (layout().getLineWidth(0) + getPaddingLeft() + getPaddingRight());
                }
            }
        }

        this.setMeasuredDimension(width, height);
    }

    private void build(int width) {

        positions.clear();
        width = width - getPaddingLeft() - getPaddingRight();

        if (width <= 0 || TextUtils.isEmpty(text)) {
            truncatedLayout = expandedLayout = null;
            return;
        }

        for (BubbleSpan span : spans) {
            span.resetWidth(width);
        }

        try {

            truncatedLayout = expandedLayout = new StaticLayout(text,
                    textPaint,
                    width,
                    Layout.Alignment.ALIGN_NORMAL,
                    lineSpacing,
                    1,
                    false);

            if (maxLines > 0 && truncatedLayout.getLineCount() > maxLines) {

                int lineEnd = truncatedLayout.getLineEnd(maxLines - 1);
                int offset = -1;
                StaticLayout sl = new StaticLayout(moreText,
                        textPaint,
                        width,
                        Layout.Alignment.ALIGN_NORMAL,
                        lineSpacing,
                        1,
                        false);

                sl.getWidth();
                while (truncatedLayout.getLineCount() > maxLines && lineEnd > 0) {

                    if (offset == -1 && truncatedLayout.getLineWidth(maxLines - 1) + sl.getLineWidth(0) > width) {

                        offset = truncatedLayout.getOffsetForHorizontal(
                                maxLines - 1,
                                width - sl.getLineWidth(0));

                        lineEnd = offset;

                    } else if (offset > 0) {
                        lineEnd--;
                    }

                    SpannableStringBuilder textTruncated = new SpannableStringBuilder(text.subSequence(0, lineEnd));
                    textTruncated.append(moreText);

                    truncatedLayout = new StaticLayout(textTruncated,
                            textPaint,
                            width,
                            Layout.Alignment.ALIGN_NORMAL,
                            lineSpacing,
                            1,
                            false);
                }
            }
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            return;
        }

        if (truncated) {
            recomputeSpans((Spannable)truncatedLayout.getText());
        } else {
            recomputeSpans((Spannable)expandedLayout.getText());
        }

        for (BubbleSpan span : spans) {
            positions.put(span, span.rect(this));
        }
    }

    @Override
    public Point getCursorPosition(int pos) {
        int line = layout().getLineForOffset(pos);
        int baseline = layout().getLineBaseline(line);
        int ascent = layout().getLineAscent(line);
        float x = layout().getPrimaryHorizontal(pos);
        float y = baseline + ascent;
        return new Point((int)x+getPaddingLeft(), (int)y+getPaddingTop());
    }

    @Override
    public int getLine(int pos) {
        return layout().getLineForOffset(pos);
    }

    @Override
    public Spannable getSpannable() {
        return ((Spannable)layout().getText());
    }

    @Override
    public int getLineEnd(int line) {
        return layout().getLineEnd(line);
    }

    @Override
    public int getLineHeight() {
        return layout().getLineBottom(0);
    }

    public interface OnBubbleClickedListener {
        public void onBubbleClicked(View view, BubbleSpan bubbleSpan);
    }

    public void setOnBubbleClickedListener(OnBubbleClickedListener listener) {
        this.listener = listener;
    }

    public void setLineSpacing(float value) {
        lineSpacing = value;
    }

    public int getTextSize() {
        return (int)textPaint.getTextSize();
    }

    public void setMaxLines(int maxLines, Spannable moreText) {
        truncated = true;
        this.moreText = moreText;
        this.maxLines = maxLines;
    }

    public void expand(boolean animate) {

        if (!truncated) {
            return;
        }

        truncated = false;
        if (!animate) {
            requestLayout();
            return;
        }

        ResizeAnimation expandAnimation = new ResizeAnimation(
                getHeight() + expandedLayout.getHeight() - truncatedLayout.getHeight()
        );

        expandAnimation.setDuration(400);
        expandAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { animating = true; }
            @Override public void onAnimationEnd(Animation animation) { animating = false; }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        startAnimation(expandAnimation);
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public class ResizeAnimation extends Animation {
        final int startHeight;
        final int targetHeight;

        public ResizeAnimation(int targetHeight) {
            this.targetHeight = targetHeight;
            startHeight = getHeight();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            getLayoutParams().height = (int) (startHeight + (targetHeight - startHeight) * interpolatedTime);
            requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}