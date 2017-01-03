package com.tinydnn.android;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.romainpiel.titanic.library.Titanic;
import com.romainpiel.titanic.library.TitanicTextView;

public class MainActivity extends AppCompatActivity {

    private Titanic mTitanic;
    private TitanicTextView mTitanicTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitanic = new Titanic();
        mTitanicTextView = (TitanicTextView)findViewById(R.id.titanic_tv);
        new AlexNetOperation().execute();

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

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
