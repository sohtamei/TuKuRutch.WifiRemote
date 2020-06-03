package com.tukurutch.wifiremote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Created by Daniel on 7/25/2016.
 */
public class JoystickView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener
{
    private float centerX;
    private float centerY;
    private float baseRadius;
    private float hatRadius;
    private JoystickListener joystickCallback;

    private void setupDimensions()
    {   // Alex once left his computer unlocked...
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        baseRadius = Math.min(getWidth(), getHeight()) / 3;
        hatRadius = Math.min(getWidth(), getHeight()) / 10;
    }   // That is something you should never do...

    public JoystickView(Context context)
    {
        super(context);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }   // NEVER!!!

    public JoystickView(Context context, AttributeSet attributes, int style)
    {
        super(context, attributes, style);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }

    public JoystickView (Context context, AttributeSet attributes)
    {
        super(context, attributes);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }

    private void drawJoystick(float newX, float newY)
    {
        if(getHolder().getSurface().isValid())
        {
            Canvas myCanvas = this.getHolder().lockCanvas(); //Stuff to draw
            myCanvas.drawRGB(255,255,255);

            Paint colors = new Paint();
            colors.setARGB(255, 192, 192, 192);
            myCanvas.drawCircle(centerX, centerY, baseRadius, colors);

            colors.setARGB(255, 0, 0, 0);
            myCanvas.drawCircle(newX, newY, hatRadius, colors);
            getHolder().unlockCanvasAndPost(myCanvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        setupDimensions();
        drawJoystick(centerX, centerY);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public boolean onTouch(View v, MotionEvent e)
    {
        if(v.equals(this))
        {
            float x = 0;
            float y = 0;
            float k = 1;
            if(e.getAction() != e.ACTION_UP)
            {
                //centerX = v.getWidth()/2;
                x = (e.getX() - centerX)/baseRadius;
                y = (e.getY() - centerY)/baseRadius;
                if(x!=0 || y!=0) {
                    double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                     k = (float) ((Math.abs(x) > Math.abs(y)) ? Math.abs(r / x) : Math.abs(r / y));  // circle->square
                     if(r > 1) {
                       x /= r;
                       y /= r;
                     }
                }
            }
            drawJoystick(x*baseRadius + centerX, y*baseRadius + centerY);
            joystickCallback.onJoystickMoved(x*k, -y*k, getId());
        }
        return true;
    }

    public interface JoystickListener
    {
        void onJoystickMoved(float xPercent, float yPercent, int id);
    }
}
