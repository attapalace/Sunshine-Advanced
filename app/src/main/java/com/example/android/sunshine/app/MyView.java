package com.example.android.sunshine.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Created by ahmedatta on 3/29/16.
 */
public class MyView extends View {

    private static final String LOG_TAG = MyView.class.getSimpleName();
    private float windDirection;
    private String windDirectionText = "";
    Paint mPaint;

    public MyView(Context context) {
        super(context);

    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyView(Context context, AttributeSet attrs, int DefaultStyle) {
        super(context, attrs, DefaultStyle);
        init();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(windDirectionText);
        return true;
    }

    public void setWindDirection(float windDirection) {
        final String oldWindDirectionText = windDirectionText;
        this.windDirection = windDirection;
        this.windDirectionText = Utility.getFormattedWindDirection(windDirection);
        if (!windDirectionText.equals(oldWindDirectionText)) {
            requestLayout(); //call to onMeasure for the parent hierarchy
            invalidate(); // call to onDraw for this view
        }
    }

    private void init() {
        
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(8);
        mPaint.setColor(Color.GRAY);

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    @Override
    protected void onMeasure(int wMeasureSpec, int hMeasureSpec) {
        int hSpecMode = MeasureSpec.getMode(hMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(hMeasureSpec);
        int myHeight = hSpecSize;

        if (hSpecMode == MeasureSpec.EXACTLY) {
            myHeight = hSpecSize;
        } else if ( hSpecMode == MeasureSpec.AT_MOST){
            //wrap content
            myHeight = Math.min(myHeight , hSpecSize);
        }

        int wSpecMode = MeasureSpec.getMode(wMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(wMeasureSpec);
        int myWidth = wSpecSize;

        if (wSpecMode == MeasureSpec.EXACTLY) {
            myWidth = wSpecSize;
        } else if ( wSpecMode == MeasureSpec.AT_MOST){
            //wrap content
            myWidth = Math.min(myWidth , wSpecSize);
        }

        setMeasuredDimension(myWidth, myHeight);
    }
//
//    @Override
//    protected void onDraw(Canvas canvas){
//        super.onDraw(canvas);
//
//        int w = getMeasuredWidth();
//        int h = getMeasuredHeight();
//        int r;
//        if(w > h) {
//            r = h/2 -3;
//        }else{
//            r = w/2 -3;
//        }
//
//        canvas.drawCircle(w/2, h/2, r, mPaint);
//
//        canvas.drawLine(
//                w/2,
//                h/2,
//                (float)(w/2 + r * Math.sin(-windDirection)),
//                (float)(h/2 - r * Math.cos(-windDirection)),
//                mPaint);
//    }

}
