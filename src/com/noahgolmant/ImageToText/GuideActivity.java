package com.noahgolmant.ImageToText;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Noah on 6/20/2014.
 */
public class GuideActivity extends Activity implements DecodeTask.DecodeInterface{

    private Button createPhotoButton;
    private Button cropButton;
    private Button analyzeButton;
    private TextView textDisplay;
    private Uri currentImage;

    private ImageView img;

    /**
     * Static codes to create/access images with the android intent system
     */
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE    = 101;
    private static final int ROTATE_IMAGE_ACTIVITY_REQUEST_CODE  = 102;

    private static final int CAPTURE_DONE_FLAG = 0x01;
    private static final int CROP_DONE_FLAG = 0x02;

    private int imageProcessFlag = 0x0000;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_layout);

        createPhotoButton = (Button) findViewById(R.id.photo_button);
        cropButton = (Button) findViewById(R.id.crop_button);
        analyzeButton = (Button) findViewById(R.id.analyze_button);
        textDisplay = (TextView) findViewById(R.id.test_display);

        initializeListeners();

        if(getIntent().hasExtra(MediaStore.EXTRA_OUTPUT)) {
            currentImage = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);
        }
    }

    private void initializeListeners() {
        createPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImageCapture();
            }
        });

        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImageCrop();
            }
        });

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTextExtraction();
            }
        });
    }

    /**
     * Creates the intent to send to the android OS to take a new picture.
     * This lets the user select their camera application and take a photo externally.
     */
    private void startImageCapture() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        currentImage = getOutputImageUri(); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImage); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

        //imageProcessFlag &= CAPTURE_DONE_FLAG;
    }

    private void startImageCrop() {

        if(currentImage == null)// ||
        //        (imageProcessFlag & CAPTURE_DONE_FLAG) != CAPTURE_DONE_FLAG)
            return;

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(currentImage, "image/*");

        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        //indicate output X and Y
        //retrieve data on return
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImage);
        //start the activity - we handle returning in onActivityResult
        startActivityForResult(cropIntent, CROP_IMAGE_ACTIVITY_REQUEST_CODE);

        imageProcessFlag &= CROP_DONE_FLAG;
    }

    private void startTextExtraction() {
        if(currentImage == null)/* ||
                (imageProcessFlag & CAPTURE_DONE_FLAG) != CAPTURE_DONE_FLAG ||
                (imageProcessFlag & CROP_DONE_FLAG) != CROP_DONE_FLAG)*/
            return;

        DecodeTask task = new DecodeTask(this);
        task.intent = this;
        task.execute(currentImage);
    }

    public void useExtractedText(String text) {
        this.textDisplay.setText(text);
        Log.d("ImageToText", "set text to " + text);
    }

    /**
     * Receives the result of the activity's request to take a new picture.
     * @param requestCode int that shows the initial request made by the application.
     * @param resultCode  int that shows the success or failure of the operation.
     * @param data        the data from the initial intent.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_CANCELED && resultCode != RESULT_OK) {
            Toast.makeText(this, "Image creation failed.", Toast.LENGTH_LONG).show();
            Log.d("ImageToText", "failed to create image with code " + requestCode);
            return;
        }

        switch (requestCode) {
            case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image created successfully.", Toast.LENGTH_SHORT).show();
                break;
            case CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image cropped successfully.", Toast.LENGTH_SHORT).show();

                // start image analysis
                break;
            default:
                break;
        }
    }

    /** Create a Uri for saving an image. */
    private static Uri getOutputImageUri(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ImageToText");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("ImageToText", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return Uri.fromFile(mediaFile);
    }
}