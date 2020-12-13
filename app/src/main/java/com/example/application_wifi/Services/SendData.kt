package com.example.application_wifi.Services



import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import kotlinx.coroutines.*
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL


//https://stackoverflow.com/questions/3505930/make-an-http-request-with-android
/*
Service.START_STICKY
Service is restarted if it gets terminated. Intent data passed to the onStartCommand method is null. Used for services which manages their own state and do not depend on the Intent data.


Service.START_NOT_STICKY
Service is not restarted. Used for services which are periodically triggered anyway. The service is only restarted if the runtime has pending startService() calls since the service termination.


Service.START_REDELIVER_INTENT
Similar to Service.START_STICKY but the original Intent is re-delivered to the onStartCommand method.


https://developer.android.com/reference/android/app/job/JobService
 */
class SendData : Service() {

    override fun onBind(intent: Intent) = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService() = this@SendData
    }

    val job_Scope = CoroutineScope(SupervisorJob())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var jsonstring = intent.extras as String
        job_Scope.launch(Dispatchers.Unconfined) {
            try {
                var task = DoBackgroundTask()
                var response = task.execute(jsonstring);
                delay(10000);
            } catch (e: Exception) {
                // Handle exception
            }
        }
        return START_NOT_STICKY ;
    }

    //android.os.AsyncTask<Params, Progress, Result>
    private class DoBackgroundTask : AsyncTask<String, String, String>() {

        override fun doInBackground(vararg params: String?): String? {
            var response = "";
            var dataToSend = params[0] as String;

            try {
                val url = URL("asd")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                val os = DataOutputStream(conn.getOutputStream());
                os.writeBytes(dataToSend);

                os.flush();
                os.close();

                response =  conn.getResponseCode().toString()
                Log.i("STATUS", response);
                conn.disconnect();
            } catch (e: Exception) {
                e.printStackTrace();
            }
            return response ;
        }

    }

}
