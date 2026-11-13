package com.danzabot.stepcounter

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max

class StepCounterView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()

    private val uiPrefs = context.getSharedPreferences("step_counter_ui", Context.MODE_PRIVATE)

    // ── Color palette (matches Glass House reference slots) ──────────────────
    private val colorAccent  = 0xFF4A9EF5.toInt()
    private val colorMain    = 0xFFEEEEEE.toInt()
    private val colorGoal    = 0xFF4CD787.toInt()   // exact from reference

    private fun colShoe()   = colorAccent
    private fun colText()   = colorMain
    private fun colBarOn()  = colorAccent
    private fun colBarOff() = Color.argb(45, 180, 200, 255)   // subtle unlit track
    private fun colGoal()   = colorGoal
    private fun colWalker() = colorMain
    private fun colPanel()  = Color.argb(140, 0, 0, 0)         // readability overlay

    // ── Settings helpers ─────────────────────────────────────────────────────
    private fun goal()          = uiPrefs.getInt("goal", 10_000).coerceAtLeast(1_000)
    private fun barStyle()      = uiPrefs.getString("bar_style", "solid") ?: "solid"
    private fun walkerEnabled() = uiPrefs.getBoolean("walker", true)
    private fun walkerStyle()   = WalkerStyle.values()
        .firstOrNull { it.name == uiPrefs.getString("walker_style", "CLASSIC") } ?: WalkerStyle.CLASSIC
    private fun showMiles()     = uiPrefs.getBoolean("show_miles", true)
    private fun showStats()     = uiPrefs.getBoolean("show_stats", true)

    // ── Main draw ────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val k = resources.displayMetrics.density
        val t = 1.0f   // text scale factor (future: could be a setting)
        val w = width.toFloat()
        val pad = 20f * k

        // Semi-transparent panel behind the step counter for scene readability
        fillPaint.color = colPanel()
        canvas.drawRoundRect(0f, 0f, w, height.toFloat(), 28f * k, 28f * k, fillPaint)

        if (!Steps.granted(context)) {
            drawPermissionPrompt(canvas, w, k, t, pad)
            return
        }

        val goal    = goal()
        val n       = max(0, Steps.count)
        val reached = n >= goal
        val frac    = (n.toFloat() / goal).coerceIn(0f, 1f)

        val shoe = 9.5f * t * 1.3f * k
        val style = walkerStyle()
        val walker = walkerEnabled()
        val wh = if (walker) WalkerDrawer.heightFor(style, k) else 0f

        val contentX = pad
        val contentW = w - pad * 2

        // Shoe
        drawShoe(canvas, contentX, pad, shoe, k)

        // Step count + goal text
        val textSize = 9.5f * t * k
        val textX = contentX + shoe * 1.3f + 10f * k
        val textTopY = pad + (shoe - textSize) / 2f + shoe * 0.1f

        textPaint.textSize = textSize
        textPaint.color = colText()
        textPaint.typeface = Typeface.DEFAULT_BOLD

        val stepStr = if (Steps.count < 0) "--" else fmt(n.toLong())
        val goalStr = fmt(goal.toLong())
        val milesStr = if (showMiles()) " (${fmtMiles(Steps.toMiles(n))} mi)" else ""
        canvas.drawText("$stepStr / $goalStr$milesStr", textX, baseline(textTopY, textPaint), textPaint)

        // GOAL! label (right-aligned)
        if (reached) {
            textPaint.color = colGoal()
            val gw = textPaint.measureText("GOAL!")
            canvas.drawText("GOAL!", contentX + contentW - gw, baseline(textTopY, textPaint), textPaint)
        }

        // Walker + bar
        val barX = contentX
        val barY = pad + shoe + 10f * k + wh
        val barH = 5f * k
        val on = if (reached) colGoal() else colBarOn()

        if (walker) {
            val wx = barX + contentW * frac
            val clampedX = wx.coerceIn(barX + 5f * k, barX + contentW - 5f * k)
            WalkerDrawer.draw(canvas, style, clampedX, barY, wh, k, colWalker())
        }

        drawBar(canvas, frac, barX, barY, contentW, barH, barStyle(), on, colBarOff())

        // Stats row (calories & weight)
        if (showStats() && Steps.count >= 0) {
            val statsY = barY + barH + 14f * k
            val smallSize = 7.2f * t * k
            textPaint.textSize = smallSize
            textPaint.color = Color.argb(180, 220, 220, 220)
            textPaint.typeface = Typeface.DEFAULT

            val cals   = Steps.toCalories(n)
            val lbs    = Steps.toPounds(cals)
            val statsText = "${fmt(cals.toLong())} kcal  ·  ${fmtLbs(lbs)} lbs est."
            canvas.drawText(statsText, contentX, baseline(statsY, textPaint), textPaint)
        }
    }

    // ── Permission prompt ────────────────────────────────────────────────────

    private fun drawPermissionPrompt(canvas: Canvas, w: Float, k: Float, t: Float, pad: Float) {
        val textSize = 9.5f * t * k
        textPaint.textSize = textSize
        textPaint.color = colText()
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val msg = "TAP TO ALLOW STEPS"
        val mw = textPaint.measureText(msg)
        canvas.drawText(msg, (w - mw) / 2f, baseline(pad, textPaint), textPaint)
    }

    // ── Shoe geometry (exact from reference) ─────────────────────────────────

    private fun drawShoe(canvas: Canvas, ox: Float, oy: Float, shoe: Float, k: Float) {
        fillPaint.color = colShoe()

        // Sole: rounded rect
        canvas.drawRoundRect(
            ox, oy + shoe * 0.7f,
            ox + shoe * 1.3f, oy + shoe * 0.7f + shoe * 0.3f,
            shoe * 0.12f, shoe * 0.12f, fillPaint
        )

        // Upper: 6-point polygon
        path.reset()
        path.moveTo(ox + 0f,         oy + shoe * 0.72f)
        path.lineTo(ox + shoe * 0.1f, oy + shoe * 0.1f)
        path.lineTo(ox + shoe * 0.55f, oy + shoe * 0.1f)
        path.lineTo(ox + shoe * 0.7f, oy + shoe * 0.45f)
        path.lineTo(ox + shoe * 1.3f, oy + shoe * 0.55f)
        path.lineTo(ox + shoe * 1.3f, oy + shoe * 0.72f)
        path.close()
        canvas.drawPath(path, fillPaint)
    }

    // ── Progress bar (Bars.build from reference) ──────────────────────────────

    private fun drawBar(canvas: Canvas, frac: Float, x: Float, y: Float, w: Float, h: Float,
                        style: String, on: Int, off: Int) {
        val f = frac.coerceIn(0f, 1f)
        when (style) {
            "solid" -> {
                fillPaint.color = off
                canvas.drawRoundRect(x, y, x + w, y + h, h / 2f, h / 2f, fillPaint)
                if (f > 0f) {
                    fillPaint.color = on
                    val litW = max(h, w * f)
                    canvas.drawRoundRect(x, y, x + litW, y + h, h / 2f, h / 2f, fillPaint)
                }
            }
            "dots" -> {
                val n = 24; val segW = w / n; val d = minOf(segW * 0.6f, h)
                for (i in 0 until n) {
                    fillPaint.color = if ((i + 0.5f) / n <= f) on else off
                    canvas.drawOval(x + i * segW + (segW - d) / 2f, y + (h - d) / 2f,
                        x + i * segW + (segW - d) / 2f + d, y + (h - d) / 2f + d, fillPaint)
                }
            }
            else -> { // segments
                val n = 24; val segW = w / n; val gap = max(segW * 0.2f, 0.3f)
                for (i in 0 until n) {
                    fillPaint.color = if ((i + 0.5f) / n <= f) on else off
                    canvas.drawRect(x + i * segW, y, x + i * segW + segW - gap, y + h, fillPaint)
                }
            }
        }
    }

    // ── Measure ───────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val k = resources.displayMetrics.density
        val t = 1.0f
        val pad = 20f * k
        val shoe = 9.5f * t * 1.3f * k
        val style = walkerStyle()
        val wh = if (walkerEnabled()) WalkerDrawer.heightFor(style, k) else 0f
        val bh = 5f * k
        val statsRow = if (showStats()) 7.2f * k + 14f * k else 0f
        val totalH = (pad + shoe + 10f * k + wh + bh + statsRow + pad).toInt()

        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, totalH)
    }

    fun refresh() {
        requestLayout()
        invalidate()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun baseline(topY: Float, p: Paint): Float {
        val fm = p.fontMetrics
        return topY - fm.ascent
    }

    private fun fmt(n: Long)      = String.format(Locale.US, "%,d", n)
    private fun fmtMiles(m: Double) = String.format(Locale.US, "%.1f", m)
    private fun fmtLbs(lbs: Double) = String.format(Locale.US, "%.2f", lbs)
}
