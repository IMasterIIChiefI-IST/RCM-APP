package com.example.application_wifi

import android.R
import android.R.style.Widget
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews


class NewAppWidget : AppWidgetProvider() {

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
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val widgetText_top = context.getString(R.string.appwidget_text_top)
    val widgetText_bot = context.getString(R.string.appwidget_text_bot)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.appwidget_text_top, widgetText_top)
    views.setTextViewText(R.id.appwidget_text_bot, widgetText_bot)
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
    val intent = Intent(this, Settings::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

    views.setOnClickPendingIntent(R.id.btnActivate, pendingIntent)

    val appWidgetManager = AppWidgetManager.getInstance(this)
    appWidgetManager.updateAppWidget(ComponentName(this.getPackageName(), Widget::class.java.name),
        views)

}
