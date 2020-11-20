package com.example.application_wifi.Workers

import android.R
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.application_wifi.Application_Wifi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class Init_PWorker(appContext: Application_Wifi, workerParams: WorkerParameters):Worker , PeriodicWorkRequest(appContext: Application_Wifi, workerParams: WorkerParameters) {

        override fun doWork(): Result {
        return try {
            try {
                Log.d("MyWorker", "Run work manager")
                //Do Your task here
                init()
                Result.success()
            } catch (e: Exception) {
                Log.d("MyWorker", "exception in doWork ${e.message}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.d("MyWorker", "exception in doWork ${e.message}")
            Result.failure()
        }
    }

    fun init() {
        if (this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(Application.WIFI_SERVICE) as WifiManager
        }
        if (WifiManager.WIFI_STATE_ENABLED != wManager.wifiState && WifiManager.WIFI_STATE_ENABLING != wManager.wifiState) {
            var myToast = Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT)
            myToast.show()
        } else {
            if (checkNetwork()) {
                sensorManager.registerListener(this, acc_sensor, Update_period)
                sensorManager.registerListener(this, grav_sensor, Update_period)
                pManager = this.applicationContext.getSystemService(Application.POWER_SERVICE) as PowerManager
                mAppWidgetManager = AppWidgetManager.getInstance(this)
                mAppWidgetHost = AppWidgetHost(this, R.id.APPWIDGET_HOST_ID)
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

}