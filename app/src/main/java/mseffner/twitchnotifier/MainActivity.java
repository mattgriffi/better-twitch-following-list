package mseffner.twitchnotifier;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;

import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity {

    private TextView testTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testTextView = (TextView) findViewById(R.id.test);

        new TestAsyncTask().execute();
    }

    private class TestAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {

            String apiResponse = "";

            try {
                apiResponse = NetworkUtils.makeHttpRequest();
            } catch (IOException e) {
                Log.e("TestAsyncTask", e.toString());
            }

            return apiResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            testTextView.setText(s);
        }
    }

}
