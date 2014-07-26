package com.noahgolmant.ImageToText.camera;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Noah on 7/6/2014.
 */
public class SelectionView extends View {

    private Context context;

    private Paint paint;
    private Rect rect;

    private int maskColor, frameColor;

    CameraActivity cameraActivity;


    public SelectionView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public SelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new Rect();

        // so we don't have to reference the xml file after initialization
        maskColor = context.getResources().getColor(android.R.color.darker_gray);
        frameColor = context.getResources().getColor(android.R.color.black);
    }

    public void setCameraActivity(CameraActivity activity) {
        this.cameraActivity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // get the rect for the current user selection where this will be drawn
        rect = this.cameraActivity.getSelectionRect();

        //border's properties
        paint.setColor(frameColor);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rect, paint);
    }

    public void redraw() {
        invalidate();
    }


}
