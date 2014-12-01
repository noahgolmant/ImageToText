package com.noahgolmant.ImageToText;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;

/**
 * Created by noahg_000 on 7/3/2014.
 */
public class DecodeTask extends AsyncTask<Bitmap, Void, String> {

    private Context context;
    private TessBaseAPI baseAPI;
    private ProgressDialog progDialog;

    private static double THRESHOLD_DELTA = 8.0;

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
    protected void onPreExecute() {
        super.onPreExecute();
        progDialog = new ProgressDialog(context);
        progDialog.setMessage("Loading...");
        progDialog.setIndeterminate(false);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setCancelable(true);
        progDialog.show();
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
        //Core.bitwise_not(mat, mat); // invert the colors

        //double thresh = 200, color = 255;
        //Imgproc.threshold(mat, mat, thresh, color, Imgproc.THRESH_BINARY);

        // for every 2x2 square in the matrix, calculate the average intensity value and apply a threshold
        // at around that level (8 less) to the matrix

        // 2x2 works because the .bmp original format significantly reduces the calculated number of rows
        // and columns via its RowSize formula seen at http://en.wikipedia.org/wiki/BMP_file_format#Pixel_storage

        /*for(int row = 0; row < mat.rows(); row += 3) {
            for(int col = 0; col < mat.cols(); col += 3) {
                //Identify the 2x2 region of interest (roi) as a pointer to the submatrix of mat
                Mat roi = mat.submat(row, row + 2, col, col + 2);
                // Calculate the average intensity value of the roi. Scalar.val[] has length of the number of channels
                // in the image. Our image is grayscale, so the channel length is 1 and we can just call val[0]
                // to get its value.
                double averageIntensity = Core.mean(roi).val[0];
                // apply the threshold to this square minus a constant "delta" for leeway
                Imgproc.threshold(roi, roi, averageIntensity - THRESHOLD_DELTA, 255.0, Imgproc.THRESH_BINARY);

            }
        }*/

        //Imgproc.threshold(mat, mat, 120, 255.0, Imgproc.THRESH_BINARY);

        // Divide the image by its morphologically closed counterpart
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(19,19));
//        Mat closed = new Mat();
//
//        Imgproc.morphologyEx(mat, closed, Imgproc.MORPH_CLOSE, kernel);
//        closed.convertTo(closed, CvType.CV_32FC3);
//        mat.convertTo(mat, CvType.CV_32FC3); // divide requires floating-point
//        Core.divide(mat, closed, mat, 1);
//        Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX);
//        mat.convertTo(mat, CvType.CV_8UC1); // convert back to unsigned int

        // Threshold each block (3x3 grid) of the image separately to
        // correct for minor differences in contrast across the image.
        /*int blocks = mat.rows();
        for (int i = 0; i < blocks; i++) {
            for (int j = 0; j < blocks; j++) {
                //Mat block = mat.rowRange(144*i, 144*(i+1)).colRange(144*j, 144*(j+1));
                Mat block = mat.submat((mat.rows() * i) / blocks, (mat.rows() * (i + 1))/blocks, (mat.cols() * j) / blocks, (mat.cols() * (j + 1))/blocks);
                //Imgproc.threshold(block, block, -1, 255, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);
                //Imgproc.adaptiveThreshold(block,block,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,3,5);
                //Imgproc.threshold(block, block, Core.mean(block).val[0], 255, Imgproc.THRESH_BINARY);
                double avgIntensity = Core.mean(block).val[0];
                Imgproc.threshold(block,block, .62*avgIntensity, 255, Imgproc.THRESH_TOZERO);
                Imgproc.threshold(block,block, .88*avgIntensity, 255, Imgproc.THRESH_TRUNC);
            }
        }*/

        double alpha = 1.3;
        double beta = 6.0;
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                double[] val = new double[] { alpha * mat.get(i,j)[0] + beta };

                mat.put(i,j, val);
            }
        }

//        double avgIntensity = Core.mean(mat).val[0];
//        //Imgproc.equalizeHist(mat,mat);
//
//        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
//                new Size(5, 5));
//
//        Imgproc.dilate(mat,mat,element);
//        Imgproc.threshold(mat,mat, .6*avgIntensity, 255, Imgproc.THRESH_TOZERO);
//        Imgproc.threshold(mat,mat, 1.20*avgIntensity, 255, Imgproc.THRESH_TRUNC);
//        Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 5);
//        Log.d("ImageToText", "Mat size: " + mat.cols() + "," + mat.rows());
//        Log.d("ImageToText", "BMP size: " + img.getWidth() + "," + img.getHeight());
        // set the bitmap back to our modified matrix
        Utils.matToBitmap(mat, img);

        // Get working directory.
        //baseAPI.init(context.getFilesDir().toString(), "eng");
        File sdcard = Environment.getExternalStorageDirectory();
        baseAPI.init(sdcard.toString(), "eng");

        Log.d("ImageToText", sdcard.toString());
        baseAPI.setImage(img);

        String text = baseAPI.getUTF8Text();
        baseAPI.end();
        img.recycle();

        return text;

    }

    @Override
    protected void onPostExecute(String result) {
        progDialog.dismiss();
        intent.useExtractedText(result);
    }


}
