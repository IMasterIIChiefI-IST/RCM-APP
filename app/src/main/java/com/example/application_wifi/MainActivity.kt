package com.example.application_wifi

import android.os.Bundle
import android.os.Debug
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //var Old_state = false
        //val vg = setContentView(R.layout.settings_activity)
        /*val constraints = Constraints.Builder()
            .setRequiresCharging((application as Application_Wifi).checkNetwork())
            .build()*/
        //PeriodicWorkRequest.Builder(MyPeriodicWork, 10, TimeUnit.SECONDS);
        /*val work =
            (application as Application_Wifi).workManager.enqueueUniquePeriodicWork("MyPeriodicWork",
                ExistingPeriodicWorkPolicy.KEEP, MyPeriodicWork) */


        SW_On_Off.setOnClickListener {
            //SW_On_Off.isClickable = (application as Application_Wifi).checkNetwork()
            TODO("Not yet implemented")
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                /*val bool = (application as Application_Wifi).checkNetwork()
                seekBar?.isClickable = bool
                seekBar_value.text = (seekBar?.progress).toString()
                if (bool) {
                } else {
                    return
                }*/
                TODO("Not yet implemented")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                /*val bool = (application as Application_Wifi).checkNetwork()
                seekBar?.isClickable = bool
                seekBar_value.text = (seekBar?.progress).toString()
                if (bool) {
                } else {
                    return
                }*/
                TODO("Not yet implemented")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                /*val bool = (application as Application_Wifi).checkNetwork()
                seekBar?.isClickable = bool
                seekBar_value.text = (seekBar?.progress).toString()
                if (bool) {
                } else {
                    return
                }
                }*/
                TODO("Not yet implemented")
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    /*
    fun updateUi(context_activity: AppCompatActivity , context_application: Application_Wifi) {
    findViewById<ViewGroup>(R.id.app).invalidate()
    val Actual_state = context.checkNetwork()
    seekBar?.isEnabled = Actual_state
    SW_On_Off?.isEnabled = Actual_state
    (application as Application_Wifi).workManager.WorkInfo.getState()
    if (!Actual_state && !Old_state) {
    var context = (application as Application_Wifi)
    (application as Application_Wifi).sensorManager.unregisterListener(context)
    seekBar_value.text = "Not Available"
    X.text = "Not Available"
    Y.text = "Not Available"
    Z.text = "Not Available"
    flag.text = "Not Available"
    (application as Application_Wifi).workManager.cancelAllWorkByTag("UI")
    Old_state = Actual_state
    } else {
    val sensor = (application as Application_Wifi).acc_data
    val sensor2 = (application as Application_Wifi).grav_data
    X.text = sensor.x.toString()
    Y.text = sensor.y.toString()
    Z.text = sensor.z.toString()
    X1.text = sensor2.x.toString()
    Y1.text = sensor2.y.toString()
    Z1.text = sensor2.z.toString()
    val aux = sqrt((sensor.x * sensor.x) + (sensor.y * sensor.y) + (sensor.z * sensor.z))
    val aux2 = sqrt((sensor2.x * sensor2.x) + (sensor2.y * sensor2.y) + (sensor2.z * sensor2.z))
    flag.text = if(abs(aux - aux2)<(0.20 * (if(aux < aux2) aux2 else aux))) "True" else "False"
    }
    } */

}
