package org.traffic.odroid.stats;

import android.app.AlertDialog;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Handler mHandler = new Handler();
    private Handler mPostHandler = new Handler();

    private long mStartRX = 0;
    private long mStartTX = 0;
    final String TAG = MainActivity.class.getSimpleName();

    private final Runnable mPostRunnable = new Runnable() {
        public void run() {
            try {
                getUsingNormalURLConnection("http://www.yahoo.com/", 0xF00090);
                getUsingNormalURLConnection("http://www.hardkernel.com/",0xF00091);
                getUsingNormalURLConnection("http://www.google.com/",0xF00092);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Executors.newSingleThreadScheduledExecutor().schedule(this,
                        500,
                        TimeUnit.MILLISECONDS);
            }
        }
    };


    private final Runnable mRunnable = new Runnable() {
        public void run() {
            TextView RX = (TextView) findViewById(R.id.RX);
            TextView TX = (TextView) findViewById(R.id.TX);
            long rxBytes = TrafficStats.getUidRxBytes(getApplicationInfo().uid);
            RX.setText(Long.toString(rxBytes));
            long txBytes = TrafficStats.getUidTxBytes(getApplicationInfo().uid);
            TX.setText(Long.toString(txBytes));
            mHandler.postDelayed(mRunnable, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartRX = TrafficStats.getTotalRxBytes();
        mStartTX = TrafficStats.getTotalTxBytes();
        if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage("Device does not support traffic stat monitoring.");
            alert.show();
        } else {
            mHandler.postDelayed(mRunnable, 500);
            Executors.newSingleThreadScheduledExecutor().schedule(mPostRunnable,
                    500,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void getUsingNormalURLConnection(String urlText, int threadTag)
            throws IOException {
        URL url = null;
        TrafficStats.setThreadStatsTag(threadTag);
        try {
            url = new URL(urlText);
        } catch (MalformedURLException e1) {
            throw new IOException(e1);
        }
        HttpURLConnection urlConnection = getConnection(url);
        try {
            urlConnection.connect();// fire HTTP request
            int resCode = urlConnection.getResponseCode();

            if (resCode == 200) {
                try {
                    InputStream in = urlConnection.getInputStream();
                } catch (Exception e) {
                   Log.e(TAG,"Error occured " + e);
                }
            }
        } finally {
            urlConnection.disconnect();
            TrafficStats.clearThreadStatsTag();
        }
    }

    /**
     *
     * @param url
     * @return
     * @throws IOException
     */
    public HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection conn = null;
        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30 * 1000);
        conn.setReadTimeout(30 * 1000);
        return conn;
    }


    /**
     * you cannot see anything using  okhttp3
     * @param url
     * @param threadTag
     */
    private void getUsingOkHTTP3(String url, int threadTag) {
        String responseString=null;
        TrafficStats.setThreadStatsTag(threadTag);

        OkHttpClient client = new OkHttpClient
                .Builder()
                .readTimeout(50, TimeUnit.SECONDS)
                .writeTimeout(50,TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                TrafficStats.clearThreadStatsTag();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG,response.body().string());
                TrafficStats.clearThreadStatsTag();

            }
        });
    }
}
