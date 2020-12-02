package com.example.application_wifi


import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.application_wifi.Application_Wifi
import com.example.application_wifi.NewAppWidget
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class Init_PWorker(appContext: Application_Wifi, workerParams: WorkerParameters) : ListenableWorker() {


    override fun getId(): UUID {
        TODO("Not yet implemented")
    }

    override fun getTags(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun startWork(): ListenableFuture<Result> {
        TODO("Not yet implemented")
    }

    override fun doWork(): Result {
        return try {
            try {

                Log.d("MyWorker", "Run work manager")
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

    fun init(applicationWifi: Application_Wifi): Boolean {
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Init_PWorker> {
        override fun createFromParcel(parcel: Parcel): Init_PWorker {
            return Init_PWorker(parcel)
        }

        override fun newArray(size: Int): Array<Init_PWorker?> {
            return arrayOfNulls(size)
        }
    }
}