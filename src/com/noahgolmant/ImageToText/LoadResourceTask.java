package com.noahgolmant.ImageToText;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import java.io.*;

/**
 * Created by noahg_000 on 7/1/2014.
 */
public class LoadResourceTask extends AsyncTask<String, Void, Void> {

    private Context context;

    LoadResourceTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... params) {

        File tessPath = new File(context.getFilesDir(), "tessdata");
        if(!tessPath.exists() && !tessPath.mkdir())
            Log.d("ImageToText", "Failed to access tessdata directory.");


        try {
            File outFile = new File(tessPath, params[0]);
            if(!outFile.exists())
                outFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(outFile, false);
            InputStream input = context.getAssets().open("tess/" + params[0]);

            Log.d("ImageToText", "Output: " + outFile.getName() + " / Input: "  + input.toString());


            byte[] buffer = new byte[4096];
            int bytesRead;

            while((bytesRead = input.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
