package com.danzabot.stepcounter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

enum class WalkerStyle(val label: String) {
    CLASSIC("Classic"),
    RUNNER("Runner"),
    HIKER("Hiker"),
    CYCLIST("Cyclist"),
    DOG_WALKER("Dog Walker")
}

object WalkerDrawer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun heightFor(style: WalkerStyle, k: Float): Float = when (style) {
        WalkerStyle.CYCLIST -> 24f * k
        else -> 16f * k
    }

    fun draw(canvas: Canvas, style: WalkerStyle, x: Float, by: Float, wh: Float, k: Float, color: Int) {
        paint.color = color
        when (style) {
            WalkerStyle.CLASSIC   -> drawClassic(canvas, x, by, wh, k)
            WalkerStyle.RUNNER    -> drawRunner(canvas, x, by, wh, k)
            WalkerStyle.HIKER     -> drawHiker(canvas, x, by, wh, k)
            WalkerStyle.CYCLIST   -> drawCyclist(canvas, x, by, wh, k)
            WalkerStyle.DOG_WALKER -> drawDogWalker(canvas, x, by, wh, k)
        }
    }

    // Original reference implementation — static walker on the bar
    private fun drawClassic(canvas: Canvas, x: Float, by: Float, wh: Float, k: Float) {
        val top = by - wh
        // Head
        oval(canvas, x - 2.5f * k, top, 5f * k, 5f * k)
        // Body
        rect(canvas, x - 1f * k, top + 5f * k, 2f * k, 6f * k)
        // Right leg
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x + 3.5f * k, by, x + 1.8f * k, by)
        // Left leg
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x - 1.8f * k, by, x - 3.5f * k, by)
    }

    // Runner: one leg forward, one back; arms extended
    private fun drawRunner(canvas: Canvas, x: Float, by: Float, wh: Float, k: Float) {
        val top = by - wh
        // Head (slightly forward)
        oval(canvas, x - 1.5f * k, top, 5f * k, 5f * k)
        // Body (angled forward)
        poly(canvas, x, top + 5f * k, x + 2f * k, top + 5f * k, x + 3f * k, top + 11f * k, x + 1f * k, top + 11f * k)
        // Forward leg
        poly(canvas, x + 1f * k, top + 11f * k, x + 3f * k, top + 11f * k, x + 6f * k, by, x + 4f * k, by)
        // Back leg
        poly(canvas, x + 1f * k, top + 11f * k, x + 3f * k, top + 11f * k, x - 2f * k, by, x - 4f * k, by)
        // Forward arm
        poly(canvas, x + 1f * k, top + 6f * k, x + 2f * k, top + 6f * k, x - 2f * k, top + 10f * k, x - 3f * k, top + 9f * k)
        // Back arm
        poly(canvas, x + 1f * k, top + 6f * k, x + 2f * k, top + 6f * k, x + 5f * k, top + 9f * k, x + 4f * k, top + 10f * k)
    }

    // Hiker: backpack bump on the back
    private fun drawHiker(canvas: Canvas, x: Float, by: Float, wh: Float, k: Float) {
        val top = by - wh
        // Head
        oval(canvas, x - 2.5f * k, top, 5f * k, 5f * k)
        // Body
        rect(canvas, x - 1f * k, top + 5f * k, 2f * k, 6f * k)
        // Backpack (rounded rect on the back)
        rrect(canvas, x - 4f * k, top + 5.5f * k, 3f * k, 5f * k, 1f * k)
        // Right leg
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x + 2.5f * k, by, x + 0.8f * k, by)
        // Left leg (with slight stride)
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x - 3f * k, by, x - 1.5f * k, by)
        // Walking stick
        line(canvas, x + 2f * k, top + 6f * k, x + 5f * k, by, 1f * k)
    }

    // Cyclist: on a simple two-circle bike frame
    private fun drawCyclist(canvas: Canvas, x: Float, by: Float, wh: Float, k: Float) {
        val top = by - wh
        val wheelR = 7f * k
        val wheelY = by - wheelR     // wheel center Y
        val lWheelX = x - wheelR * 1.4f
        val rWheelX = x + wheelR * 1.4f

        // Wheels (outline rings)
        strokeOval(canvas, lWheelX - wheelR, wheelY - wheelR, wheelR * 2, wheelR * 2, 1.5f * k)
        strokeOval(canvas, rWheelX - wheelR, wheelY - wheelR, wheelR * 2, wheelR * 2, 1.5f * k)

        // Frame: down-tube, top-tube, seat-tube
        line(canvas, lWheelX, wheelY, rWheelX, wheelY, 1.5f * k)                       // chain stay
        line(canvas, lWheelX, wheelY, x, wheelY - wheelR * 0.9f, 1.5f * k)             // down tube
        line(canvas, rWheelX, wheelY, x, wheelY - wheelR * 0.9f, 1.5f * k)             // seat tube
        line(canvas, x, wheelY - wheelR * 0.9f, x - wheelR * 0.6f, wheelY - wheelR * 1.5f, 1.5f * k) // seat post

        // Rider: head, body
        val seatY = wheelY - wheelR * 1.5f
        oval(canvas, x - wheelR * 0.7f - 2.5f * k, seatY - 8f * k, 5f * k, 5f * k)   // head
        line(canvas, x - wheelR * 0.7f, seatY, x, wheelY - wheelR * 0.9f, 2f * k)      // torso
        // Leg (pedaling)
        line(canvas, x - wheelR * 0.7f, seatY, x + 1f * k, wheelY - 2f * k, 2f * k)   // forward leg
    }

    // Dog walker: stick figure + leash + small dog
    private fun drawDogWalker(canvas: Canvas, x: Float, by: Float, wh: Float, k: Float) {
        val top = by - wh
        // Person
        oval(canvas, x - 2.5f * k, top, 5f * k, 5f * k)
        rect(canvas, x - 1f * k, top + 5f * k, 2f * k, 6f * k)
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x + 2f * k, by, x + 0.5f * k, by)
        poly(canvas, x - 1f * k, top + 11f * k, x + 1f * k, top + 11f * k, x - 2.5f * k, by, x - 1f * k, by)
        // Arm holding leash
        line(canvas, x + 1f * k, top + 7f * k, x + 8f * k, top + 9f * k, 1.5f * k)
        // Leash (curve toward dog)
        line(canvas, x + 8f * k, top + 9f * k, x + 11f * k, by - 4f * k, 1f * k)
        // Dog body
        oval(canvas, x + 10f * k, by - 5f * k, 7f * k, 4f * k)
        // Dog head
        oval(canvas, x + 15f * k, by - 7f * k, 4f * k, 4f * k)
        // Dog legs
        line(canvas, x + 11f * k, by - 2f * k, x + 11f * k, by, 1.5f * k)
        line(canvas, x + 15f * k, by - 2f * k, x + 15f * k, by, 1.5f * k)
        // Dog tail
        line(canvas, x + 10f * k, by - 4f * k, x + 7f * k, by - 8f * k, 1f * k)
    }

    // ── primitives ───────────────────────────────────────────────────────────

    private fun oval(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawOval(x, y, x + w, y + h, paint)
    }

    private fun strokeOval(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, strokeW: Float) {
        val saved = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeW
        canvas.drawOval(x, y, x + w, y + h, paint)
        paint.style = saved
        paint.strokeWidth = 0f
    }

    private fun rect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        canvas.drawRect(x, y, x + w, y + h, paint)
    }

    private fun rrect(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float) {
        canvas.drawRoundRect(x, y, x + w, y + h, r, r, paint)
    }

    private fun line(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, strokeW: Float) {
        val saved = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeW
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.style = saved
        paint.strokeWidth = 0f
    }

    private fun poly(canvas: Canvas, vararg pts: Float) {
        if (pts.size < 4) return
        path.reset()
        path.moveTo(pts[0], pts[1])
        var i = 2
        while (i + 1 < pts.size) {
            path.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
