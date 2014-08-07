package com.oliveira.bubble;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.asolutions.widget.RowLayout;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class AutoCompletePopover extends RelativeLayout {

    private static final double TRIANGLE = Math.PI / 2.0;

    private RelativeLayout root;
    private ChipsEditText et;
    private Adapter adapter;
    private ScrollView scrollView;
    private InputMethodManager imm;

    private int bgColor;
    private Paint bgPaint;

    public AutoCompletePopover(Context context) {
        super(context);
        init(R.layout.autocomplete_popover, R.layout.autocomplete_row);
    }

    public AutoCompletePopover(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styledAttributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.AutoCompletePopover);

        startWithAttributes(styledAttributes);
    }

    public AutoCompletePopover(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray styledAttributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.AutoCompletePopover);

        startWithAttributes(styledAttributes);
    }

    private void startWithAttributes(TypedArray styledAttributes) {

        int viewId = styledAttributes.getResourceId(
                R.styleable.AutoCompletePopover_view,
                R.layout.autocomplete_popover);

        int rowId = styledAttributes.getResourceId(
                R.styleable.AutoCompletePopover_row,
                R.layout.autocomplete_row);

        bgColor = styledAttributes.getColor(
                R.styleable.AutoCompletePopover_color,
                android.R.color.darker_gray);

        styledAttributes.recycle();
        init(viewId, rowId);
    }

    void init(int viewId, int rowId) {

        LayoutInflater.from(getContext()).inflate(viewId, this, true);
        scrollView = (ScrollView)findViewById(R.id.suggestions);

        ViewGroup vg;
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            vg = new RowLayout(getContext(), null);
        } else {
            vg = new LinearLayout(getContext());
            ((LinearLayout) vg).setOrientation(LinearLayout.VERTICAL);
        }

        scrollView.addView(vg, -2, -2);

        adapter = new Adapter(vg, rowId);
        adapter.onItemClickListener = onItemClickListener;
        setVisibility(View.GONE);

        OnClickListener x = new OnClickListener() {
            @Override
            public void onClick(View v) {
                et.onXPressed();
                et.cancelManualMode();
            }
        };

        findViewById(R.id.x_border).setOnClickListener(x);
        imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setChipsEditText(ChipsEditText et) {
        this.et = et;
    }

    public void setItems(ArrayList<Entity> items) {
        adapter.setItems(items);
    }

    public RelativeLayout root() {
        if (root == null) {
            root = (RelativeLayout) getParent();
            root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    reposition();
                }
            });
        }
        return root;
    }

    public void reposition() {

        Point p = et.getCursorPosition();
        Point bOff = et.cursorDrawable.bubble_offset();
        p.offset(-bOff.x, -bOff.y);
        p.y -= et.getScrollY();

        int topMargin = et.getTop() + p.y;
        if (topMargin > et.getBottom()) topMargin = et.getBottom();
        ((RelativeLayout.LayoutParams)getLayoutParams()).topMargin = topMargin;

        getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        xTriOffset = p.x + et.getLeft();
        invalidate();
    }

    public void show() {

        if (!et.canAddMoreBubbles()) {
            return;
        }

        reposition();
        setVisibility(View.VISIBLE);
    }

    public boolean isHidden() {
        return getVisibility() == View.GONE;
    }

    public void hide() {
        setVisibility(View.GONE);
        if (et.manualModeOn) {
            et.endManualMode();
        }
        if (onHideListener != null) {
            onHideListener.onHide(this);
        }
    }

    public static class Adapter extends BaseAdapter {

        private ArrayList<Entity> items = new ArrayList<Entity>();
        private LayoutInflater li;
        private ViewGroup vg;
        private int rowId;

        public Adapter(ViewGroup vg, int rowId) {
            this.vg = vg;
            this.rowId = rowId;
        }

        public void setItems(ArrayList<Entity> items) {

            if (items == null) {
                items = new ArrayList<Entity>();
            }

            this.items = items;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(final int position, View c, ViewGroup parent) {

            if (li == null) {
                li = LayoutInflater.from(parent.getContext());
            }

            ViewHolder h;
            if (c == null) {
                h = new ViewHolder();
                c = li.inflate(rowId, null);
                h.title = (TextView)c.findViewById(R.id.title);
                c.setTag(h);
            } else {
                h = (ViewHolder)c.getTag();
            }

            h.title.setText(items.get(position).label);
            h.title.setTag(items.get(position).data);

            h.title.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(null, v, position, 0);
                }
            });

            return c;
        }

        public static class ViewHolder {
            TextView title;
        }

        @Override
        public void notifyDataSetChanged() {
            vg.removeAllViews();
            for (int i = 0; i < items.size(); i++) {
                View view = getView(i, null, vg);
                vg.addView(view);
            }
        }

        public AdapterView.OnItemClickListener onItemClickListener;
    }

    public AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            et.onBubbleSelected(position);
            et.manualModeOn = false;
            et.muteHashWatcher(true);

            String textToAdd = adapter.items.get(position).label;
            if (et.lastEditAction != null) {
                try {
                    Editable t = et.getText();
                    if (t.subSequence(et.lastEditAction.start, et.lastEditAction.end()).toString().equals(et.lastEditAction.text)) {
                        t.delete(et.lastEditAction.start, et.lastEditAction.end());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            et.addBubble(textToAdd, et.manualStart, adapter.items.get(position).data);
            if (et.getSelectionEnd() == et.length() || et.getSelectionEnd() + 1 == et.length()) {
                et.append(" ");
                imm.restartInput(et);
            }

            hide();
            et.muteHashWatcher(false);
        }
    };

    OnHideListener onHideListener;
    public void setOnHideListener(OnHideListener onHideListener) {
        this.onHideListener = onHideListener;
    }

    public interface OnHideListener {
        public void onHide(View view);
    }

    public void scrollToTop() {
        scrollView.scrollTo(0, 0);
    }

    public void setBackgroundColor(int color) {
        this.bgColor = color;
    }

    int xTriOffset;

    @SuppressWarnings("NullableProblems")
    @Override
    protected void dispatchDraw(Canvas canvas) {

        int x_start = 0;
        int y_start = getPaddingTop();

        int x_end = getWidth();
        int y_end = getHeight();

        int tri_h = getPaddingTop();
        int tri_base = (int)(Math.tan(TRIANGLE / 2) * tri_h);

        Path path = new Path();
        path.moveTo(x_start, y_start);

        path.lineTo(xTriOffset - tri_base, y_start);
        path.lineTo(xTriOffset, y_start - tri_h);
        path.lineTo(xTriOffset + tri_base, y_start);

        path.lineTo(x_end, y_start);
        path.lineTo(x_end, y_end);
        path.lineTo(x_start, y_end);
        path.close();

        canvas.drawPath(path, bgPaint);
        super.dispatchDraw(canvas);
    }

    public static class Entity {
        public String label;
        public Object data;
    }
}