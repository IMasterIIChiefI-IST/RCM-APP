package com.example.application_wifi

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WIFI_STATE_ENABLED
import android.net.wifi.WifiManager.WIFI_STATE_ENABLING
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.collection.CircularArray
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.log
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
Service.START_STICKY
Service is restarted if it gets terminated. Intent data passed to the onStartCommand method is null. Used for services which manages their own state and do not depend on the Intent data.


Service.START_NOT_STICKY
Service is not restarted. Used for services which are periodically triggered anyway. The service is only restarted if the runtime has pending startService() calls since the service termination.


Service.START_REDELIVER_INTENT
Similar to Service.START_STICKY but the original Intent is re-delivered to the onStartCommand method.

 */


class Wifi_Telemetry : Service() , SensorEventListener {

    /*
      Excellent >-50 dBm
      Good -50 to -60 dBm
      Fair -60 to -70 dBm
      Weak < -70 dBm
    */
    //------------------------------------------------------------------------------------------vars
    private val TIMESONSORREAD: Long = 500
    private val MAXTRY = 5
    private val DATASIZE = 8
    private val DEFAULTINTREVAL = 5000
    private val SENSORINTREVAL = 1000

    private var machineState = 0
    private var failedAttempts = 0
    private var buttonState : Boolean = true

    lateinit var pManager: PowerManager
    //------------------------------------------------------------------------------------Widgetvars
    private lateinit var appWidgetManager: AppWidgetManager
    var ids = IntArray(0)
    //------------------------------------------------------------------------------------Sensorvars
    lateinit var sensorManager: SensorManager
    lateinit var acc_sensor: Sensor
    lateinit var grav_sensor: Sensor
    lateinit var accData: SensorData
    lateinit var gravData: SensorData
    var accregs: Boolean = false
    var gravregs: Boolean = false
    private val accDataLock = ReentrantLock()
    private val gravDataLock = ReentrantLock()
    //------------------------------------------------------------------------------------SensorData
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
    //--------------------------------------------------------------------------------------WIFIvars
    lateinit var wManager: WifiManager
    lateinit var target_connection: WifiData
    lateinit var otherConnectionResults: List<WifiData>
    lateinit var myConnectionResults: WifiData
    private val myConnectionLock = ReentrantLock()
    private val otherConnectionLock = ReentrantLock()
    //--------------------------------------------------------------------------------------WifiData
    data class WifiData(
        val SSID: String,
        val BSSID: String,
        val rssi: Int,
        val frequency: Int,
        val connected: Boolean
    ) {
        /*fun ToJson(data: JSONObject): JSONObject {
            try {
                data.put("SSID", SSID)
                data.put("BSSID", BSSID);
                data.put("rssi", rssi);
                data.put("frequency", frequency);
                data.put("connected", connected);
                return data

            } catch (e: JSONException) {
                throw e
            }
        }*/

        fun ToJson(): JSONObject {
            val dataJson = JSONObject();
            try {
                dataJson.put("SSID", SSID);
                dataJson.put("BSSID", BSSID);
                dataJson.put("rssi", rssi);
                dataJson.put("frequency", frequency);
                dataJson.put("connected", connected);
                return dataJson

            } catch (e: JSONException) {
                throw e
            }
        }
    }
    //---------------------------------------------------------------------------------Coroutinevars
    val jobScope = CoroutineScope(SupervisorJob())
    //-----------------------------------------------------------------------------------dataReports
    var dataReports = CircularArray<List<WifiData>>(DATASIZE)
    private val dataReportsLock = ReentrantLock()

    fun DataToJson(data: List<WifiData>): String {
        val mainJson = JSONObject();
        val dataJson = JSONArray();
        try {
            mainJson.put("size", data.size);
            data.forEach() {
                dataJson.put(it.ToJson());
            }
            mainJson.put("data", dataJson);
            return mainJson.toString();
        } catch (e: JSONException) {
            throw e
        }
    }
    //----------------------------------------------------------------------------------------BINDER
    override fun onBind(intent: Intent): IBinder? = null

    inner class LocalBinder : Binder() {
        fun getService() = this@Wifi_Telemetry
    }
    //----------------------------------------------------------------------------------onHandleWork
    /*override fun onHandleWork(intent: Intent) {
        buttonState = intent.getBooleanExtra("buttonState", true)
        jobScope.launch(Dispatchers.Unconfined) {
            try {
                looperService()
            } catch (e: Exception) {

            }
        }
        unregisterReceiver(broadcastReceiver)
    }*/
    //--------------------------------------------------------------------------------onStartCommand
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        buttonState = intent.getBooleanExtra("buttonState", true)
        Log.d("onStartCommand","$buttonState")
        jobScope.launch(Dispatchers.Unconfined) {
            try {
                looperService()
            } catch (e: Exception) {

            }
        }
        unregisterReceiver(broadcastReceiver)
        stopSelf()
        return START_NOT_STICKY ;
    }
    //------------------------------------------------------------------------------------------HTTP
    // SERVICE LAUCHES INTENTSERVICE TO SEND DATA
    class LiteRequestService : IntentService("LiteRequestService") {
        override fun onHandleIntent(intent: Intent?) {
            val dataToSend = intent?.getStringExtra("json")
            var response = "";
            try {
                val url = URL("https://hookbin.com/6JXza2o9xQcLbb031Y7B")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST";
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.doOutput = true
                conn.doInput = true;

                val os = DataOutputStream(conn.outputStream);
                os.writeBytes(dataToSend);

                os.flush();
                os.close();

                response = conn.responseCode.toString()
                Log.i("STATUS", response);
                conn.disconnect();
            } catch (e: Exception) {
                e.printStackTrace();
                Thread.currentThread().interrupt()
            }

        }
    }
    //----------------------------------------------------------------------------------------LOCKED

    fun isDeviceLocked(context: Context): Boolean {
        var isLocked = false
        val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode()
        isLocked = if (inKeyguardRestrictedInputMode) {
            true
        } else {

            val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                !powerManager.isInteractive
            } else {
                !powerManager.isScreenOn
            }
        }
        return isLocked
    }
    //-----------------------------------------------------------------------------broadcastReceiver

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            val connectivityManager =
                context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetInfo = connectivityManager.activeNetworkInfo
            if (activeNetInfo != null
                && activeNetInfo.type == ConnectivityManager.TYPE_WIFI
            ) {
                if (updateMyConnection()) {
                    if (!jobScope.isActive) {
                        looperService()
                    }
                } else {
                    if (machineState == 1) {
                        unregister(this@Wifi_Telemetry)
                        failedAttempts = MAXTRY
                    }

                }
            } else {
                unregister(this@Wifi_Telemetry)
                failedAttempts = MAXTRY
            }
        }
    }
    //---------------------------------------------------------------------------Widget-Comunication
    fun widget_coms(stringtop: String, stringbot: String, int: Int , suceess: Boolean = true){
        val intentWidget = Intent(this@Wifi_Telemetry,
            NewAppWidget::class.java)
        intentWidget.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ids = appWidgetManager.getAppWidgetIds((ComponentName(
            applicationContext,
            NewAppWidget::javaClass.get(NewAppWidget()))))
        intentWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        intentWidget.putExtra("Top", stringtop)
        intentWidget.putExtra("Bot", stringbot)
        intentWidget.putExtra("Pro", int)
        intentWidget.putExtra("Success", suceess)
        sendBroadcast(intentWidget)
    }
    //--------------------------------------------------------------------------------------Movement
    private fun isNotMoving(): Boolean {
        val acc = getAcc()
        val grav = getGrav()
        var vectorAcc = 0.0f
        var vectorGrav = 0.0f
        var counter = 1
        object : CountDownTimer(TIMESONSORREAD, TIMESONSORREAD / 8) {
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
    //-----------------------------------------------------------------------------------OTHERS-WIFI
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
    //---------------------------------------------------------------------------------------MY-WIFI
    fun updateMyConnection(): Boolean {
        Log.d("estou a ver ","a minha rede")
        if (WIFI_STATE_ENABLED != wManager.wifiState && WIFI_STATE_ENABLING != wManager.wifiState) {
            return false
        } else {
            val wifiInfo =
                (this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo
            Log.d("estou ligado ",wifiInfo.ssid)
            if (wifiInfo.frequency != -1 && wifiInfo.ssid.contains(target_connection.SSID, ignoreCase = true)) {
                try {
                    Log.d("passei ",wifiInfo.ssid)
                    myConnectionLock.lock()
                    myConnectionResults = WifiData(
                        wifiInfo.ssid,
                        wifiInfo.bssid,
                        wManager.connectionInfo.rssi,
                        wifiInfo.frequency,
                        true
                    )
                    Log.d("minha ligaçao e", myConnectionResults.SSID)
                } finally {
                    myConnectionLock.unlock()
                }
                return true
            } else {
                try {
                    myConnectionLock.lock()
                    myConnectionResults = WifiData(
                        "N/Connected",
                        "0",
                        0,
                        -1,
                        false
                    )
                    Log.d("minha ligaçao e", myConnectionResults.SSID)
                } finally {
                    myConnectionLock.unlock()
                }
                return false
            }
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
    //---------------------------------------------------------------------------------------SENSORS
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
                (event.values[0]),
                (event.values[1]),
                (event.values[2]))
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
                (event.values[0]),
                (event.values[1]),
                (event.values[2]))
        } finally {
            gravDataLock.unlock()
        }
    }
    //------------------------------------------------------------------------------------unregister
    fun unregister(context: SensorEventListener) {
        if (accregs) {
            sensorManager.unregisterListener(context, grav_sensor)
        }
        if (gravregs) {
            sensorManager.unregisterListener(context, acc_sensor)
        }
        unregisterReceiver(broadcastReceiver)
    }

    //----------------------------------------------------------------------------------Notification
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "com.example.simpleapp"
        val channelName = "My Background Service"
        val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }
    //--------------------------------------------------------------------------------------ONCREATE

    override fun onCreate() {
        super.onCreate()
        Log.d("onCreate","estou aqui ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startMyOwnForeground()
        else startForeground(1, Notification())
        machineState = 0
        // TARGET CONNECTION CREATION
        target_connection = WifiData("eduroam", "MAC", 0, 0, true)
        if (!this::wManager.isInitialized) {
            wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        }
        if (!this::appWidgetManager.isInitialized) {
            appWidgetManager = AppWidgetManager.getInstance(this)
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.net.wifi.STATE_CHANGE")
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(broadcastReceiver, intentFilter)
        Log.d("onCreate","acabei")

    }
    //----------------------------------------------------------------------------------------LOOPER
    fun looperService() {
        failedAttempts = 0
        GlobalScope.launch() {
            try {
                while (failedAttempts < MAXTRY) {
                    if (buttonState) {
                        when (machineState) {
                            0 -> {
                                if (initialization()) {
                                    ++machineState
                                    failedAttempts = 0
                                    Log.d("machineState0","$machineState")
                                    Log.d("failedAttempts0","$failedAttempts")
                                } else {
                                    ++failedAttempts
                                    widget_coms("Initializing", "Attempt".plus("-->").plus(
                                        failedAttempts.toString()), 0)
                                    Log.d("machineState0","$machineState")
                                    Log.d("failedAttempts0","$failedAttempts")
                                }
                            }
                            1 -> {
                                if (updateMyConnection()) {
                                    if (isNotMoving() && isDeviceLocked(this@Wifi_Telemetry)) {//keyguardManager.isKeyguardLocked
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
                                        val intent = Intent(this@Wifi_Telemetry,
                                            LiteRequestService::class.java)
                                        intent.putExtra("json", DataToJson(dataReports.popLast()))
                                        startActivity(intent)
                                        widget_coms(getMyConnection().SSID, "Reports_Generated :".plus(dataReports.size().toString()), getMyConnection().rssi)
                                    } else {
                                        widget_coms(getMyConnection().SSID,
                                            "Stop Moving/Lock The Phone",
                                            getMyConnection().rssi)
                                    }
                                } else {
                                    ++failedAttempts
                                    widget_coms("Connected", "Wrong NetWork", 0)
                                }
                            }
                        }
                    }
                    delay(DEFAULTINTREVAL.toLong())
                }
            } catch (e: Exception) {
                //onTerminate()
            }
            widget_coms("Failed", "Searching", 0,false)
        }
    }
    //----------------------------------------------------------------------------------------LOOPER
    private suspend fun initialization(): Boolean {
        Log.d("initialization","estou aqui ")
        for (i in 1..MAXTRY) {
            if (!this::wManager.isInitialized) {
                wManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            }
            if (!this::appWidgetManager.isInitialized) {
                appWidgetManager = AppWidgetManager.getInstance(this)
            }
            if (WIFI_STATE_ENABLED != wManager.wifiState) {
                delay(1000L)
            } else {
                if (updateMyConnection()) {

                    if (!this::sensorManager.isInitialized) {
                        sensorManager =
                            this.applicationContext.getSystemService(SENSOR_SERVICE) as SensorManager;
                    }
                    acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                    grav_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                    accregs = this.sensorManager.registerListener(this, acc_sensor, SENSORINTREVAL)
                    gravregs = this.sensorManager.registerListener(this,
                        grav_sensor,
                        SENSORINTREVAL)

                    if (!this::pManager.isInitialized) {
                        pManager =
                            this.applicationContext.getSystemService(POWER_SERVICE) as PowerManager
                    }

                    appWidgetManager.updateAppWidget(ComponentName(applicationContext,
                        NewAppWidget::javaClass.get(NewAppWidget())),
                        RemoteViews(this.packageName, R.layout.new_app_widget))
                    return true
                } else {
                    delay(1000L)
                }
            }
        }
        return false
    }
    //-----------------------------------------------------------------------------------ONTERMINATE
    /*
    override fun onTerminate() {
        unregister(this)
        super.onTerminate()
    }*/

}