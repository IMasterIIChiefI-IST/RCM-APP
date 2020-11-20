package com.example.application_wifi

import android.R
import android.R.style.Widget
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WIFI_STATE_ENABLED
import android.net.wifi.WifiManager.WIFI_STATE_ENABLING
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.work.*
import com.example.application_wifi.Workers.Init_PWorker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


// https://developer.android.com/guide/topics/sensors
// https://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout
// https://developer.android.com/reference/android/net/wifi/package-summary
// https://developer.android.com/reference/android/net/wifi/rtt/package-summary
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
    val appWidgetManager: AppWidgetManager= AppWidgetManager.getInstance(this)
    var ids = IntArray(0)
    lateinit var appWidgetHost : AppWidgetHost
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


    data class SensorData(
        val x: Float,
        val y: Float,
        val z: Float
    ) {
    }

    data class WifiInfo(
        val SSID: String,
        val BSSID: String,
        val rssi: Int,
        val level: Int,
        val Frequency: Int
    ) {
    }

    private fun checkNetwork(): Boolean =
        if (WIFI_STATE_ENABLED != wManager.wifiState && WIFI_STATE_ENABLING != wManager.wifiState) {
            val myToast = Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT)
            myToast.show()
            false
        } else {
            if (!my_connection_results.SSID.contains(target_connection.SSID, ignoreCase = true)) {
                val myToast = Toast.makeText(
                    applicationContext,
                    "Por Favor Ligar a rede eduroam",
                    Toast.LENGTH_SHORT
                )
                myToast.show()
                false
            } else {
                true
            }
        }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun wifiGetInfo() {

        //fazer a media durante X tempo !!!!!!!!!!!

        var wifiList = wManager.scanResults;
        wifiList = wifiList.filter { it.SSID.contains(target_connection.SSID, ignoreCase = true) }
        val wifiInfo =
            (this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo
        wifi_info_results = wifiList.map {
            WifiInfo(
                it.SSID,
                it.BSSID,
                it.level,
                wManager.calculateSignalLevel(it.level),
                it.frequency
            )
        }
        my_connection_results = WifiInfo(wifiInfo.ssid,
            wifiInfo.bssid,
            wManager.connectionInfo.rssi,
            wManager.calculateSignalLevel(wManager.connectionInfo.rssi),
            wifiInfo.frequency)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        // TARGET CONNECTION CREATION
        target_connection = WifiInfo("eduroam", "MAC", 0, 0, 0)
        //Var INITIALIZATION
        if (this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        }
        // INITIALIZATION JOB
        val init_constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .also {
               checkNetwork()
            }
            .build()
        val WInit = PeriodicWorkRequest.Builder(Init_PWorker, 30, TimeUnit.SECONDS).setConstraints(
            init_constraints).build()
        workManager.enqueueUniquePeriodicWork("INIT", ExistingPeriodicWorkPolicy.KEEP, WInit)

        // INITIALIZATION JOB

        init()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {

        //GlobalScope.launch(Dispatchers.Main) { }

        if (checkNetwork()) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                accUpdate(event)
            }
            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                gravUpdate(event)
            }
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

    @RequiresApi(Build.VERSION_CODES.M)
    fun init() {
        if (this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        }
        if (WIFI_STATE_ENABLED != wManager.wifiState && WIFI_STATE_ENABLING != wManager.wifiState) {
            var myToast = Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT)
            myToast.show()
        } else {
            if (checkNetwork()) {
                sensorManager.registerListener(this, acc_sensor, Update_period)
                sensorManager.registerListener(this, grav_sensor, Update_period)
                pManager = this.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
                ids = appWidgetManager.getAppWidgetIds(NewAppWidget)
                appWidgetManager.updateAppWidget(ComponentName(this.packageName,Application_Wifi::Java.Class),)
                AppWidgetHost = AppWidgetHost(this, R.id.APPWIDGET_HOST_ID)
                //acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) -> nao sei se e necessario
                //grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) -> nao sei se e necessario
            } else {
                // COMEÇAR UM WORKER QUE CHECK DOS PAREMETRO DE INICIALIZAÇAO
                GlobalScope.launch {
                    repeat(1000) {
                        delay(1000L)
                        init()
                    }
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        sensorManager.unregisterListener(this)
        workManager.cancelAllWorkByTag("UI")
        TODO("apagar as threads todas")
    }
}

object Check_init {

    fun refreshPeriodicWork(context: Context) {


        //define constraints
        val myConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshCpnWork = PeriodicWorkRequest.Builder(MyWorker::class.java, 10, TimeUnit.SECONDS)
            .setConstraints(myConstraints)
            .addTag("UI")
            .build()


        WorkManager.getInstance(context).enqueueUniquePeriodicWork("CI",
            ExistingPeriodicWorkPolicy.REPLACE, refreshCpnWork)

    }
}

