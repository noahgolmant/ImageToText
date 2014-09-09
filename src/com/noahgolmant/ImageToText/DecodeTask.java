package com.noahgolmant.ImageToText;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * Created by noahg_000 on 7/3/2014.
 */
public class DecodeTask extends AsyncTask<Bitmap, Void, String> {

    private Context context;
    private TessBaseAPI baseAPI;

    // interface to interact with main intent
    public interface DecodeInterface {
        void useExtractedText(String text);
    }

    public DecodeInterface intent = null;

    public DecodeTask(Context context) {
        this.context = context;
        this.baseAPI = new TessBaseAPI();
    }

    @Override
    protected String doInBackground(Bitmap... params) {

        // set the bmp to the correct type
        Bitmap img = params[0].copy(Bitmap.Config.ARGB_8888, true);

        // begin image pre-processing by applying threshold and inverting the colors
        Mat mat = new Mat();
        Utils.bitmapToMat(img, mat);
        //Mat mat = Highgui.imread(params[0].getPath());

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);

        Core.bitwise_not(mat, mat); // invert the colors

        double thresh = 200, color = 255;
        Imgproc.threshold(mat, mat, thresh, color, Imgproc.THRESH_BINARY);

        //Highgui.imwrite(params[0].getPath(), mat);
        //Bitmap img = BitmapFactory.decodeFile(params[0].getPath()).copy(Bitmap.Config.ARGB_8888, true);

        // set the bitmap back to our modified matrix
        Utils.matToBitmap(mat, img);

        // Get working directory.
        baseAPI.init(context.getFilesDir().toString(), "eng");
        baseAPI.setImage(img);

        String text = baseAPI.getUTF8Text();
        baseAPI.end();

        return text;

    }

    @Override
    protected void onPostExecute(String result) {
        intent.useExtractedText(result);
    }


}
