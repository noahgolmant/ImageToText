package com.noahgolmant.ImageToText;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Noah on 6/20/2014.
 */
public class AlignActivity extends Activity implements View.OnTouchListener{

    private ImageView image;
    private Uri imageUri;
    private Matrix rotateMatrix = new Matrix();
    private float startX;
    Point size = new Point(); // screen size

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.align_layout);

        // store the screen's size for checking the drag distance later on
        getWindowManager().getDefaultDisplay().getSize(size);

        image = (ImageView) findViewById(R.id.rotateImageView);

        // get the image we were sent via extra info from the intent
        imageUri = (Uri)getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);
        image.setImageURI(imageUri);

        // set the touch listener for the main view to interact with the image view
        this.findViewById(R.id.layout_view).setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction() & MotionEvent.ACTION_MASK) {
            // Placed first pointer down; store the initial x position.
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            // Called whenever the first pointer is moved.
            // We get the delta from the initial x position and calculate a rotation based on
            // how large the delta is compared to the width of the entire screen
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - startX;
                float rotate = (dx / (float)size.x) * 30;
                rotateMatrix.postRotate(rotate, image.getX(), image.getY());
                break;
            default:
                break;
        }
        // apply our rotation matrix to the main image view
        image.setImageMatrix(rotateMatrix);
        return true;

    }
}