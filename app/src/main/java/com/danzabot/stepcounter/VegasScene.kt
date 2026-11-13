package com.danzabot.stepcounter

import android.graphics.*
import kotlin.math.*

object VegasScene {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val blur4 = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    private val blur8 = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
    private val blur16 = BlurMaskFilter(48f, BlurMaskFilter.Blur.NORMAL)
    private val startNanos = System.nanoTime()

    fun draw(
        canvas: Canvas,
        ox: Float, oy: Float,
        w: Float, h: Float,
        u: Float,
        weatherKind: String,
        isNight: Boolean,
        alpha: Int = 255
    ) {
        val secs = (System.nanoTime() - startNanos) / 1_000_000_000f
        val s = min(w, h)

        drawSky(canvas, ox, oy, w, h, weatherKind, isNight, alpha)

        if (isNight && weatherKind != "fog") {
            drawStars(canvas, ox, oy, w, h, secs, alpha, weatherKind)
        }

        if (weatherKind == "cloud" || weatherKind == "partly" || weatherKind == "rain" || weatherKind == "storm") {
            drawClouds(canvas, ox, oy, w, h, s, secs, weatherKind, alpha)
        }

        drawLightPollution(canvas, ox, oy, w, h, alpha, weatherKind)
        drawLuxorBeam(canvas, ox, oy, w, h, secs, alpha, weatherKind)
        drawSkyline(canvas, ox, oy, w, h, u, secs, alpha, weatherKind)
        drawStrip(canvas, ox, oy, w, h, u, secs, alpha)
        drawForegroundGlow(canvas, ox, oy, w, h, alpha)

        if (weatherKind == "fog") drawFog(canvas, ox, oy, w, h, secs, alpha)
        if (weatherKind == "rain" || weatherKind == "storm") drawRain(canvas, ox, oy, w, h, secs, u, alpha)
        if (weatherKind == "snow") drawSnow(canvas, ox, oy, w, h, secs, u, alpha)
        if (weatherKind == "storm") drawLightningFlash(canvas, ox, oy, w, h, secs, alpha)
    }

    // ── Sky ──────────────────────────────────────────────────────────────────

    private fun drawSky(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                        kind: String, isNight: Boolean, alpha: Int) {
        val (top, bot) = when {
            kind == "storm" -> 0xFF0A0A18.toInt() to 0xFF1A1428.toInt()
            kind == "fog" -> 0xFF2A2535.toInt() to 0xFF3A3545.toInt()
            kind == "rain" -> 0xFF121225.toInt() to 0xFF1E1830.toInt()
            kind == "cloud" -> 0xFF151530.toInt() to 0xFF201A35.toInt()
            !isNight -> 0xFF1A2555.toInt() to 0xFF3A2A60.toInt()
            else -> 0xFF080818.toInt() to 0xFF151028.toInt()
        }
        p.shader = LinearGradient(ox, oy, ox, oy + h, top, bot, Shader.TileMode.CLAMP)
        p.maskFilter = null
        p.alpha = alpha
        canvas.drawRect(ox, oy, ox + w, oy + h, p)
        p.shader = null
        p.alpha = 255
    }

    // ── Stars ────────────────────────────────────────────────────────────────

    private fun drawStars(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                          secs: Float, alpha: Int, kind: String) {
        val starAlphaScale = when (kind) {
            "cloud", "partly" -> 0.3f
            "rain" -> 0.15f
            else -> 0.7f
        }
        p.maskFilter = null
        for (i in 0..24) {
            val sx = ox + w * 0.05f + w * 0.90f * Fx.rnd(i * 8311L)
            val sy = oy + h * 0.02f + h * 0.40f * Fx.rnd(i * 6577L)
            val twinkle = abs(sin(((secs * 0.5f + i * 0.3f) % 1f) * PI.toFloat()))
            val a = (255 * (0.15f + 0.5f * twinkle) * starAlphaScale * alpha / 255f).toInt().coerceIn(0, 255)
            val size = 0.6f + Fx.rnd(i * 3377L) * 0.7f
            p.color = Color.argb(a, 0xFF, 0xFF, 0xEE)
            canvas.drawCircle(sx, sy, size * resources(w), p)
        }
    }

    // ── Light pollution glow ─────────────────────────────────────────────────

    private fun drawLightPollution(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                                    alpha: Int, kind: String) {
        val glowAlpha = when (kind) {
            "fog" -> 0.35f
            "rain", "storm" -> 0.20f
            else -> 0.28f
        }
        val glowTop = oy + h * 0.25f
        val glowBot = oy + h * 0.45f
        p.maskFilter = blur16
        p.shader = LinearGradient(ox, glowTop, ox, glowBot,
            Color.argb((255 * glowAlpha * alpha / 255f).toInt(), 0xFF, 0x99, 0x44),
            Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(ox, glowTop, ox + w, glowBot, p)
        p.shader = null
        p.maskFilter = null
    }

    // ── Luxor beam ───────────────────────────────────────────────────────────

    private fun drawLuxorBeam(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                               secs: Float, alpha: Int, kind: String) {
        val beamAlpha = when (kind) {
            "fog" -> 0.20f
            "cloud", "rain" -> 0.30f
            "storm" -> 0.15f
            else -> 0.40f
        }
        val beamX = ox + w * 0.18f
        val beamBottom = oy + h * 0.42f
        val sway = sin(secs * 0.15f) * w * 0.008f

        p.maskFilter = blur8
        val topW = w * 0.035f
        val botW = w * 0.008f
        path.reset()
        path.moveTo(beamX - botW + sway, beamBottom)
        path.lineTo(beamX - topW + sway * 0.3f, oy)
        path.lineTo(beamX + topW + sway * 0.3f, oy)
        path.lineTo(beamX + botW + sway, beamBottom)
        path.close()
        p.color = Color.argb((255 * beamAlpha * alpha / 255f).toInt(), 0xFF, 0xEE, 0x88)
        canvas.drawPath(path, p)
        p.maskFilter = null
    }

    // ── Skyline ──────────────────────────────────────────────────────────────

    private fun drawSkyline(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                             u: Float, secs: Float, alpha: Int, kind: String) {
        val baseY = oy + h * 0.42f
        val fogDim = if (kind == "fog") 0.5f else 1f

        // Luxor pyramid
        drawLuxor(canvas, ox, w, h, baseY, secs, alpha, fogDim)

        // Background hotel blocks
        drawHotelBlock(canvas, ox + w * 0.30f, baseY, w * 0.10f, h * 0.18f, secs, 0xCC9944, 0.6f * fogDim, alpha, 1001L)
        drawHotelBlock(canvas, ox + w * 0.42f, baseY, w * 0.08f, h * 0.22f, secs, 0x4488CC, 0.7f * fogDim, alpha, 2002L)

        // Tall curved tower (Wynn-like)
        drawWynnTower(canvas, ox, w, h, baseY, secs, alpha, fogDim)

        // Stratosphere needle
        drawStratosphere(canvas, ox, w, h, baseY, secs, alpha, fogDim)

        // Foreground hotels
        drawHotelBlock(canvas, ox + w * 0.55f, baseY, w * 0.12f, h * 0.16f, secs, 0xDD5599, 0.8f * fogDim, alpha, 3003L)
        drawHotelBlock(canvas, ox + w * 0.70f, baseY, w * 0.14f, h * 0.20f, secs, 0x44BBAA, 0.7f * fogDim, alpha, 4004L)
        drawHotelBlock(canvas, ox + w * 0.86f, baseY, w * 0.10f, h * 0.14f, secs, 0xEEAA33, 0.65f * fogDim, alpha, 5005L)

        // Marquee lights on rooflines
        drawMarqueeLights(canvas, ox, w, baseY, secs, u, alpha, fogDim)

        // Neon signs
        drawNeonSigns(canvas, ox, w, h, baseY, secs, u, alpha, fogDim)
    }

    private fun drawLuxor(canvas: Canvas, ox: Float, w: Float, h: Float, baseY: Float,
                           secs: Float, alpha: Int, fogDim: Float) {
        val cx = ox + w * 0.18f
        val pyramidW = w * 0.14f
        val pyramidH = h * 0.16f
        val top = baseY - pyramidH

        // Pyramid body
        p.maskFilter = null
        p.color = Color.argb((255 * 0.85f * fogDim * alpha / 255f).toInt(), 0x1A, 0x18, 0x22)
        path.reset()
        path.moveTo(cx, top)
        path.lineTo(cx - pyramidW / 2, baseY)
        path.lineTo(cx + pyramidW / 2, baseY)
        path.close()
        canvas.drawPath(path, p)

        // Gold edge glow
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.maskFilter = blur4
        val edgeAlpha = (0.5f + 0.2f * sin(secs * 0.8f)) * fogDim
        p.color = Color.argb((255 * edgeAlpha * alpha / 255f).toInt(), 0xFF, 0xCC, 0x44)
        canvas.drawPath(path, p)
        p.style = Paint.Style.FILL
        p.maskFilter = null
    }

    private fun drawWynnTower(canvas: Canvas, ox: Float, w: Float, h: Float, baseY: Float,
                               secs: Float, alpha: Int, fogDim: Float) {
        val cx = ox + w * 0.52f
        val towerW = w * 0.06f
        val towerH = h * 0.28f
        val top = baseY - towerH

        // Tower body - slight curve via rounded rect
        p.maskFilter = null
        p.color = Color.argb((255 * 0.75f * fogDim * alpha / 255f).toInt(), 0x1E, 0x1A, 0x28)
        val rect = RectF(cx - towerW / 2, top, cx + towerW / 2, baseY)
        canvas.drawRoundRect(rect, towerW * 0.3f, towerW * 0.1f, p)

        // Color wash
        val washColor = when (((secs * 0.15f).toInt()) % 3) {
            0 -> 0xCC4488
            1 -> 0x44AACC
            else -> 0xBB8833
        }
        val r = (washColor shr 16) and 0xFF
        val g = (washColor shr 8) and 0xFF
        val b = washColor and 0xFF
        p.maskFilter = blur8
        p.color = Color.argb((255 * 0.18f * fogDim * alpha / 255f).toInt(), r, g, b)
        canvas.drawRoundRect(rect, towerW * 0.3f, towerW * 0.1f, p)
        p.maskFilter = null

        // Windows
        drawWindowGrid(canvas, cx - towerW * 0.35f, top + towerH * 0.08f,
            towerW * 0.7f, towerH * 0.85f, 3, 14, secs, alpha * fogDim, 6006L)
    }

    private fun drawStratosphere(canvas: Canvas, ox: Float, w: Float, h: Float, baseY: Float,
                                  secs: Float, alpha: Int, fogDim: Float) {
        val cx = ox + w * 0.78f
        val needleH = h * 0.32f
        val top = baseY - needleH

        // Thin shaft
        p.maskFilter = null
        p.color = Color.argb((255 * 0.7f * fogDim * alpha / 255f).toInt(), 0x33, 0x33, 0x44)
        p.strokeWidth = w * 0.006f
        p.strokeCap = Paint.Cap.ROUND
        p.style = Paint.Style.STROKE
        canvas.drawLine(cx, baseY, cx, top + needleH * 0.15f, p)
        p.style = Paint.Style.FILL

        // Observation deck
        val deckY = top + needleH * 0.15f
        val deckW = w * 0.035f
        p.color = Color.argb((255 * 0.8f * fogDim * alpha / 255f).toInt(), 0x28, 0x28, 0x3A)
        canvas.drawRoundRect(RectF(cx - deckW, deckY, cx + deckW, deckY + needleH * 0.08f),
            deckW * 0.3f, deckW * 0.3f, p)

        // Spike above deck
        p.strokeWidth = w * 0.003f
        p.style = Paint.Style.STROKE
        canvas.drawLine(cx, deckY, cx, top, p)
        p.style = Paint.Style.FILL

        // Blinking top light
        val blink = if ((secs * 1.5f).toInt() % 2 == 0) 1f else 0.3f
        p.maskFilter = blur4
        p.color = Color.argb((255 * blink * fogDim * alpha / 255f).toInt(), 0xFF, 0x33, 0x33)
        canvas.drawCircle(cx, top, w * 0.005f, p)
        p.maskFilter = null
    }

    private fun drawHotelBlock(canvas: Canvas, x: Float, baseY: Float, bw: Float, bh: Float,
                                secs: Float, neonColor: Int, brightness: Float, alpha: Int, seed: Long) {
        val top = baseY - bh
        p.maskFilter = null
        p.color = Color.argb((255 * 0.8f * brightness * alpha / 255f).toInt(), 0x18, 0x16, 0x22)
        canvas.drawRect(x, top, x + bw, baseY, p)

        // Neon accent strip at top
        val r = (neonColor shr 16) and 0xFF
        val g = (neonColor shr 8) and 0xFF
        val b = neonColor and 0xFF
        p.maskFilter = blur4
        val pulse = 0.5f + 0.3f * sin(secs * 0.7f + seed * 0.001f)
        p.color = Color.argb((255 * pulse * brightness * alpha / 255f).toInt(), r, g, b)
        canvas.drawRect(x, top, x + bw, top + bh * 0.03f, p)
        p.maskFilter = null

        // Windows
        drawWindowGrid(canvas, x + bw * 0.08f, top + bh * 0.06f,
            bw * 0.84f, bh * 0.88f, 4, 8, secs, alpha * brightness, seed)
    }

    private fun drawWindowGrid(canvas: Canvas, x: Float, y: Float, gw: Float, gh: Float,
                                cols: Int, rows: Int, secs: Float, alphaF: Float, seed: Long) {
        val cellW = gw / cols
        val cellH = gh / rows
        val winW = cellW * 0.5f
        val winH = cellH * 0.4f
        p.maskFilter = null
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val hash = Fx.rnd(seed + r * 97L + c * 31L)
                val on = hash > 0.25f
                if (!on) continue
                val flicker = if (Fx.rnd(seed + r * 53L + c * 17L + (secs * 0.1f).toLong()) > 0.95f) 0.3f else 1f
                val warmth = Fx.rnd(seed + r * 71L + c * 41L)
                val rr = (200 + warmth * 55).toInt()
                val gg = (180 + warmth * 40).toInt()
                val bb = (100 + warmth * 60).toInt()
                val a = (alphaF * 0.6f * flicker).toInt().coerceIn(0, 255)
                p.color = Color.argb(a, rr, gg, bb)
                val wx = x + c * cellW + (cellW - winW) / 2
                val wy = y + r * cellH + (cellH - winH) / 2
                canvas.drawRect(wx, wy, wx + winW, wy + winH, p)
            }
        }
    }

    // ── Marquee lights ───────────────────────────────────────────────────────

    private fun drawMarqueeLights(canvas: Canvas, ox: Float, w: Float, baseY: Float,
                                   secs: Float, u: Float, alpha: Int, fogDim: Float) {
        val rooflines = floatArrayOf(
            ox + w * 0.30f, ox + w * 0.40f, baseY - w * 0.18f * (1440f / w).coerceIn(0.8f, 1.2f),
            ox + w * 0.55f, ox + w * 0.67f, baseY - w * 0.16f * (1440f / w).coerceIn(0.8f, 1.2f),
            ox + w * 0.70f, ox + w * 0.84f, baseY - w * 0.20f * (1440f / w).coerceIn(0.8f, 1.2f),
        )
        p.maskFilter = null
        for (ri in rooflines.indices step 3) {
            val rx1 = rooflines[ri]
            val rx2 = rooflines[ri + 1]
            val ry = rooflines[ri + 2]
            val span = rx2 - rx1
            val dotCount = (span / (6f * u)).toInt().coerceIn(4, 20)
            val phase = (secs * 4f) % dotCount
            for (di in 0 until dotCount) {
                val brightness = if (((di + phase.toInt()) % 3) == 0) 1f else 0.25f
                val dx = rx1 + span * di / dotCount
                val a = (255 * brightness * fogDim * alpha / 255f).toInt().coerceIn(0, 255)
                p.color = Color.argb(a, 0xFF, 0xEE, 0xDD)
                canvas.drawCircle(dx, ry, 1.5f * u, p)
            }
        }
    }

    // ── Neon signs ───────────────────────────────────────────────────────────

    private fun drawNeonSigns(canvas: Canvas, ox: Float, w: Float, h: Float, baseY: Float,
                               secs: Float, u: Float, alpha: Int, fogDim: Float) {
        data class Sign(val x: Float, val y: Float, val sw: Float, val sh: Float,
                        val colors: List<Int>, val speed: Float)

        val signs = listOf(
            Sign(ox + w * 0.33f, baseY - h * 0.10f, w * 0.06f, h * 0.025f,
                listOf(0xFFFF3388.toInt(), 0xFF3388FF.toInt(), 0xFFFFCC33.toInt()), 0.25f),
            Sign(ox + w * 0.60f, baseY - h * 0.08f, w * 0.07f, h * 0.02f,
                listOf(0xFF33FFAA.toInt(), 0xFFFF5533.toInt(), 0xFF8833FF.toInt()), 0.18f),
            Sign(ox + w * 0.75f, baseY - h * 0.12f, w * 0.05f, h * 0.022f,
                listOf(0xFFFFAA33.toInt(), 0xFF33AAFF.toInt(), 0xFFFF33AA.toInt()), 0.22f),
        )

        for (sign in signs) {
            val colorIdx = ((secs * sign.speed) % sign.colors.size).toInt()
            val nextIdx = (colorIdx + 1) % sign.colors.size
            val blend = (secs * sign.speed) % 1f
            val c1 = sign.colors[colorIdx]
            val c2 = sign.colors[nextIdx]
            val r = ((Color.red(c1) * (1 - blend) + Color.red(c2) * blend)).toInt()
            val g = ((Color.green(c1) * (1 - blend) + Color.green(c2) * blend)).toInt()
            val b = ((Color.blue(c1) * (1 - blend) + Color.blue(c2) * blend)).toInt()

            p.maskFilter = blur4
            val a = (255 * 0.7f * fogDim * alpha / 255f).toInt().coerceIn(0, 255)
            p.color = Color.argb(a, r, g, b)
            canvas.drawRoundRect(RectF(sign.x, sign.y, sign.x + sign.sw, sign.y + sign.sh),
                sign.sh * 0.3f, sign.sh * 0.3f, p)
            p.maskFilter = null
        }
    }

    // ── The Strip road ───────────────────────────────────────────────────────

    private fun drawStrip(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                           u: Float, secs: Float, alpha: Int) {
        val roadTop = oy + h * 0.44f
        val roadBot = oy + h * 0.52f

        // Road surface
        p.maskFilter = null
        p.color = Color.argb((255 * 0.9f * alpha / 255f).toInt(), 0x1A, 0x1A, 0x22)
        canvas.drawRect(ox, roadTop, ox + w, roadBot, p)

        // Center line dashes
        val lineY = (roadTop + roadBot) / 2
        p.color = Color.argb((255 * 0.3f * alpha / 255f).toInt(), 0xFF, 0xFF, 0x55)
        p.strokeWidth = 1.5f * u
        p.strokeCap = Paint.Cap.ROUND
        p.style = Paint.Style.STROKE
        val dashLen = w * 0.03f
        val gapLen = w * 0.025f
        val offset = (secs * w * 0.02f) % (dashLen + gapLen)
        var dx = ox - offset
        while (dx < ox + w) {
            canvas.drawLine(dx, lineY, dx + dashLen, lineY, p)
            dx += dashLen + gapLen
        }
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT

        // Car lights
        for (i in 0..11) {
            val speed = 0.04f + Fx.rnd(i * 8123L) * 0.06f
            val goingRight = i % 2 == 0
            val laneY = if (goingRight) lineY + (roadBot - lineY) * 0.5f else lineY - (lineY - roadTop) * 0.5f
            val cx = if (goingRight)
                ox + ((Fx.rnd(i * 3311L) + secs * speed) % 1.2f) * w - w * 0.1f
            else
                ox + w - ((Fx.rnd(i * 4411L) + secs * speed) % 1.2f) * w + w * 0.1f

            if (cx < ox || cx > ox + w) continue

            p.maskFilter = blur4
            val isTaxi = Fx.rnd(i * 9977L) > 0.85f
            if (goingRight) {
                // Headlights (white)
                p.color = Color.argb((255 * 0.8f * alpha / 255f).toInt(), 0xFF, 0xFF, 0xFF)
                canvas.drawCircle(cx, laneY, 2.5f * u, p)
                canvas.drawCircle(cx - 5f * u, laneY, 2f * u, p)
            } else {
                // Taillights (red) or taxi (yellow)
                if (isTaxi) {
                    p.color = Color.argb((255 * 0.9f * alpha / 255f).toInt(), 0xFF, 0xDD, 0x33)
                    canvas.drawCircle(cx, laneY, 3f * u, p)
                } else {
                    p.color = Color.argb((255 * 0.8f * alpha / 255f).toInt(), 0xFF, 0x22, 0x22)
                    canvas.drawCircle(cx, laneY, 2.2f * u, p)
                    canvas.drawCircle(cx + 5f * u, laneY, 1.8f * u, p)
                }
            }
            p.maskFilter = null
        }
    }

    // ── Foreground neon glow ─────────────────────────────────────────────────

    private fun drawForegroundGlow(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float, alpha: Int) {
        val glowTop = oy + h * 0.50f
        p.maskFilter = blur16
        p.shader = LinearGradient(ox, glowTop, ox, oy + h,
            Color.argb((255 * 0.12f * alpha / 255f).toInt(), 0xFF, 0xAA, 0x55),
            Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(ox, glowTop, ox + w, oy + h, p)
        p.shader = null
        p.maskFilter = null
    }

    // ── Weather overlays (reusing WeatherScene patterns) ─────────────────────

    private fun drawClouds(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                            s: Float, secs: Float, kind: String, alpha: Int) {
        val cloudAlpha = when (kind) {
            "partly" -> 0.35f; "cloud" -> 0.50f; else -> 0.40f
        }
        p.maskFilter = blur4
        for (ci in 0..2) {
            val speed = 0.006f + Fx.rnd(ci * 4027L) * 0.008f
            val baseX = w * Fx.rnd(ci * 7013L)
            val cx = ox + ((baseX + w * secs * speed) % (w * 1.4f)) - w * 0.2f
            val cy = oy + h * (0.08f + 0.30f * Fx.rnd(ci * 5839L))
            val cr = s * (0.035f + Fx.rnd(ci * 9001L) * 0.020f)
            val a = (255 * cloudAlpha * alpha / 255f).toInt()
            p.color = Color.argb(a, 0x44, 0x40, 0x55)
            canvas.drawOval(RectF(cx, cy, cx + cr * 3f, cy + cr * 1.1f), p)
            canvas.drawCircle(cx + cr * 0.5f, cy + cr * 0.05f, cr * 0.75f, p)
            canvas.drawCircle(cx + cr * 1.2f, cy - cr * 0.25f, cr * 0.85f, p)
            canvas.drawCircle(cx + cr * 2f, cy - cr * 0.1f, cr * 0.7f, p)
        }
        p.maskFilter = null
    }

    private fun drawFog(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                         secs: Float, alpha: Int) {
        p.maskFilter = null
        for (fi in 0..4) {
            val bandY = oy + h * (0.30f + fi * 0.12f)
            val xOff = (secs * 0.015f * (1 + fi * 0.5f)) % 1.4f - 0.2f
            val bx = ox + xOff * w
            val bandH = h * 0.08f
            val fogAlpha = (0.18f + fi * 0.04f) * alpha / 255f
            val fogColor = Color.argb((255 * fogAlpha).toInt(), 0x88, 0x77, 0x99)
            p.shader = LinearGradient(bx, bandY, bx + w, bandY,
                intArrayOf(Color.TRANSPARENT, fogColor, fogColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(bx, bandY, bx + w, bandY + bandH, p)
            p.shader = null
        }
    }

    private fun drawRain(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                          secs: Float, u: Float, alpha: Int) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.maskFilter = null
        for (i in 0..49) {
            val speed = 0.85f + Fx.rnd(i * 7331L) * 0.55f
            val fy = (secs * speed * 0.32f + Fx.rnd(i * 4481L)) % 1.15f
            val rx = ox + w * Fx.rnd(i * 1193L)
            val ry = oy + h * fy
            val len = h * (0.020f + Fx.rnd(i * 2801L) * 0.015f)
            p.strokeWidth = (0.5f + Fx.rnd(i * 3701L) * 0.5f) * u
            p.color = Color.argb((255 * 0.35f * alpha / 255f).toInt(), 0x88, 0xAA, 0xCC)
            canvas.drawLine(rx, ry, rx - len * 0.3f, ry + len, p)
        }
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawSnow(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                          secs: Float, u: Float, alpha: Int) {
        p.maskFilter = null
        for (i in 0..39) {
            val speed = 0.10f + Fx.rnd(i * 5527L) * 0.15f
            val fy = (secs * speed * 0.20f + Fx.rnd(i * 8191L)) % 1.1f
            val fx = Fx.rnd(i * 2311L) + sin(secs * 1.2f + i * 0.6f).toFloat() * 0.025f
            val size = 1.0f + Fx.rnd(i * 6101L) * 1.6f
            p.color = Color.argb((255 * 0.5f * alpha / 255f).toInt(), 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(ox + w * fx, oy + h * fy, size * u, p)
        }
    }

    private fun drawLightningFlash(canvas: Canvas, ox: Float, oy: Float, w: Float, h: Float,
                                    secs: Float, alpha: Int) {
        val flashT = secs % 5.2f
        if (flashT >= 0.30f) return
        val flashAlpha = ((1f - flashT / 0.30f) * 0.6f * alpha / 255f).coerceIn(0f, 1f)
        p.maskFilter = null
        p.color = Color.argb((255 * flashAlpha * 0.12f).toInt(), 0xFF, 0xEE, 0xDD)
        canvas.drawRect(ox, oy, ox + w, oy + h, p)
    }

    private fun resources(w: Float): Float = w / 1440f
}
