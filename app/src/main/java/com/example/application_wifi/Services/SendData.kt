package com.example.application_wifi.Services


import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.widget.Toast

class SendData : Service() {

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }

        fun sendMessage(msg: Any): Any {

        }
    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }
/*
public class ServiceStatusUpdate extends Service {

@Override
public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    while(true)
    {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            new DoBackgroundTask().execute(Utilities.QUERYstatus);
            e.printStackTrace();
        }
        return START_STICKY;
    }
}

private class DoBackgroundTask extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        String response = "";
        String dataToSend = params[0];
        Log.i("FROM STATS SERVICE DoBackgroundTask", dataToSend);
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(Utilities.AGENT_URL);

        try {
            httpPost.setEntity(new StringEntity(dataToSend, "UTF-8"));

            // Set up the header types needed to properly transfer JSON
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept-Encoding", "application/json");
            httpPost.setHeader("Accept-Language", "en-US");

            // Execute POST
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity responseEntity = httpResponse.getEntity();
            if (responseEntity != null) {
                response = EntityUtils.toString(responseEntity);
            } else {
                response = "{\"NO DATA:\"NO DATA\"}";
            }
        } catch (ClientProtocolException e) {
            response = "{\"ERROR\":" + e.getMessage().toString() + "}";
        } catch (IOException e) {
            response = "{\"ERROR\":" + e.getMessage().toString() + "}";
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        Utilities.STATUS = result;
        Log.i("FROM STATUS SERVICE: STATUS IS:", Utilities.STATUS);
        super.onPostExecute(result);
    }
}
}
        }
    }
*/
}
