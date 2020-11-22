package com.example.application_wifi

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WIFI_STATE_ENABLED
import android.net.wifi.WifiManager.WIFI_STATE_ENABLING
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.work.*
import com.example.application_wifi.Workers.Init_PWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


// https://developer.android.com/guide/topics/sensors
// https://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout
// https://developer.android.com/reference/android/net/wifi/package-summary
// https://developer.android.com/reference/android/net/wifi/rtt/package-summary


//https://stackoverflow.com/questions/8317331/detecting-when-screen-is-locked !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
/*
Aplicaçao corre apenas
quando esta ligado ao Eduroam
quando não se esta a mexer
quando o telemovel esta em Low State em Usage
*/



class Application_Wifi : Application(), SensorEventListener {

    /*
      Excellent >-50 dBm
      Good -50 to -60 dBm
      Fair -60 to -70 dBm
      Weak < -70 dBm
    */
    val DefaultValue = 500000

    private val Chnnl: IntArray = intArrayOf(2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
        2452, 2457, 2462, 2467, 2472, 2484)

    lateinit var wManager: WifiManager
    lateinit var pManager: PowerManager
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)
    var ids = IntArray(0)
    lateinit var appWidgetHost: AppWidgetHost
    val workManager: WorkManager = WorkManager.getInstance(this)
    lateinit var sensorManager: SensorManager
    lateinit var acc_sensor: Sensor
    lateinit var grav_sensor: Sensor
    lateinit var acc_data: SensorData
    lateinit var grav_data: SensorData
    lateinit var target_connection: WifiInfo
    lateinit var wifi_info_results: List<WifiInfo>
    lateinit var my_connection_results: WifiInfo
    var Update_period: Int = 500000


    @RequiresApi(Build.VERSION_CODES.R)
    data class SensorData(
        val x: Float,
        val y: Float,
        val z: Float
    )

    data class WifiInfo(
        val SSID: String,
        val BSSID: String,
        val rssi: Int,
        val frequency: Int
    )

    fun checkNetwork(): Boolean =
        if (WIFI_STATE_ENABLED != wManager.wifiState && WIFI_STATE_ENABLING != wManager.wifiState) {
            Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT).show()
            false
        } else {
            if (!my_connection_results.SSID.contains(target_connection.SSID, ignoreCase = true)) {
                Toast.makeText(
                    applicationContext,
                    "Por Favor Ligar a rede eduroam",
                    Toast.LENGTH_SHORT
                ).show()
                false
            } else true
        }


    fun wifiGetInfo() {
        val wifiList = wManager.scanResults.filter {
            it.SSID.contains(target_connection.SSID, ignoreCase = true) && it.frequency != -1
        }
        val wifiInfo =
            (this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo
        wifi_info_results = wifiList.map {
            WifiInfo(
                it.SSID,
                it.BSSID,
                it.level,
                it.frequency
            )
        }
        if (wifiInfo.frequency != -1) {
            my_connection_results = WifiInfo(
                wifiInfo.ssid,
                wifiInfo.bssid,
                wManager.connectionInfo.rssi,
                wifiInfo.frequency
            )
        } else {
            //Cancel all jobs
            my_connection_results = WifiInfo(
                "N/Connected",
                "0",
                0,
                0
            )
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val connectivityManager =
                context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetInfo = connectivityManager.activeNetworkInfo
            if (activeNetInfo != null
                && activeNetInfo.type == ConnectivityManager.TYPE_WIFI
            ) {
                Toast.makeText(context, "Wifi Connected!", Toast.LENGTH_SHORT).show()
                if (checkNetwork()) {

                } else {
                    //CANCEL ALL JOBS
                }
            } else {
                Toast.makeText(context, "Wifi Not Connected!", Toast.LENGTH_SHORT).show()
                //CANCEL ALL JOBS
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()

        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction("android.net.wifi.STATE_CHANGE")
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        })
        // TARGET CONNECTION CREATION
        target_connection = WifiInfo("eduroam", "MAC", 0, 0)
        //Var INITIALIZATION
        if (!this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        }
        // INITIALIZATION JOB

        val job = GlobalScope.launch(Dispatchers.IO) {
            if (WIFI_STATE_ENABLED != wManager.wifiState) {
                //launch(Dispatchers.Main) { // TODO understand if necessary
                Toast.makeText(applicationContext,
                    "Por Favor Ligar O Wifi",
                    Toast.LENGTH_SHORT).show()
                //}
            } else {
                if (checkNetwork()) {
                    sensorManager.registerListener(this@Application_Wifi,
                        acc_sensor,
                        Update_period)
                    sensorManager.registerListener(this@Application_Wifi,
                        grav_sensor,
                        Update_period)
                    pManager =
                        applicationContext.getSystemService(POWER_SERVICE) as PowerManager
                    appWidgetManager.updateAppWidget(
                        ComponentName(applicationContext, NewAppWidget::class.java),
                        RemoteViews(packageName, R.layout.new_app_widget)
                    )
                    //ids = appWidgetManager.getAppWidgetIds((ComponentName(applicationContext,
                    //    NewAppWidget::javaClass.get(NewAppWidget()))))
                    //acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) -> nao sei se e necessario
                    //grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) -> nao sei se e necessario
                }

            }
        }

        //GlobalScope.launch {}
        val init_constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val WInit = PeriodicWorkRequest.Builder(Init_PWorker, 10, TimeUnit.SECONDS)
            .setConstraints(init_constraints).build()
        workManager.enqueueUniquePeriodicWork("INIT", ExistingPeriodicWorkPolicy.KEEP, WInit)
        // INITIALIZATION JOB
        init()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accUpdate(event)
            Sensor.TYPE_GRAVITY -> gravUpdate(event)
        }
    }

    private fun accUpdate(event: SensorEvent) {
        Log.d("acc", event.toString());
        acc_data = SensorData((event.values[0] + acc_data.x) / 2,
            (event.values[1] + acc_data.y) / 2,
            (event.values[2] + acc_data.z) / 2)
    }

    private fun gravUpdate(event: SensorEvent) {
        Log.d("grav", event.toString());
        grav_data = SensorData((event.values[0] + grav_data.x) / 2,
            (event.values[1] + grav_data.y) / 2,
            (event.values[2] + grav_data.z) / 2)
    }


    override fun onTerminate() {
        super.onTerminate()
        sensorManager.unregisterListener(this)
        workManager.cancelAllWorkByTag("UI")
        TODO("apagar as threads todas")
    }
}

