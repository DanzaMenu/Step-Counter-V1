package com.danzabot.stepcounter

import android.graphics.*
import kotlin.math.*

object WeatherScene {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val blur4 = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    private val blur8 = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
    private val startNanos = System.nanoTime()

    fun draw(
        canvas: Canvas,
        ox: Float, oy: Float,
        w: Float, h: Float,
        u: Float,
        kind: String,
        isNight: Boolean,
        alpha: Int = 255
    ) {
        val secs = (System.nanoTime() - startNanos) / 1_000_000_000f
        val s = min(w, h)
        val (skyTop, skyBot) = skyColors(kind, isNight)

        drawSky(canvas, ox, oy, w, h, skyTop, skyBot, alpha)

        if (!isNight && (kind == "sun" || kind == "partly")) {
            drawSun(canvas, ox, oy, w, h, s, secs, alpha)
        }
        if (isNight && kind != "fog") {
            drawMoon(canvas, ox, oy, w, h, s, skyTop, alpha)
            drawStars(canvas, ox, oy, w, h, secs, u, alpha)
        }

        if (kind == "partly" || kind == "cloud" || kind == "rain" || kind == "storm") {
            drawClouds(canvas, ox, oy, w, h, s, secs, kind, alpha)
        }

        if (kind == "fog") drawFog(canvas, ox, oy, w, h, secs)

        if (kind == "rain" || kind == "storm") drawRain(canvas, ox, oy, w, h, secs, u, alpha)

        if (kind == "snow") drawSnow(canvas, ox, oy, w, h, secs, u, alpha)

        if (kind == "storm") drawLightning(canvas, ox, oy, w, h, secs, u, alpha)
    }

    // ── Sky gradient ─────────────────────────────────────────────────────────

    private fun skyColors(kind: String, isNight: Boolean): Pair<Int, Int> = when {
        kind == "storm"                   -> 0x1A1A2E to 0x2D2D44
        kind == "fog"                     -> 0xBBC0C4 to 0xD0D4D8
        isNight && kind == "cloud"        -> 0x1E2235 to 0x192340
        isNight                           -> 0x0D1B3E to 0x192340
        kind == "cloud" || kind == "rain" -> 0x7A8FA0 to 0x8FA3B1
        kind == "partly"                  -> 0x5B9BD5 to 0x87C1EA
        else                              -> 0x4A90D9 to 0x87C1EA
    }.let { (t, b) -> (0xFF000000.toInt() or t) to (0xFF000000.toInt() or b) }

    private fun drawSky(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                         skyTop: Int, skyBot: Int, alpha: Int) {
        p.shader = LinearGradient(ox, oy, ox, oy + h, skyTop, skyBot, Shader.TileMode.CLAMP)
        p.alpha = alpha
        p.maskFilter = null
        canvas.drawRect(ox, oy, ox + w, oy + h, p)
        p.shader = null
        p.alpha = 255
    }

    // ── Sun ──────────────────────────────────────────────────────────────────

    private fun drawSun(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                        s: Float, secs: Float, alpha: Int) {
        val cx = ox + w * 0.74f
        val cy = oy + h * 0.13f
        val sunR = s * 0.065f

        p.style = Paint.Style.FILL
        p.maskFilter = blur8
        p.color = Color.argb((alpha * 0.25f).toInt(), 0xFF, 0xE0, 0x40)
        canvas.drawCircle(cx, cy, sunR * 2.2f, p)
        p.maskFilter = null

        p.color = Color.argb(alpha, 0xFF, 0xD7, 0x00)
        canvas.drawCircle(cx, cy, sunR, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = sunR * 0.10f
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.argb((alpha * 0.85f).toInt(), 0xFF, 0xD7, 0x00)
        for (i in 0..11) {
            val angle = i * (PI.toFloat() * 2f / 12f) + secs * 0.25f
            canvas.drawLine(
                cx + cos(angle) * sunR * 1.35f, cy + sin(angle) * sunR * 1.35f,
                cx + cos(angle) * sunR * 1.85f, cy + sin(angle) * sunR * 1.85f,
                p
            )
        }
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    // ── Moon + Stars ─────────────────────────────────────────────────────────

    private fun drawMoon(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                         s: Float, skyTopColor: Int, alpha: Int) {
        val moonX = ox + w * 0.72f
        val moonY = oy + h * 0.12f
        val moonR = s * 0.055f

        p.maskFilter = blur8
        p.color = Color.argb((alpha * 0.15f).toInt(), 0xEE, 0xEE, 0xCC)
        canvas.drawCircle(moonX, moonY, moonR * 2.5f, p)
        p.maskFilter = null

        p.color = Color.argb((255 * 0.92f).toInt(), 0xEE, 0xEE, 0xC0)
        canvas.drawCircle(moonX, moonY, moonR, p)

        p.color = skyTopColor
        canvas.drawCircle(moonX + moonR * 0.40f, moonY - moonR * 0.12f, moonR * 0.78f, p)
    }

    private fun drawStars(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                          secs: Float, u: Float, alpha: Int) {
        for (i in 0..29) {
            val sx = ox + w * 0.04f + w * 0.92f * Fx.rnd(i * 7919L)
            val sy = oy + h * 0.02f + h * 0.55f * Fx.rnd(i * 6271L)
            val twinkle = abs(sin(((secs * 0.6f + i * 0.28f) % 1.0f) * PI.toFloat()))
            val starAlpha = ((0.20f + 0.55f * twinkle) * alpha / 255f).coerceIn(0f, 1f)
            val size = 0.8f + Fx.rnd(i * 3331L) * 0.8f
            p.color = Color.argb((255 * starAlpha).toInt(), 0xFF, 0xFF, 0xFF)
            p.maskFilter = null
            canvas.drawCircle(sx, sy, size * u, p)
        }
    }

    // ── Clouds ───────────────────────────────────────────────────────────────

    private fun drawClouds(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                           s: Float, secs: Float, kind: String, alpha: Int) {
        val cloudAlpha = when (kind) {
            "partly" -> 0.55f; "cloud" -> 0.75f; else -> 0.68f
        }
        val rgb = if (kind == "storm") Triple(0x3D, 0x3D, 0x55) else Triple(0xCC, 0xCC, 0xCC)
        val darkRgb = if (kind == "storm") Triple(0x2A, 0x2A, 0x3D) else Triple(0xAA, 0xAA, 0xAA)

        p.maskFilter = blur4

        for (ci in 0..3) {
            val speed = 0.008f + Fx.rnd(ci * 4027L) * 0.010f
            val baseX = w * Fx.rnd(ci * 7013L)
            val cx = ox + ((baseX + w * secs * speed) % (w * 1.4f)) - w * 0.2f
            val cy = oy + h * (0.05f + 0.45f * Fx.rnd(ci * 5839L))
            val cr = s * (0.040f + Fx.rnd(ci * 9001L) * 0.025f)

            val a = (255 * cloudAlpha * alpha / 255f).toInt()

            p.color = Color.argb((a * 0.3f).toInt(), darkRgb.first, darkRgb.second, darkRgb.third)
            drawCloudShape(canvas, cx, cy + cr * 0.15f, cr * 1.05f)

            p.color = Color.argb(a, rgb.first, rgb.second, rgb.third)
            drawCloudShape(canvas, cx, cy, cr)
        }

        p.maskFilter = null
    }

    private fun drawCloudShape(canvas: Canvas, cx: Float, cy: Float, cr: Float) {
        canvas.drawOval(RectF(cx, cy, cx + cr * 3.0f, cy + cr * 1.1f), p)
        canvas.drawCircle(cx + cr * 0.5f, cy + cr * 0.05f, cr * 0.75f, p)
        canvas.drawCircle(cx + cr * 1.2f, cy - cr * 0.25f, cr * 0.85f, p)
        canvas.drawCircle(cx + cr * 2.0f, cy - cr * 0.10f, cr * 0.70f, p)
        canvas.drawCircle(cx + cr * 2.5f, cy + cr * 0.10f, cr * 0.55f, p)
    }

    // ── Fog bands ────────────────────────────────────────────────────────────

    private fun drawFog(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float, secs: Float) {
        p.maskFilter = null
        for (fi in 0..4) {
            val bandY = oy + h * (0.15f + fi * 0.15f)
            val xOff = (secs * 0.018f * (1 + fi * 0.7f)) % 1.4f - 0.2f
            val bx = ox + xOff * w
            val bandH = h * 0.10f
            val fogAlpha = 0.22f + fi * 0.05f
            val fogColor = Color.argb((255 * fogAlpha).toInt(), 0xCC, 0xCC, 0xCC)
            p.shader = LinearGradient(
                bx, bandY, bx + w, bandY,
                intArrayOf(Color.TRANSPARENT, fogColor, fogColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bx, bandY, bx + w, bandY + bandH, p)
            p.shader = null
        }
    }

    // ── Rain ─────────────────────────────────────────────────────────────────

    private fun drawRain(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                         secs: Float, u: Float, alpha: Int) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.maskFilter = null

        for (i in 0..54) {
            val speed = 0.85f + Fx.rnd(i * 7331L) * 0.55f
            val fy = (secs * speed * 0.32f + Fx.rnd(i * 4481L)) % 1.15f
            val rx = ox + w * Fx.rnd(i * 1193L)
            val ry = oy + h * fy
            val len = h * (0.025f + Fx.rnd(i * 2801L) * 0.020f)
            val dropAlpha = 0.30f + Fx.rnd(i * 5501L) * 0.35f
            p.strokeWidth = (0.6f + Fx.rnd(i * 3701L) * 0.6f) * u
            p.color = Color.argb((255 * dropAlpha * alpha / 255f).toInt(), 0x88, 0xBB, 0xDD)
            canvas.drawLine(rx, ry, rx - len * 0.35f, ry + len, p)
        }

        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    // ── Snow ─────────────────────────────────────────────────────────────────

    private fun drawSnow(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                         secs: Float, u: Float, alpha: Int) {
        p.maskFilter = null
        for (i in 0..44) {
            val speed = 0.12f + Fx.rnd(i * 5527L) * 0.18f
            val fy = (secs * speed * 0.22f + Fx.rnd(i * 8191L)) % 1.1f
            val fx = Fx.rnd(i * 2311L) + sin(secs * 1.4f + i * 0.7f).toFloat() * 0.028f
            val flakeAlpha = 0.45f + Fx.rnd(i * 4201L) * 0.40f
            val size = 1.2f + Fx.rnd(i * 6101L) * 1.8f
            p.color = Color.argb((255 * flakeAlpha * alpha / 255f).toInt(), 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(ox + w * fx, oy + h * fy, size * u, p)
        }
    }

    // ── Lightning ────────────────────────────────────────────────────────────

    private fun drawLightning(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                               secs: Float, u: Float, alpha: Int) {
        val flashT = secs % 5.2f
        if (flashT >= 0.35f) return

        val flashAlpha = ((1f - flashT / 0.35f) * 0.80f * alpha / 255f).coerceIn(0f, 1f)

        p.maskFilter = null
        p.color = Color.argb((255 * flashAlpha * 0.08f).toInt(), 0xFF, 0xFF, 0xFF)
        canvas.drawRect(ox, oy, ox + w, oy + h, p)

        p.color = Color.argb((255 * flashAlpha).toInt(), 0xFF, 0xFF, 0xBB)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.0f * u
        p.strokeCap = Paint.Cap.ROUND
        p.maskFilter = blur4
        val lx = ox + w * 0.63f
        val ly = oy + h * 0.06f
        path.reset()
        path.moveTo(lx, ly)
        path.lineTo(lx - 8 * u, ly + h * 0.12f)
        path.lineTo(lx + 4 * u, ly + h * 0.16f)
        path.lineTo(lx - 12 * u, ly + h * 0.30f)
        path.lineTo(lx - 2 * u, ly + h * 0.24f)
        path.lineTo(lx - 16 * u, ly + h * 0.42f)
        canvas.drawPath(path, p)
        p.maskFilter = null
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }
}
