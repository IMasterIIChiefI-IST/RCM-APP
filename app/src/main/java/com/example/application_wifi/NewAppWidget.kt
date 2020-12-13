package com.example.application_wifi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews


class NewAppWidget : AppWidgetProvider() {

    var text_top: String? = "not connected"
    var text_bot: String? = "not connected"
    var pro: Int? = 0

    override fun onReceive(context: Context?, intent: Intent?) {
        text_top = intent?.getStringExtra("Top")
        text_bot = intent?.getStringExtra("Bot")
        pro = intent?.getIntExtra("Pro", 0)
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

/*
   Excellent >-50 dBm
   Good -50 to -60 dBm
   Fair -60 to -70 dBm
   Weak < -70 dBm
 */

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.new_app_widget)
        views.setTextViewText(R.id.appwidget_text_top, text_top)
        views.setTextViewText(R.id.appwidget_text_bot, text_bot)
        var quality = -1
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
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
        val intent = Intent(context, Application_Wifi::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val appWidgetM = AppWidgetManager.getInstance(context)
        appWidgetM.updateAppWidget(ComponentName(context.packageName,
            NewAppWidget::class.java.name),
            views)

    }
}


