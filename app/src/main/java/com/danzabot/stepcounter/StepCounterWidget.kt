package com.danzabot.stepcounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
class StepCounterWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { push(context, manager, it) }
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, WidgetUpdateService::class.java))
    }

    companion object {

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, StepCounterWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { push(context, mgr, it) }
        }

        private fun push(context: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_step_counter)

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)

            views.setImageViewBitmap(R.id.widgetImage, renderBitmap(context))
            mgr.updateAppWidget(id, views)
        }

        private fun renderBitmap(context: Context): Bitmap {
            // Ensure the last known step count is loaded from prefs
            Steps.peekFromPrefs(context)

            val dm = context.resources.displayMetrics
            val widthPx = dm.widthPixels

            // Measure the real StepCounterView to get its natural height
            val view = StepCounterView(context)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val h = view.measuredHeight.coerceAtLeast(1)
            view.layout(0, 0, widthPx, h)

            val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Clip to rounded rect so bitmap corners are transparent (not square)
            val cornerRadius = 28f * dm.density
            val clipPath = Path().apply {
                addRoundRect(RectF(0f, 0f, widthPx.toFloat(), h.toFloat()),
                    cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            // Draw weather background (same system as the main view)
            val uiPrefs = context.getSharedPreferences("step_counter_ui", Context.MODE_PRIVATE)
            val scene = uiPrefs.getString("scene", "sun") ?: "sun"
            val kind = if (scene == "live") WeatherState.kind else scene
            WeatherScene.draw(canvas, 0f, 0f, widthPx.toFloat(), h.toFloat(),
                dm.density, kind, WeatherState.isNight)

            // Draw the exact same StepCounterView on top (it has its own semi-transparent panel)
            view.draw(canvas)

            return bmp
        }
    }
}
