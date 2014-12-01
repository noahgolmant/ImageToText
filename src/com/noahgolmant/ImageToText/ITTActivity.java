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
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ITTActivity extends Activity implements DownloadTask.DownloadInterface {

    /**
     * Widgets and items interacted with in the main window.
     */
    //private Button startNewPhotoButton;
    private String loadedJSON;

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

        initializeWidgets();
        initializeListeners();

        checkCameraHardware();

        loadedJSON = loadJSONFromAsset();

        try {
            JSONObject json = new JSONObject(loadedJSON);
            String urlString  = json.getString("English");

            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1).replace(".gz", "");

            File data = new File(Environment.getExternalStorageDirectory().toString() + "/tessdata/" + fileName);

            if(!data.exists()) {
                DownloadTask initDownload = new DownloadTask(this);
                initDownload.intent = this;
                initDownload.execute("English");
            } else {
                startGuidedPhoto();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset() {
        String json = null;
        try {

            InputStream is = this.getAssets().open("languages.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

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
                            //startNewPhotoButton.setEnabled(false);
                        }
                    });
            return false;
        }
    }

    /**
     * Initialization methods.
     */

    private void initializeWidgets() {
        // Initialize widgets
        //startNewPhotoButton = (Button) findViewById(R.id.startNewPhotoButton);
        //previousImageListView   = (ListView) findViewById(R.id.listView);

        //clearPhotoButton = (Button) findViewById(R.id.clearPhotosButton);

        // Init ListView array adapter that manages the ListView's data
        //previousImageAdapter = new ImageAdapter(this, R.layout.image_row_entry, previousImages);
        //previousImageListView.setAdapter(previousImageAdapter);
    }

    private void initializeListeners() {
        // Listener called when the "new photo" button is pressed down.
        /*newPhotoButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuidedPhoto();
            }
        };
        startNewPhotoButton.setOnClickListener(newPhotoButtonListener);*/

//        clearPhotoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                clearPhotoEntries();
//            }
//        });

        // Listener called when a previous picture in the picture's ListView is selected.
        // This will go to the rotate activity unless we already have aligned and cropped it.
//        previousImageListView.setOnItemClickListener(new ListView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view,
//                                    int position, long id) {
//                startGuidedPhoto(previousImages.get(position));
//            }
//        });
    }

    private void startGuidedPhoto() {
        //Intent guideIntent = new Intent(this, GuideActivity.class);
        Intent guideIntent = new Intent();
        guideIntent.setClassName(getApplicationContext(), "com.noahgolmant.ImageToText.camera.CameraActivity");
        super.onResume();
        startActivity(guideIntent);
    }

    @Override
    public void useDownload() {
        startGuidedPhoto();
    }
}