package com.noahgolmant.ImageToText;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * The connection between the ListView and its ArrayList data
 */
public class ImageAdapter extends ArrayAdapter<Uri> {

    private Context context;
    private ArrayList<Uri> imageUris;

    public ImageAdapter(Context context, int resource, ArrayList<Uri> objects) {
        super(context, resource, objects);
        this.context = context;
        this.imageUris = objects;
    }

    @Override
    // The view here is an individual element in the ListView, i.e. an instance of an image_row_entry.
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // We create a "holder" with primitive info about the view IF it doesn't already exist.
        // This prevents us from directly accessing the XML elements whenever we iterate or check the ListView,
        // which is expensive.
        ViewHolder holder;
        if(convertView == null) {
            convertView  = inflater.inflate(R.layout.image_row_entry, null);
            holder = new ViewHolder();
            holder.imgView = (ImageView) convertView.findViewById(R.id.img);
            holder.uriPath = imageUris.get(position).getPath();
            holder.position = position;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Using an AsyncTask to load the slow images in a background thread
        // calling execute() basically stacks all the images we need to load up as this task takes cares of them one by one
        new AsyncTask<ViewHolder, Void, Bitmap>() {
            private ViewHolder v;

            @Override
            protected Bitmap doInBackground(ViewHolder... params) {
                v = params[0];
                // Apply sampling to limit the image resolution and limit the height
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                options.outHeight = 10;
                return BitmapFactory.decodeFile(v.uriPath, options);
            }

            @Override
            // once doInBackground is done with a param, we use this to actually set the loaded image on the imageView.
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                if (v.position == position) {
                    // If this item hasn't been recycled already, hide the
                    // progress and set and show the image
                    v.imgView.setVisibility(View.VISIBLE);
                    v.imgView.setImageBitmap(result);
                }
            }
        }.execute(holder);

        return convertView;
    }

    // holds basic data about a View to prevent recurring access to XML data via findViewById()
    static class ViewHolder {
        ImageView imgView;
        String uriPath;
        int position;
    }

}
