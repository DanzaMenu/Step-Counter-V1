package com.danzabot.stepcounter

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View

class SceneView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val prefs = context.getSharedPreferences("step_counter_ui", Context.MODE_PRIVATE)
    private val validKinds = setOf("sun", "partly", "cloud", "fog", "rain", "snow", "storm")
    private var frameCount = 0L

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            if (frameCount % 120 == 0L) Log.d("SceneView", "frame $frameCount")
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("SceneView", "attached, starting choreographer")
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val u = resources.displayMetrics.density
        val scene = prefs.getString("scene", "sun") ?: "sun"

        val kind = when {
            scene == "live"     -> WeatherState.kind
            scene in validKinds -> scene
            else                -> "sun"
        }

        if (scene == "vegas") {
            VegasScene.draw(canvas, 0f, 0f, w, h, u, kind, WeatherState.isNight)
        } else {
            WeatherScene.draw(canvas, 0f, 0f, w, h, u, kind, WeatherState.isNight)
        }
    }
}
