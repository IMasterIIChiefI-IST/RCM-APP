package com.example.application_wifi


import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.application_wifi.Application_Wifi
import com.example.application_wifi.NewAppWidget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class Init_PWorker(appContext: Application_Wifi, workerParams: WorkerParameters) : Worker(appContext, workerParams),
    PeriodicWorkRequest(appContext, workerParams) {

    override fun getId(): UUID {
        TODO("Not yet implemented")
    }

    override fun getTags(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun doWork(): Result {
        return try {
            try {
                Log.d("MyWorker", "Run work manager")
                val context = applicationContext
                init(applicationWifi)
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

    fun init(applicationWifi: Application_Wifi): Boolean {
        applicationWifi.isinitialized
        applicationWifi.wManager =
            applicationContext.getSystemService(Application.WIFI_SERVICE) as WifiManager
        if (WifiManager.WIFI_STATE_ENABLED != wManager.wifiState) {
            var myToast = Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT)
            myToast.show()
        } else {
            if (checkNetwork()) {
                sensorManager.registerListener(this, acc_sensor, Update_period)
                sensorManager.registerListener(this, grav_sensor, Update_period)
                pManager =
                    this.applicationContext.getSystemService(Application.POWER_SERVICE) as PowerManager
                appWidgetManager.updateAppWidget(ComponentName(applicationContext,
                    NewAppWidget::javaClass.get(NewAppWidget())),
                    RemoteViews(this.packageName, R.layout.new_app_widget))
                //ids = appWidgetManager.getAppWidgetIds((ComponentName(applicationContext,
                //    NewAppWidget::javaClass.get(NewAppWidget()))))
                //acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) -> nao sei se e necessario
                //grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) -> nao sei se e necessario
                return true
            } else {
                return false
            }
        }
    }
}