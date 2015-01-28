package com.noahgolmant.ImageToText;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

/**
 * Created by Noah on 8/28/2014.
 */
public class ResultActivity extends Activity {

    //private ImageView resultView;
    private EditText textView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_layout);

        //resultView = (ImageView) findViewById(R.id.resultImageView);
        textView = (EditText) findViewById(R.id.resultTextView);
        //resultView.setImageBitmap((Bitmap) getIntent().getParcelableExtra("image"));

        String text = getIntent().getStringExtra("extracted");



        textView.setText(text);

        textView.selectAll();
        textView.requestFocus();

        ((InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}