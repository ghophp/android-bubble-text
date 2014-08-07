package com.oliveira.bubble;

import android.content.Context;
import android.graphics.*;
import android.text.TextPaint;

public class CursorDrawable {

    private final Paint paint;
    private ChipsEditText editText;
    private float textSize;
    private float cursorWidth;
    private AwesomeBubble bubble;
    public int color = 0xffffffff;

    public CursorDrawable(ChipsEditText editText, float textSize, float cursorWidth, Context context) {

        this.editText = editText;
        this.paint = new Paint();

        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        this.textSize = textSize;
        this.cursorWidth = cursorWidth;

        bubble = new AwesomeBubble(" ", 100, DefaultBubbles.get(DefaultBubbles.GRAY_WHITE_TEXT, context, (int)editText.getTextSize()), new TextPaint());
    }

    public void draw(Canvas canvas, boolean blink) {

        Point p = editText.getCursorPosition();
        canvas.save();
        canvas.translate(p.x, p.y);

        if (editText.manualModeOn) {

            int x_offset;
            int y_offset = bubble.style.bubblePadding;
            int y_h = bubble.getHeight() - 2*bubble.style.bubblePadding;

            canvas.translate(0, -BubbleSpanImpl.lineCorrectionLogic(
                    editText.getSelectionStart(),
                    editText,
                    bubble));

            if (editText.manualStart == editText.getSelectionStart()) {
                bubble.draw(canvas);
                x_offset = - bubble.getWidth()/2;
            } else {
                x_offset = 2*bubble.style.bubblePadding;
            }

            if (blink) {
                paint.setColor(0xffffffff);
                canvas.drawRect(0 - x_offset, y_offset, cursorWidth - x_offset, y_offset + y_h, paint);
            }

        } else if (blink) {
            paint.setColor(color);
            canvas.drawRect(0, 0, cursorWidth, textSize, paint);
        }
        canvas.restore();
    }

    public Point bubble_offset() {

        int x_offset = 0;
        int y_offset = 0;

        if (editText.manualModeOn) {
            y_offset = - bubble.getHeight();
            if (editText.manualStart == editText.getSelectionStart()) {
                x_offset = -bubble.getWidth() / 2;
            } else {
                x_offset = 2 * bubble.style.bubblePadding;
            }
        }

        return new Point(x_offset, y_offset);
    }

    public void setColor(int color) {
        this.color = color;
    }
}