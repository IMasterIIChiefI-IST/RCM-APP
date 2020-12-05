package com.example.application_wifi

import android.app.Application
import android.app.KeyguardManager
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
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.collection.CircularArray
import androidx.work.WorkManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.sqrt


// https://developer.android.com/guide/topics/sensors
// https://developer.android.com/guide/topics/appwidgets/index.html#CreatingLayout
// https://developer.android.com/reference/android/net/wifi/package-summary
// https://developer.android.com/reference/android/net/wifi/rtt/package-summary
// https://stackoverflow.com/questions/8317331/detecting-when-screen-is-locked !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// https://stackoverflow.com/questions/11688689/save-variables-after-quitting-application
// https://stackoverflow.com/questions/47871868/what-does-the-suspend-function-mean-in-a-kotlin-coroutine
// https://developer.android.com/guide/components/services
/*
Aplicaçao corre apenas
quando esta ligado ao Eduroam
quando não se esta a mexer
quando o telemovel esta em Low State em Usage aka locked
*/

class Application_Wifi : Application(), SensorEventListener {

    private val TIMESONSORREAD = 2
    private val MAXTRY = 10
    private val DATASIZE = 8 //(2^3)
/*
    class Event<T> {
        private val handlers = arrayListOf<(Event<T>.(T) -> Unit)>()
        operator fun plusAssign(handler: Event<T>.(T) -> Unit) {
            handlers.add(handler)
        }

        operator fun invoke(value: T) {
            for (handler in handlers) handler(value)
        }

        private var singletonDuck = Duck();

        class Duck {
            val onTalk = Event<String>()

            fun poke() {
                onTalk("Quaak!")
            }
        }

        public fun subcribeToDuck(lmbd: Event<String>.(String) -> Unit) {
            singletonDuck.onTalk += lmbd;

            singletonDuck.onTalk("")
            singletonDuck.poke()
        }
    }
*/

    /*
      Excellent >-50 dBm
      Good -50 to -60 dBm
      Fair -60 to -70 dBm
      Weak < -70 dBm
    */

    val DefaultValue = 500000
    var machine_State = 0
    var failedAttempts = 0
    var Update_period: Int = 500000

    lateinit var pManager: PowerManager
    var keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

    val workManager: WorkManager = WorkManager.getInstance(this)
    val job_Scope = CoroutineScope(SupervisorJob())

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)
    var ids = IntArray(0)
    lateinit var appWidgetHost: AppWidgetHost

    lateinit var sensorManager: SensorManager
    lateinit var acc_sensor: Sensor
    lateinit var grav_sensor: Sensor
    lateinit var accData: SensorData
    lateinit var gravData: SensorData
    private val accDataLock = ReentrantLock()
    private val gravDataLock = ReentrantLock()

    lateinit var wManager: WifiManager
    lateinit var target_connection: WifiData
    lateinit var otherConnectionResults: List<WifiData>
    lateinit var myConnectionResults: WifiData
    private val myConnectionLock = ReentrantLock()
    private val otherConnectionLock = ReentrantLock()

    var dataReports = CircularArray<List<WifiData>>(DATASIZE)
    private val dataReportsLock = ReentrantLock()

    fun DataToJson(data: List<WifiData>): String {

        var mainJson = JSONObject();
        var dataJson = JSONArray();
        try {
            mainJson.put("size", data.size);
            data.forEach() {
                dataJson.put(it.ToJson());
            }
            mainJson.put("data", dataJson);
            return mainJson.toString();

        } catch (e:JSONException) {
            throw e
        }

    }

    data class SensorData(
        var x: Float,
        var y: Float,
        var z: Float
    ) {
        operator fun plusAssign(add: SensorData) {
            this.x = (this.x + add.x)
            this.y = (this.y + add.y)
            this.z = (this.z + add.z)
        }

        fun mean(counter: Int) {
            this.x = this.x / counter
            this.y = this.y / counter
            this.z = this.z / counter
        }
    }

    data class WifiData(
        val SSID: String,
        val BSSID: String,
        val rssi: Int,
        val frequency: Int,
        val connected: Boolean
    ) {
        fun ToJson(data: JSONObject): JSONObject {
            try {
                data.put("SSID", SSID);
                data.put("BSSID", BSSID);
                data.put("rssi", rssi);
                data.put("frequency", frequency);
                data.put("connected", connected);
                return data

            } catch (e:JSONException) {
                throw e
            }
        }

        fun ToJson(): JSONObject {
            val dataJson = JSONObject();
            try {
                dataJson.put("SSID", SSID);
                dataJson.put("BSSID", BSSID);
                dataJson.put("rssi", rssi);
                dataJson.put("frequency", frequency);
                dataJson.put("connected", connected);
                return dataJson

            } catch (e:JSONException) {
                throw e
            }
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
                if (updateMyConnection()) {
                    //ndad

                } else {
                    //depende onde estou
                    //CANCEL ALL JOBS
                    //un register sensores
                    //unbind
                    //INIT
                }
            } else {
                Toast.makeText(context, "Wifi Not Connected!", Toast.LENGTH_SHORT).show()
                //CANCEL ALL JOBS
                //un register sensores
                //unbind
                //INIT
            }
        }

    }


    override fun onCreate() {
        super.onCreate()
        /*
        val init_constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val WInit = PeriodicWorkRequest.Builder(PWorker::class.java, 10, TimeUnit.SECONDS)
            .setConstraints(init_constraints)
        val data = Data.Builder()
        data.put("application_context", this)
        val WInit_wk = WInit.setInputData(data.build()).build()
        workManager.enqueueUniquePeriodicWork("Program", ExistingPeriodicWorkPolicy.KEEP, WInit_wk)

        val job_Scope = CoroutineScope(SupervisorJob())

        job_Scope.launch {
            try {
                init(this@Application_Wifi)
            } catch (e: Exception) {
                val myToast = Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT)
                myToast.show()
            }
        }
        */

        machine_State = 0
        failedAttempts = 0

        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction("android.net.wifi.STATE_CHANGE")
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        })

        // TARGET CONNECTION CREATION
        target_connection = WifiData("eduroam", "MAC", 0, 0, true)

        //INIT
        if (!this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        }

        job_Scope.launch(Dispatchers.Unconfined) {
            try {
                while (failedAttempts < MAXTRY) {
                    when (machine_State) {
                        0 -> {
                            if (init()) {
                                machine_State++
                                failedAttempts = 0
                            } else {
                                failedAttempts++
                            }
                        }
                        1 -> {
                            if (updateMyConnection()) {
                                if (isNotMoving() && keyguardManager.isKeyguardLocked) {
                                    updateOtherConnection()
                                    try {
                                        dataReportsLock.lock()
                                        if (dataReports.size() >= DATASIZE) {
                                            dataReports.popLast()
                                        }
                                        if (getOtherConnection().isNullOrEmpty()) {
                                            dataReports.addFirst(listOf(getMyConnection()))
                                        } else {
                                            dataReports.addFirst(getOtherConnection()?.plusElement(
                                                getMyConnection()))
                                        }
                                    } finally {
                                        dataReportsLock.unlock()
                                    }
                                    failedAttempts = 0
                                    //bind service
                                }
                            } else {
                                failedAttempts++
                            }
                        }
                        -1 -> {
                            machine_State = 1
                            failedAttempts = 0
                            sensorManager.unregisterListener(this@Application_Wifi)
                            //unbind service
                        }
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                // Handle exception
                onTerminate()
            }
        }

// INITIALIZATION JOB
/*
val task = Timer.scheduleAtFixedRate(task, after, interval)

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
            cancel("completed")
            //ids = appWidgetManager.getAppWidgetIds((ComponentName(applicationContext,
            //    NewAppWidget::javaClass.get(NewAppWidget()))))
            //acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) -> nao sei se e necessario
            //grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) -> nao sei se e necessario
        }
    }
}

val init_constraints = Constraints.Builder()
    .setRequiresBatteryNotLow(true)
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
val WInit = PeriodicWorkRequest.Builder(Init_PWorker::class.java, 10, TimeUnit.SECONDS)
    .setConstraints(init_constraints)
val data = Data.Builder()
data.put("application_context",this)
val WInit_wk = WInit.setInputData(data.build()).build()
workManager.enqueueUniquePeriodicWork("INIT", ExistingPeriodicWorkPolicy.KEEP, WInit_wk)
*/

        onTerminate()
    }

    private fun isNotMoving(): Boolean {
        var acc = getAcc()
        var grav = getGrav()
        var vectorAcc = 0.0f
        var vectorGrav = 0.0f
        var counter = 1
        object : CountDownTimer(TIMESONSORREAD.toLong(), 250) {
            override fun onTick(millisUntilFinished: Long) {
                acc += getAcc()
                grav += getGrav()
                counter += 1
            }

            override fun onFinish() {
                acc.mean(counter)
                grav.mean(counter)
                vectorAcc = sqrt((acc.x * acc.x) + (acc.y * acc.y) + (acc.z * acc.z))
                vectorGrav = sqrt((grav.x * grav.x) + (grav.y * grav.y) + (grav.z * grav.z))
            }
        }.start()
        return (((vectorAcc * 1.2) > vectorGrav) && ((vectorAcc * 0.8) < vectorGrav))
    }


    private suspend fun init(): Boolean {
        for (i in 1..MAXTRY) {
            if (!this::wManager.isInitialized) {
                wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            }
            if (WifiManager.WIFI_STATE_ENABLED != wManager.wifiState) {
                var myToast = Toast.makeText(applicationContext,
                    "Por Favor Ligar O Wifi :Try".plus(i.toString()),
                    Toast.LENGTH_SHORT)
                myToast.show()
                delay(1000L)
            } else {
                if (updateMyConnection()) {
                    if (!this::sensorManager.isInitialized) {
                        sensorManager =
                            this.applicationContext.getSystemService(SENSOR_SERVICE) as SensorManager;
                    }
                    this.sensorManager.registerListener(this, acc_sensor, Update_period)
                    this.sensorManager.registerListener(this, grav_sensor, Update_period)
                    if (!this::pManager.isInitialized) {
                        pManager =
                            this.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
                    }
                    appWidgetManager.updateAppWidget(ComponentName(applicationContext,
                        NewAppWidget::javaClass.get(NewAppWidget())),
                        RemoteViews(this.packageName, R.layout.new_app_widget))
                    /*
                    ids = appWidgetManager.getAppWidgetIds((ComponentName(applicationContext,NewAppWidget::javaClass.get(NewAppWidget()))))
                    acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) -> nao sei se e necessario
                    grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) -> nao sei se e necessario
                    */
                    return true

                } else {
                    delay(1000L)
                }
            }
        }
        return false
    }

    fun updateMyConnection(): Boolean {
        if (WIFI_STATE_ENABLED != wManager.wifiState && WIFI_STATE_ENABLING != wManager.wifiState) {
            Toast.makeText(applicationContext,
                "Por Favor Ligar O Wifi",
                Toast.LENGTH_SHORT).show()
            return false
        } else {
            val wifiInfo =
                (this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo
            try {
                if (wifiInfo.frequency != -1) {
                    myConnectionLock.lock()
                    myConnectionResults = WifiData(
                        wifiInfo.ssid,
                        wifiInfo.bssid,
                        wManager.connectionInfo.rssi,
                        wifiInfo.frequency,
                        true
                    )
                    if (!myConnectionResults.SSID.contains(target_connection.SSID,
                            ignoreCase = true)
                    ) {
                        Toast.makeText(
                            applicationContext,
                            "Por Favor Ligar a rede eduroam",
                            Toast.LENGTH_SHORT
                        ).show()
                        return true
                    } else
                        return false
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Por Favor Ligar a rede internet",
                        Toast.LENGTH_SHORT
                    ).show()
                    myConnectionLock.lock()
                    myConnectionResults = WifiData(
                        "N/Connected",
                        "0",
                        0,
                        -1,
                        false
                    )
                    return false
                }
            } finally {
                myConnectionLock.unlock()
            }
        }
    }

    private fun updateOtherConnection() {
        val wifiList = wManager.scanResults.filter {
            it.SSID.contains(target_connection.SSID, ignoreCase = true)
        }
        try {
            otherConnectionLock.lock()
            otherConnectionResults = wifiList.map {
                WifiData(
                    it.SSID,
                    it.BSSID,
                    it.level,
                    it.frequency,
                    false
                )
            }
        } finally {
            otherConnectionLock.unlock()
        }
    }

    fun getOtherConnection(): List<WifiData>? {
        try {
            otherConnectionLock.lock()
            return otherConnectionResults
        } finally {
            otherConnectionLock.unlock()
        }
    }

    fun getMyConnection(): WifiData {
        try {
            myConnectionLock.lock()
            return myConnectionResults
        } finally {
            myConnectionLock.unlock()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accUpdate(event)
            Sensor.TYPE_GRAVITY -> gravUpdate(event)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun accUpdate(event: SensorEvent) {
        Log.d("acc", event.toString());
        try {
            accDataLock.lock()
            accData = SensorData(
                (event.values[0] + accData.x) / 2,
                (event.values[1] + accData.y) / 2,
                (event.values[2] + accData.z) / 2)
        } finally {
            accDataLock.unlock()
        }
    }

    private fun getAcc(): SensorData {
        try {
            accDataLock.lock()
            return accData
        } finally {
            accDataLock.unlock()
        }
    }

    private fun getGrav(): SensorData {
        try {
            accDataLock.lock()
            return gravData
        } finally {
            accDataLock.unlock()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun gravUpdate(event: SensorEvent) {
        Log.d("grav", event.toString());
        try {
            gravDataLock.lock()
            gravData = SensorData(
                (event.values[0] + gravData.x) / 2,
                (event.values[1] + gravData.y) / 2,
                (event.values[2] + gravData.z) / 2)
        } finally {
            gravDataLock.unlock()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        sensorManager.unregisterListener(this)
        TODO("apagar as threads todas")
    }
}

