package com.noahgolmant.ImageToText;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.noahgolmant.ImageToText.camera.CameraActivity;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ITTActivity extends Activity {

    /**
     * Widgets and items interacted with in the main window.
     */
    private Button startNewPhotoButton;
    private Button clearPhotoButton;
    private ListView previousImageListView;

    private ArrayList<Uri> previousImages = new ArrayList<Uri>();
    private ImageAdapter previousImageAdapter;

    /**
     * Listeners to call when a button or object is selected.
     */
    private View.OnClickListener newPhotoButtonListener;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        OpenCVLoader.initDebug();

        fillImageList();
        initializeWidgets();
        initializeListeners();

        checkCameraHardware();
        loadResources();

        //File tessDir = new File(getFilesDir(), "tessdata");
        //tessDir.mkdirs();

        //File tessLang = new File(tessDir, "tesseract-ocr-3.02.eng.tar.gz");
        //new DownloadTask(this, tessLang).execute("https://tesseract-ocr.googlecode.com/files/tesseract-ocr-3.02.eng.tar.gz");

    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a cameram continue
            return true;
        } else {
            // no camera on this device, construct an alert dialog instance to inform the user.
            new AlertDialog.Builder(this)
                    .setTitle("No Camera Detected")
                    .setMessage("ImageToText detected no camera on this device.")
                    .setNeutralButton("OK", new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Disable the photo button
                            startNewPhotoButton.setEnabled(false);
                        }
                    });
            return false;
        }
    }

    /**
     * Initialization methods.
     */

    private void loadResources() {
        try {
            for (String s : getAssets().list("tess")) {
                Log.d("ImageToText", s);
                File res = new File(getFilesDir(), "tessdata" + File.separator + s);
                if(!res.exists())
                    new LoadResourceTask(this).execute(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeWidgets() {
        // Initialize widgets
        startNewPhotoButton = (Button) findViewById(R.id.startNewPhotoButton);
        previousImageListView   = (ListView) findViewById(R.id.listView);

        clearPhotoButton = (Button) findViewById(R.id.clearPhotosButton);

        // Init ListView array adapter that manages the ListView's data
        previousImageAdapter = new ImageAdapter(this, R.layout.image_row_entry, previousImages);
        previousImageListView.setAdapter(previousImageAdapter);
    }

    private void initializeListeners() {
        // Listener called when the "new photo" button is pressed down.
        newPhotoButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuidedPhoto();
            }
        };
        startNewPhotoButton.setOnClickListener(newPhotoButtonListener);

        clearPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearPhotoEntries();
            }
        });

        // Listener called when a previous picture in the picture's ListView is selected.
        // This will go to the rotate activity unless we already have aligned and cropped it.
        previousImageListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                startGuidedPhoto(previousImages.get(position));
            }
        });
    }

    private void clearPhotoEntries() {
        // Local media storage for the specific app
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ImageToText");

        // If we don't have the directory, ain't no images to use.
        // The folder will be created once a new image is taken.
        if(!mediaStorageDir.exists())
            return;

        // Iterate through any files in the folder and add their corresponding URI to our images array.
        for(File image : mediaStorageDir.listFiles()) {
            image.delete();
        }

        previousImages.clear();
    }

    private void startGuidedPhoto() {
        //Intent guideIntent = new Intent(this, GuideActivity.class);
        Intent guideIntent = new Intent();
        guideIntent.setClassName(getApplicationContext(), "com.noahgolmant.ImageToText.camera.CameraActivity");
        super.onResume();
        startActivity(guideIntent);
    }

    private void startGuidedPhoto(Uri image) {
         /*Intent guideIntent = new Intent(this, GuideActivity.class);
        guideIntent.putExtra(MediaStore.EXTRA_OUTPUT, image);

        super.onResume();
        startActivity(guideIntent);*/

        // TEMP
        Intent guideIntent = new Intent();
        guideIntent.setClassName(getApplicationContext(), "com.noahgolmant.ImageToText.camera.CameraActivity");
        super.onResume();
        startActivity(guideIntent);
    }

    /**
     * Populates the ArrayList with all image URIs to fill the ListView
     */
    private void fillImageList() {
        // Start with a fresh new ArrayList to populate
        if(previousImages == null)
            previousImages = new ArrayList<Uri>();
        else
            previousImages.clear();

        // Local media storage for the specific app
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ImageToText");

        // If we don't have the directory, ain't no images to use.
        // The folder will be created once a new image is taken.
        if(!mediaStorageDir.exists())
            return;

        // Iterate through any files in the folder and add their corresponding URI to our images array.
        for(File image : mediaStorageDir.listFiles()) {
            previousImages.add(Uri.fromFile(image));
        }
    }

}