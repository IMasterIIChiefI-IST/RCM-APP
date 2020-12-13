package com.example.application_wifi


import android.R.style.Widget
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService


class NewAppWidget : AppWidgetProvider() {

    private val BTN_STATE = "BtnPressedUpdate"

    companion object{
        private var button = true
        private var text_top: String? = "Initializing Service"
        private var text_bot: String? = "..."
        private var pro: Int? = 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.hasExtra("Top")) {
            text_top = intent.getStringExtra("Top")
            Log.d("text_top","$text_top")
        }
        if (intent.hasExtra("Bot")) {
            text_bot = intent.getStringExtra("Bot")
            Log.d("text_bot","$text_bot")
        }
        if (intent.hasExtra("Pro")) {
            pro = intent.getIntExtra("Pro", 0)
            Log.d("text_bot","$text_bot")
        }
        if (intent.hasExtra("Success")) {
           button = intent.getBooleanExtra("Success", true)
        }
        if (BTN_STATE == intent.action) {
            if (!button){
                button=!button
                val serviceIntent = Intent(context, Wifi_Telemetry::class.java)
                context.startForegroundService(serviceIntent)
            }

        }
    }

    init{
        Log.d("notbutton", "ESTOU A DAR UPDATE")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray){

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        val serviceIntent = Intent(context, Wifi_Telemetry::class.java)
        context.startForegroundService(serviceIntent)
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled

        val serviceIntent = Intent(context, Wifi_Telemetry::class.java)
        context.stopService(serviceIntent)
        super.onDisabled(context)
    }

/*
   Excellent >-50 dBm
   Good -50 to -60 dBm
   Fair -60 to -70 dBm
   Weak < -70 dBm
 */

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.new_app_widget)
        val watchWidget = ComponentName(context, NewAppWidget::class.java)
        val intent = Intent(context, NewAppWidget::class.java)
        intent.action = BTN_STATE

        Log.d("estou a dar", "UPDATE UI ")

        views.setTextViewText(R.id.appwidget_text_top, text_top)
        views.setTextViewText(R.id.appwidget_text_bot, text_bot)

        var quality = 0
        if (pro!! < -70) {
            quality = 1
        } else {
            if (-60 > pro!! && pro!! >= -70) {
                quality = 2
            } else {
                if (-50 > pro!! && pro!! >= -60) {
                    quality = 3
                } else {
                    quality = 4
                }
            }
        }

        views.setProgressBar(R.id.progressBar, 4, quality, pro == 0)
        views.setOnClickPendingIntent(R.id.imageButton, PendingIntent.getBroadcast(context, 0, intent, 0))
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.updateAppWidget(watchWidget, views)
        val appintent = Intent(context, Wifi_Telemetry::class.java)
        val appWidgetM = AppWidgetManager.getInstance(context)
        val pendingIntent = PendingIntent.getActivity(context, 0, appintent, 0)
        val pendingIntentapp = PendingIntent.getActivity(context, 0, intent, 0)
        appWidgetM.updateAppWidget(ComponentName(context.packageName, NewAppWidget::class.java.name), views)

    }
}



