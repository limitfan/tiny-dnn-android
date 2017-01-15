package com.tinydnn.android;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.romainpiel.titanic.library.Titanic;
import com.romainpiel.titanic.library.TitanicTextView;
import com.tinydnn.android.util.FingerPaintView;
import com.tinydnn.android.util.PixelGridView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements FingerPaintView.OnDigitListener {

    private Titanic mTitanic;
    private TitanicTextView mTitanicTextView;
    private FingerPaintView fingerPaintView;
    private Button mClear;
    private TextView mDigitText;

    private static String TAG = "PermissionReadStorage";
    private static final int REQUEST_WRITE_STORAGE = 112;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fingerPaintView = (FingerPaintView) findViewById(R.id.drawView);
        fingerPaintView.setDigitListener(this);
        mTitanic = new Titanic();
        mTitanicTextView = (TitanicTextView)findViewById(R.id.titanic_tv);
        mClear = (Button)findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fingerPaintView.clear();
            }
        });
        mDigitText = (TextView)(findViewById(R.id.recoDigit));

        loadModel(getAssets(), "LeNet-model");
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkFirstRun();
    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }

    private void checkFirstRun() {

        final String PREFS_NAME = "MyPrefsFile";
        final String PREF_VERSION_CODE_KEY = "version_code";
        final int DOESNT_EXIST = -1;

        // Get current version code
        int currentVersionCode = 0;
        try {
            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            // handle exception
            e.printStackTrace();
            return;
        }

        // Get saved version code
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);

        // Check for first run or upgrade
        if (currentVersionCode == savedVersionCode) {

            // This is just a normal run
            fingerPaintView.setVisibility(View.VISIBLE);
            mClear.setVisibility(View.VISIBLE);
            mDigitText.setVisibility(View.VISIBLE);
            return;

        } else if (savedVersionCode == DOESNT_EXIST) {
            new AlexNetOperation().execute();

        } else if (currentVersionCode > savedVersionCode) {

        }

        // Update the shared preferences with the current version code
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).commit();

    }

    class ValueScore{
        int value;
        double score;
        public ValueScore(int v, double s){
            value = v;
            score = s;
        }
    }

    @Override
    public void ondigit() {
       new DigitRecognitionOperation().execute();
    }

    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");

                } else {

                    Log.i(TAG, "Permission has been granted by user");

                }
                return;
            }
        }
    }

    private class AlexNetOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            mTitanic.start(mTitanicTextView);
            String alexnetbenchmark = stringFromJNI();
            return alexnetbenchmark;
        }

        @Override
        protected void onPostExecute(String result) {
            final TextView tv = (TextView) findViewById(R.id.benchmark);
            mTitanic.cancel();
            tv.setText(result);
            fingerPaintView.setVisibility(View.VISIBLE);
            mClear.setVisibility(View.VISIBLE);
            mDigitText.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class DigitRecognitionOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Bitmap capturedBitmap = getBitmapFromView(fingerPaintView);
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOutputStream = null;
            File file = new File("/sdcard/data" + "/", "digit.jpg");
            try {
                fOutputStream = new FileOutputStream(file);


                int capturelen = 200;
                Bitmap resized = Bitmap.createScaledBitmap(capturedBitmap, capturelen, capturelen, true);

                int thresh = 128;
                int w = resized.getWidth();
                int h = resized.getHeight();
                int minx = Integer.MAX_VALUE;
                int maxx = Integer.MIN_VALUE;
                int miny = Integer.MAX_VALUE;
                int maxy = Integer.MIN_VALUE;
                for(int y = 0; y < h; ++y)
                    for(int x = 0; x < w; ++x){
                        int pixel = resized.getPixel(x, y);
                        int r, g, b;
                        r = Color.red(pixel);
                        g = Color.green(pixel);
                        b = Color.blue(pixel);
                        int level = (r+g+b)/3;
                        //find black ones
                        if(level<thresh){
                            if (x<minx)
                                minx = x;
                            if (x>maxx)
                                maxx = x;
                            if (y<miny)
                                miny = y;
                            if (y>maxy)
                                maxy = y;
                        }
                    }

                int xlen = maxx-minx;
                int ylen = maxy-miny;

                int startx, starty;
                int len;
                if(xlen>ylen){
                    startx = minx;
                    starty = miny - (xlen-ylen)/2;
                    len = xlen;
                }
                else{
                    startx = minx -(ylen-xlen)/2;
                    starty = miny;
                    len = ylen;
                }
                int padding = (int)(len*0.2);
                startx -= padding;
                starty -= padding;
                startx = Math.max(startx, 0);
                starty = Math.max(starty, 0);
                int wid = Math.min(capturelen-startx, len+2*padding);
                int heig = Math.min(capturelen-starty, len+2*padding);
                Bitmap cropped = Bitmap.createBitmap(resized, startx,starty, wid, heig);
                cropped.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream);


                fOutputStream.flush();
                fOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "";
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }

            float[] t = new float[1024];
            float[] recoRes = recognize(t);

            List<ValueScore> arr=  new ArrayList<ValueScore>(10);
            for(int i = 0; i< recoRes.length; ++i){
                arr.add(new ValueScore(i, recoRes[i]));
            }
            Collections.sort(arr, new Comparator<ValueScore>(){

                @Override
                public int compare(ValueScore lhs, ValueScore rhs) {
                    return -Double.compare(lhs.score, rhs.score);
                }
            });

            String text = "";
            for(int i = 0;i<3;++i){
                text+=arr.get(i).value+" "+arr.get(i).score+"\n";
            }
            return text;

        }

        @Override
        protected void onPostExecute(String result) {
            mDigitText.setText(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native float[] recognize(float[] imagedata);

    public native void loadModel(AssetManager ass, String filename);


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
