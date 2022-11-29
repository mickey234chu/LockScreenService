package com.example.bootservice;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class MoniterView extends View {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;


    public MoniterView(Context context) {
        super(context);

    }

    @Override
    public void onAttachedToWindow()
    {
        super.onAttachedToWindow();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //Log.d("testkey", "keyCode:" + event.getKeyCode());
        Log.d("testkey","dispatch:KeyEvent" + event.getKeyCode());
        return true;
       /* switch (event.getKeyCode())
        {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_HOME:
                Log.d("testkey","KeyEvent" + event.getKeyCode());
                return  true;
            default:
                Log.d("testkey","KeyEvent.?");
                return super.dispatchKeyEvent(event);
        }*/

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
       /*switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                Log.d("testkey","KeyEvent.KEYCODE_BACK");
                break;
            case KeyEvent.KEYCODE_MENU:
                Log.d("testkey","KeyEvent.KEYCODE_MENU");
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Log.d("testkey","KeyEvent.KEYCODE_VOLUME_DOWN");
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                Log.d("testkey","KeyEvent.KEYCODE_VOLUME_UP");
                break;
            case KeyEvent.KEYCODE_HOME:
                Log.d("testkey","KeyEvent.KEYCODE_HOME");
                break;
            default:
                Log.d("testkey","KeyEvent.?");
                break;
        }
        return  super.onKeyDown(keyCode,event);*/
        Log.d("testkey","down:KeyEvent.?");
        return  true;
    }
    @Override
    public  boolean onKeyUp(int keyCode, KeyEvent event)
    {
        Log.d("testkey","up:KeyEvent.?");
        return  true;
        //return  super.onKeyUp(keyCode,event);
    }

}