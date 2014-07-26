package com.noahgolmant.ImageToText.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.text.Selection;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.noahgolmant.ImageToText.DecodeTask;
import com.noahgolmant.ImageToText.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Noah on 7/5/2014.
 */
public class CameraActivity extends Activity implements View.OnClickListener, View.OnTouchListener, Camera.PictureCallback, DecodeTask.DecodeInterface {

    private static int MIN_SELECTION_AREA = 100;

    private Camera camera;
    private CameraPreview preview;
    private SelectionView selection;
    private Button captureButton;

    private Bitmap currentImage = null;

    /// whether or not we want to allow a preview
    private boolean isPreviewOn = true;

    // Get start and end coordinates to create the bounding rect to represent the user's text selection
    private int startX, startY, currentX, currentY;
    private Rect selectionRect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);

        // Get the static camera instance from our hardware
        camera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        preview = new CameraPreview(this, camera);
        FrameLayout previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
        previewFrame.addView(preview);

        // Initialize the selection overlay view
        selection = (SelectionView) findViewById(R.id.selection_overlay_view);
        selection.setCameraActivity(this);
        selection.setOnTouchListener(this);

        // Initialize the rect that represents the user's on-screen selection
        selectionRect = new Rect();

        // Initialize the capture button to use extracted text
        captureButton = (Button) findViewById(R.id.capture_text_button);
        captureButton.setOnClickListener(this);

        captureButton.setEnabled(false);

    }

    @Override
    public void onClick(View v) {
        DecodeTask decoder = new DecodeTask(this);
        decoder.intent = this;
        decoder.execute(currentImage);

        captureButton.setEnabled(false);
        //camera.startPreview();
        //isPreviewOn = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()) {
            // Just placed first pointer down, prepare
            // Store the starting position
            case MotionEvent.ACTION_DOWN:
                startX = (int) event.getX();
                startY = (int) event.getY();

                currentX = (int) event.getX();
                currentY = (int) event.getY();

                if(!isPreviewOn)
                    camera.startPreview();

                break;
            //If the single pointer is moved, update the rect accordingly.
            case MotionEvent.ACTION_MOVE:
                // Store the most recent touch coordinate.
                currentX = (int) event.getX();
                currentY = (int) event.getY();

                // use the start and end coordinates to create the user's selection area.
                selectionRect.set(
                        Math.min(startX, currentX),
                        Math.min(startY, currentY),
                        Math.max(startX, currentX),
                        Math.max(startY, currentY)
                );

                break;
            case MotionEvent.ACTION_UP:
                currentX = (int) event.getX();
                currentY = (int) event.getY();
                break;
            default:
                break;
        }

        // Only draw a new selection area if it is bigger than the given threshold.
        if(selectionRect.height() * selectionRect.width() >= MIN_SELECTION_AREA) {
            selection.redraw(); //re-draws the canvas with the rect

            // check if the hardware comes with focus capabilities
            if(camera.getParameters().getMaxNumFocusAreas() == 0)
                return true;

            // if it does, set our current focus area to our selection rect with a heavy weight (1-1000).
            ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>() {{
                new Camera.Area(selectionRect, 2000);
            }};

            Camera.Parameters focusParams = camera.getParameters();

            focusParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            focusParams.setFocusAreas(focusAreas);

            camera.setParameters(focusParams);

            // after we're focused, stop the preview if we just stopped making the rect
            if(event.getAction() == MotionEvent.ACTION_UP) {
                camera.takePicture(null, null, this);
                camera.stopPreview();
                isPreviewOn = false;
            }
        }

        return true;
    }

    public Rect getSelectionRect() {
        Log.d("ImageToText", String.format("%d,%d,%d,%d", selectionRect.left, selectionRect.top, selectionRect.right, selectionRect.bottom));
        return selectionRect;
    }

    @Override

    public void onPictureTaken(byte[] data, Camera camera) {

        Matrix rotateMatrix = new Matrix();

        Bitmap originalImg = BitmapFactory.decodeByteArray(data, 0, data.length);

        float widthScaleFactor = 1;
        float heightScaleFactor = 1;

        if(originalImg.getHeight() >= originalImg.getWidth()) {
            widthScaleFactor = (float)originalImg.getWidth() / (float)selection.getWidth();
            heightScaleFactor = (float)originalImg.getHeight() / (float)selection.getHeight();
            rotateMatrix.postRotate(0);
        } else {
            widthScaleFactor = (float)originalImg.getHeight() / (float)selection.getWidth();
            heightScaleFactor = (float)originalImg.getWidth() / (float)selection.getHeight();
            rotateMatrix.postRotate(90);
        }

        currentImage = Bitmap.createBitmap(originalImg,
                (int)(selectionRect.left*widthScaleFactor),
                (int)(selectionRect.top*heightScaleFactor),
                (int)(selectionRect.width()*widthScaleFactor),
                (int)(selectionRect.height()*heightScaleFactor),
                rotateMatrix, true);

        captureButton.setEnabled(true);
    }

    @Override
    public void useExtractedText(String text) {
        Log.d("ImageToText", "USED EXTRACTED: " + text);
    }

    // Static method to get our camera, checks if it's in use or otherwise unavailable
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d("ImageToText", "Camera unavailable: is null.");
        }
        return c; // returns null if camera is unavailable
    }
}
