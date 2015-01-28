package com.noahgolmant.ImageToText.camera;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.Toast;
import com.googlecode.leptonica.android.JpegIO;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.noahgolmant.ImageToText.DecodeTask;
import com.noahgolmant.ImageToText.R;
import com.noahgolmant.ImageToText.ResultActivity;
import com.noahgolmant.ImageToText.TranslateTask;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Noah on 7/5/2014.
 */
public class CameraActivity extends Activity implements View.OnClickListener, View.OnTouchListener, Camera.PictureCallback, DecodeTask.DecodeInterface, TranslateTask.TranslateInterface {

    private static int MIN_SELECTION_AREA = 400;

    private Camera camera;
    private CameraPreview preview;
    private SelectionView selection;
    private Button captureButton;
    private FrameLayout previewFrame;

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
        previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
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

        captureButton.setEnabled(true);

    }

    @Override
    public void onClick(View v) {

       camera.takePicture(null, null, this);
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

            //if(!isPreviewOn && (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN)) {
            //    camera.startPreview();
            //    isPreviewOn = true;
            //}

            selection.redraw(); //re-draws the canvas with the rect

            // check if the hardware comes with focus capabilities
            if(camera.getParameters().getMaxNumFocusAreas() == 0)
                return true;

            // if it does, set our current focus area to our selection rect with a heavy weight (1-1000).
            ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
           // focusAreas.add(new Camera.Area(getSelectionFocusRect(), 1000));

            Camera.Parameters focusParams = camera.getParameters();

            focusParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            //focusParams.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            focusParams.setFocusAreas(focusAreas);


            Camera.Size maxSize = camera.getParameters().getPictureSize();
            for(Camera.Size s : camera.getParameters().getSupportedPictureSizes()) {
                if(s.height * s.width >= maxSize.height * maxSize.width)
                    maxSize = s;
            }
            focusParams.setPictureSize(maxSize.width,maxSize.height);
            camera.setParameters(focusParams);
        }

        return true;
    }

    // normalize the selectionRect values and convert to new 2D grid where (0,0) is center and (-1000, -1000) is top left
    // normalize: abs( (value-min) / (max - min) )
    // then multiply this percentage by abs(max - min) + min to move into the new coordinate system
    // in this case abs(max - min) + min is abs(1000 - -1000) + -1000 = 1000
    private Rect getSelectionFocusRect() {

        Rect retRect = new Rect(-50, -50, 50, 50);

        if(selectionRect.width() * selectionRect.height() >= MIN_SELECTION_AREA) {

            int focusLeft = Float.valueOf(((float)selectionRect.left/ previewFrame.getWidth()) * 2000 - 1000).intValue();
            int focusTop  = Float.valueOf(((float)selectionRect.top/ previewFrame.getHeight()) * 2000 - 1000).intValue();
            int focusRight = Float.valueOf(((float)selectionRect.right/ previewFrame.getWidth()) * 2000 - 1000).intValue();
            int focusBottom  = Float.valueOf(((float)selectionRect.bottom/ previewFrame.getHeight()) * 2000 - 1000).intValue();

            retRect.set(focusLeft, focusTop, focusRight, focusBottom);
        }

        Log.d("ImageToText", String.format("%d,%d,%d,%d", retRect.left, retRect.top, retRect.right, retRect.bottom));

        return retRect;
    }

    public Rect getSelectionRect() {
        //Log.d("ImageToText", String.format("%d,%d,%d,%d", selectionRect.left, selectionRect.top, selectionRect.right, selectionRect.bottom));
        return selectionRect;
    }

    @Override

    public void onPictureTaken(byte[] data, Camera camera) {

        camera.stopPreview();
        isPreviewOn = false;

        Matrix rotateMatrix = new Matrix();

        Bitmap imgData = BitmapFactory.decodeByteArray(data, 0, data.length);
        rotateMatrix.postRotate(90);
        Bitmap originalImg = Bitmap.createBitmap(imgData, 0, 0, imgData.getWidth(), imgData.getHeight(), rotateMatrix, true);

        float widthScaleFactor = 1;
        float heightScaleFactor = 1;

        widthScaleFactor = (float)originalImg.getWidth() / (float)selection.getWidth();
        heightScaleFactor = (float)originalImg.getHeight() / (float)selection.getHeight();
        rotateMatrix.postRotate(-90);

        currentImage = Bitmap.createBitmap(originalImg,
                getConvertedCoord(selectionRect.left, previewFrame.getWidth(), originalImg.getWidth()),
                getConvertedCoord(selectionRect.top, previewFrame.getHeight(), originalImg.getHeight()),
                getConvertedCoord(selectionRect.width(), previewFrame.getWidth(), originalImg.getWidth()),
                getConvertedCoord(selectionRect.height(), previewFrame.getHeight(), originalImg.getHeight()),
                rotateMatrix, true);

        Log.d("ImageToText", String.format("Left: %d, Top: %d, Width: %d, Height: %d, previewFrame width: %d, previewFrame height: %d, originalImg width: %d, originialImg height: %d",
                selectionRect.left, selectionRect.top, selectionRect.width(), selectionRect.height(), previewFrame.getWidth(), previewFrame.getHeight(),
                originalImg.getWidth(), originalImg.getHeight()));

        originalImg.recycle();

        DecodeTask decoder = new DecodeTask(this, (Language)getIntent().getSerializableExtra("from"));
        decoder.intent = this;
        decoder.execute(currentImage);

        captureButton.setEnabled(false);

    }


    public String extractedText = null;

    // normalizes the selection coord and then converts it to the bitmap size
    private int getConvertedCoord(int selectionCoord, int selectionMax, int conversionMax) {
        return Float.valueOf(((float)selectionCoord) / selectionMax * conversionMax ).intValue();
    }

    @Override
    public void useExtractedText(String text) {

        this.extractedText = text;

        //Translate.setClientId("+iMJBJCzSshvoA0riBfTNo5GM5ypLnCNJrA3YzZp0fQ");
        //Translate.setClientSecret("jhfNdRP3dcmlzHcQ0VG+fXmMB3af0DjxyxFEG41UtsQ=");

        Language from = (Language)getIntent().getSerializableExtra("from");
        Language to = (Language)getIntent().getSerializableExtra("to");

        TranslateTask trans = new TranslateTask(this, from.name(), to.name());
        trans.intent = this;
        trans.execute(text);

        /*String translatedText = text;

        try {
            //translatedText = Translate.execute(text, from, to);
            translatedText = translate(from.name(), to.name(), text);

            resultIntent.putExtra("extracted", translatedText);
            this.startActivity(resultIntent);

            Log.d("ImageToText", "USED EXTRACTED: " + text);
            Log.d("ImageToText", "USED TRANSLATED: " + translatedText);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to translate text.", Toast.LENGTH_LONG);
            resultIntent.putExtra("extracted", translatedText);
            this.startActivity(resultIntent);
        }*/

    }

    @Override
    public void useTranslation(String text) {
        Intent resultIntent = new Intent(this, ResultActivity.class);
        resultIntent.putExtra("extracted", text);
        this.startActivity(resultIntent);

        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }


        this.finish();
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
