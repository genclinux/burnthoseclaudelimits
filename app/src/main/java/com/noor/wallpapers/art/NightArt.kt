package com.noor.wallpapers.art

import kotlin.random.Random

/** Procedural mosque silhouettes under a night or dusk sky. */
object NightArt {
    enum class Architecture { OTTOMAN, PERSIAN, MOGHUL }
    enum class Sky { NIGHT, DUSK, DAWN }

    class Params(
        val architecture: Architecture,
        val sky: Sky,
        val water: Boolean = true,
    )

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val ground = ctx.y(if (p.water) 0.70 else 0.82)
        val rnd = ctx.random(3)

        // Sky
        val horizon = when (p.sky) {
            Sky.NIGHT -> Colors.mix(pal.bgTop, pal.accentA, 0.35f)
            Sky.DUSK -> Colors.mix(pal.accentC, Colors.hex("#E07A3A"), 0.55f)
            Sky.DAWN -> Colors.mix(pal.glow, Colors.hex("#F2A6A0"), 0.4f)
        }
        val top = Colors.darken(pal.bgBottom, 0.35f)
        val mid = if (p.sky == Sky.NIGHT) pal.bgBottom else Colors.mix(pal.bgTop, pal.accentA, 0.3f)
        b.fill(
            Path().rect(0, 0, ctx.w, ground),
            LinearFill(0f, 0f, 0f, ground.toFloat(), intArrayOf(top, mid, horizon), floatArrayOf(0f, 0.55f, 1f)),
        )
        Common.glow(b, ctx.cx, ground, maxOf(ctx.w, ctx.safeW) * 0.9, horizon, if (p.sky == Sky.NIGHT) 0.35f else 0.6f)
        val starCount = when (p.sky) { Sky.NIGHT -> 260; Sky.DUSK -> 120; Sky.DAWN -> 60 }
        Common.starfield(b, ctx, 0.0, ground * 0.8, starCount)

        // Moon
        val moonX = ctx.x(0.22 + rnd.nextDouble() * 0.56)
        val moonY = ctx.y(0.16 + rnd.nextDouble() * 0.08)
        val moonR = 95 * ctx.u
        Common.glow(b, moonX, moonY, moonR * 4, pal.glow, 0.32f)
        b.fill(Common.crescent(moonX, moonY, moonR, rotation = -0.5 - rnd.nextDouble() * 0.6), Colors.lighten(pal.glow, 0.3f))

        // Distant hills / city haze for depth
        val haze = Colors.mix(top, horizon, 0.45f)
        b.fill(hills(ctx, ground, 70 * ctx.u, Random(ctx.seed + 5)), haze, 0.8f)

        // Mosque
        val silhouette = Colors.darken(pal.bgBottom, 0.55f)
        // Easter egg: Hanife Betül's initials as a constellation, opposite the moon.
        val hbSize = 70 * ctx.u
        val hbX = if (moonX < ctx.cx) ctx.x(0.70) else ctx.x(0.12)
        Common.initialsConstellation(b, ctx, hbX, ctx.y(0.30), hbSize)

        val (body, windows) = mosque(ctx, p.architecture, ctx.cx, ground, Random(ctx.seed + 9))
        b.fill(body, silhouette)
        b.fill(windows, pal.glow, 0.92f)

        if (p.water) {
            val waterTop = Colors.darken(horizon, 0.55f)
            b.fill(
                Path().rect(0, ground, ctx.w, ctx.h),
                LinearFill(
                    0f, ground.toFloat(), 0f, ctx.h.toFloat(),
                    intArrayOf(waterTop, Colors.darken(pal.bgBottom, 0.6f)),
                ),
            )
            val mirror: (Float, Float) -> Pair<Float, Float> = { x, y -> x to (2 * ground - y).toFloat() }
            b.group(clip = Path().rect(0, ground, ctx.w, ctx.h)) {
                fill(body.transformed(mirror), silhouette, 0.6f)
                fill(windows.transformed(mirror), pal.glow, 0.25f)
                // Moon path on the water.
                val streaks = Path()
                val r2 = Random(ctx.seed + 21)
                repeat(26) {
                    val y = ground + (ctx.h - ground) * (it / 26.0) + r2.nextDouble() * 8 * ctx.u
                    val half = (40 + r2.nextDouble() * 120) * ctx.u * (1 - it / 40.0)
                    streaks.rect(moonX - half, y, moonX + half, y + 3 * ctx.u)
                }
                fill(streaks, pal.glow, 0.18f)
            }
            b.fill(Path().rect(0, ground - 2 * ctx.u, ctx.w, ground + 2 * ctx.u), silhouette)
        } else {
            b.fill(Path().rect(0, ground, ctx.w, ctx.h), silhouette)
        }
        Common.vignette(b, ctx, 0.45f)
        return b.build()
    }

    private fun hills(ctx: RenderContext, ground: Double, amp: Double, rnd: Random): Path {
        val p = Path().moveTo(0, ground)
        var x = 0.0
        p.lineTo(0, ground - amp * rnd.nextDouble())
        while (x < ctx.w) {
            val nx = x + ctx.w * (0.15 + rnd.nextDouble() * 0.2)
            val ny = ground - amp * (0.3 + rnd.nextDouble() * 0.7)
            p.quadTo((x + nx) / 2, ny - amp * 0.4, nx, ny)
            x = nx
        }
        return p.lineTo(ctx.w, ground).close()
    }

    /** Returns (body, lit windows). Everything is in pixels, centred on [cx] standing on [ground]. */
    fun mosque(ctx: RenderContext, arch: Architecture, cx: Double, ground: Double, rnd: Random): Pair<Path, Path> {
        val u = ctx.u
        val body = Path()
        val windows = Path()
        val hallW = (560 + rnd.nextDouble() * 120) * u
        val hallH = (150 + rnd.nextDouble() * 40) * u
        val hallTop = ground - hallH
        body.rect(cx - hallW / 2, hallTop, cx + hallW / 2, ground)

        when (arch) {
            Architecture.OTTOMAN -> {
                // Cascading half domes around a broad central dome, pencil minarets.
                val mainR = hallW * 0.27
                val drumH = 60 * u
                body.rect(cx - mainR * 1.05, hallTop - drumH, cx + mainR * 1.05, hallTop)
                body.append(Shapes.hemiDome(cx, hallTop - drumH, mainR, mainR * 0.95))
                finial(body, cx, hallTop - drumH - mainR * 0.95, 70 * u, u)
                for (s in listOf(-1.0, 1.0)) {
                    val hx = cx + s * mainR * 1.25
                    body.append(Shapes.hemiDome(hx, hallTop, mainR * 0.55, mainR * 0.5))
                    val sx = cx + s * hallW * 0.40
                    body.append(Shapes.hemiDome(sx, hallTop, hallW * 0.08, hallW * 0.08))
                    finial(body, sx, hallTop - hallW * 0.08, 28 * u, u)
                }
                val count = if (rnd.nextBoolean()) 2 else 4
                for (i in 0 until count) {
                    val side = if (i % 2 == 0) -1.0 else 1.0
                    val off = hallW / 2 + (if (i < 2) 70 else 200) * u
                    val height = (if (i < 2) 760 else 620) * u
                    pencilMinaret(body, windows, cx + side * off, ground, height, 34 * u, u)
                }
                for (i in -3..3) arch(windows, cx + i * hallW * 0.12, ground - hallH * 0.25, 22 * u, 60 * u)
            }
            Architecture.PERSIAN -> {
                // Tall iwan portal flanked by minarets, turquoise-style onion dome behind.
                val domeR = hallW * 0.22
                val drumH = 120 * u
                body.rect(cx - domeR, hallTop - drumH, cx + domeR, hallTop)
                body.append(Shapes.onionDome(cx, hallTop - drumH, domeR * 1.05, domeR * 1.7))
                finial(body, cx, hallTop - drumH - domeR * 1.7, 60 * u, u)
                val iwanW = hallW * 0.42
                val iwanH = hallH + 230 * u
                body.rect(cx - iwanW / 2, ground - iwanH, cx + iwanW / 2, ground)
                windows.append(Shapes.pointedArch(cx, ground - 20 * u, iwanW * 0.5, iwanH * 0.66))
                for (s in listOf(-1.0, 1.0)) {
                    classicMinaret(body, windows, cx + s * (iwanW / 2 - 20 * u), ground, iwanH + 260 * u, 42 * u, u, onion = false)
                    for (i in 1..2) arch(windows, cx + s * (iwanW / 2 + i * 75 * u), ground - hallH * 0.3, 20 * u, 55 * u)
                }
            }
            Architecture.MOGHUL -> {
                // Raised plinth, bulbous central onion, chhatri kiosks and four corner minarets.
                val plinth = 60 * u
                body.rect(cx - hallW * 0.75, ground - plinth, cx + hallW * 0.75, ground)
                val domeR = hallW * 0.24
                body.rect(cx - domeR * 0.9, hallTop - 50 * u, cx + domeR * 0.9, hallTop)
                body.append(Shapes.onionDome(cx, hallTop - 50 * u, domeR * 1.1, domeR * 2.0))
                finial(body, cx, hallTop - 50 * u - domeR * 2.0, 80 * u, u)
                for (s in listOf(-1.0, 1.0)) {
                    val kx = cx + s * hallW * 0.32
                    body.rect(kx - 40 * u, hallTop - 60 * u, kx + 40 * u, hallTop)
                    body.append(Shapes.onionDome(kx, hallTop - 60 * u, 50 * u, 90 * u))
                    finial(body, kx, hallTop - 150 * u, 30 * u, u)
                    classicMinaret(body, windows, cx + s * hallW * 0.70, ground - plinth, 640 * u, 40 * u, u, onion = true)
                }
                windows.append(Shapes.pointedArch(cx, ground - plinth - 10 * u, 110 * u, hallH * 0.75))
                for (i in listOf(-2, -1, 1, 2)) arch(windows, cx + i * hallW * 0.13, ground - plinth - 20 * u, 22 * u, 70 * u)
            }
        }
        return body to windows
    }

    private fun arch(windows: Path, x: Double, bottom: Double, halfW: Double, h: Double) {
        windows.append(Shapes.pointedArch(x, bottom, halfW * 2, h))
    }

    private fun finial(body: Path, x: Double, y: Double, h: Double, u: Double) {
        body.rect(x - 3 * u, y - h, x + 3 * u, y + 2 * u)
        body.circle(x, y - h * 0.35, 7 * u)
        body.append(Common.crescent(x, y - h - 10 * u, 14 * u, rotation = -Math.PI / 2))
    }

    /** Slender Ottoman minaret with balconies and a needle cap. */
    private fun pencilMinaret(body: Path, windows: Path, x: Double, ground: Double, height: Double, w: Double, u: Double) {
        val top = ground - height
        body.rect(x - w / 2, top + height * 0.18, x + w / 2, ground)
        body.rect(x - w * 0.62, ground - 70 * u, x + w * 0.62, ground)
        for (f in listOf(0.42, 0.62, 0.80)) {
            val by = top + height * (1 - f)
            body.rect(x - w * 0.95, by, x + w * 0.95, by + 10 * u)
            body.moveTo(x - w * 0.95, by + 10 * u).lineTo(x + w * 0.95, by + 10 * u)
                .lineTo(x + w / 2, by + 28 * u).lineTo(x - w / 2, by + 28 * u).close()
            windows.rect(x - 4 * u, by + 40 * u, x + 4 * u, by + 60 * u)
        }
        body.moveTo(x - w * 0.55, top + height * 0.18).lineTo(x, top).lineTo(x + w * 0.55, top + height * 0.18).close()
        finial(body, x, top, 36 * u, u)
    }

    /** Thicker minaret with a lantern gallery and either an onion or a chhatri top. */
    private fun classicMinaret(
        body: Path, windows: Path, x: Double, ground: Double, height: Double, w: Double, u: Double, onion: Boolean,
    ) {
        val top = ground - height
        body.moveTo(x - w * 0.6, ground).lineTo(x - w * 0.45, top + 80 * u)
            .lineTo(x + w * 0.45, top + 80 * u).lineTo(x + w * 0.6, ground).close()
        for (f in listOf(0.45, 0.75)) {
            val by = ground - height * f
            body.rect(x - w * 0.9, by, x + w * 0.9, by + 14 * u)
            windows.rect(x - 5 * u, by + 34 * u, x + 5 * u, by + 58 * u)
        }
        body.rect(x - w * 0.6, top + 30 * u, x + w * 0.6, top + 84 * u)
        windows.rect(x - w * 0.3, top + 42 * u, x + w * 0.3, top + 76 * u)
        if (onion) body.append(Shapes.onionDome(x, top + 32 * u, w * 0.75, 70 * u))
        else body.append(Shapes.hemiDome(x, top + 32 * u, w * 0.62, w * 0.75))
        finial(body, x, top + 32 * u - (if (onion) 70 * u else w * 0.75), 26 * u, u)
    }
}

object Shapes {
    /** Ottoman hemispherical dome of radius [r] and height [h] sitting on ([cx], [base]). */
    fun hemiDome(cx: Double, base: Double, r: Double, h: Double): Path {
        val k = 0.5523
        return Path().moveTo(cx - r, base)
            .cubicTo(cx - r, base - h * k, cx - r * k, base - h, cx, base - h)
            .cubicTo(cx + r * k, base - h, cx + r, base - h * k, cx + r, base)
            .close()
    }

    /** Bulbous onion dome that swells past its base and tapers to a point. */
    fun onionDome(cx: Double, base: Double, r: Double, h: Double): Path = Path().moveTo(cx - r * 0.8, base)
        .cubicTo(cx - r * 1.25, base - h * 0.25, cx - r * 1.15, base - h * 0.62, cx - r * 0.45, base - h * 0.8)
        .cubicTo(cx - r * 0.15, base - h * 0.88, cx - r * 0.05, base - h * 0.94, cx, base - h)
        .cubicTo(cx + r * 0.05, base - h * 0.94, cx + r * 0.15, base - h * 0.88, cx + r * 0.45, base - h * 0.8)
        .cubicTo(cx + r * 1.15, base - h * 0.62, cx + r * 1.25, base - h * 0.25, cx + r * 0.8, base)
        .close()

    /** A pointed (Persian four-centred style) arch of full width [w] and height [h]. */
    fun pointedArch(cx: Double, bottom: Double, w: Double, h: Double): Path {
        val hw = w / 2
        val spring = bottom - h * 0.6
        return Path().moveTo(cx - hw, bottom).lineTo(cx - hw, spring)
            .cubicTo(cx - hw, spring - h * 0.25, cx - hw * 0.4, spring - h * 0.32, cx, bottom - h)
            .cubicTo(cx + hw * 0.4, spring - h * 0.32, cx + hw, spring - h * 0.25, cx + hw, spring)
            .lineTo(cx + hw, bottom).close()
    }
}
