package com.noor.wallpapers.art

import kotlin.math.PI

/** Ramadan and Eid: hanging fanous lanterns, a crescent and greeting calligraphy. */
object LanternArt {
    /** Half the tallest the greeting calligraphy may be, in design pixels. */
    private const val CALLIGRAPHY_HALF_HEIGHT = 110.0

    class Params(val phrase: Phrase, val script: CalligraphyArt.Script = CalligraphyArt.Script.RUQAA)

    /**
     * Positions as (x fraction of the safe area, hang length fraction, size in design px).
     * Extra lanterns beyond 0..1 only show where the canvas is wider than the
     * safe area, such as a tablet held sideways.
     */
    fun layout(ctx: RenderContext): List<Triple<Double, Double, Double>> {
        val rnd = ctx.random(31)
        val count = 3 + rnd.nextInt(3)
        val main = (0 until count).map { i ->
            val slot = (i + 0.5) / count
            val x = slot + (rnd.nextDouble() - 0.5) * 0.08
            val len = 0.10 + rnd.nextDouble() * 0.22
            val size = 150 + rnd.nextDouble() * 90
            Triple(x, len, size)
        }
        val extra = listOf(-0.32, -0.12, 1.12, 1.32).map { x ->
            Triple(x + (rnd.nextDouble() - 0.5) * 0.06, 0.06 + rnd.nextDouble() * 0.2, 130 + rnd.nextDouble() * 80)
        }
        return main + extra
    }

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        Common.background(b, ctx)
        val backdrop = GeometricArt.Params(Tiling.DODECAGON_TRIANGLE, PatternStyle.LINEWORK, edge = 70.0)
        b.group(alpha = 0.18f) { GeometricArt.drawPattern(this, ctx, backdrop, GeometricArt.tiles(ctx, backdrop)) }
        Common.starfield(b, ctx, 0.0, ctx.h * 0.75, 140, salt = 41)

        // A tablet's square safe area is much shorter than a phone screen, so tighten the layout.
        val square = ctx.safeH < ctx.safeW * 1.5
        val lanterns = layout(ctx)
        // On a tablet the moon shares the lanterns' band, so hang it in the widest gap between them.
        val moonX = ctx.x(if (square) widestGap(lanterns.map { it.first }.filter { it in 0.0..1.0 }) else 0.76)
        val moonY = ctx.y(if (square) 0.30 else 0.42)
        Common.glow(b, moonX, moonY, 300 * ctx.u, pal.glow, 0.3f)
        b.fill(Common.crescent(moonX, moonY, 120 * ctx.u, rotation = -0.9), pal.line)
        b.fill(Common.star(moonX + 10 * ctx.u, moonY + 20 * ctx.u, 34 * ctx.u, 14 * ctx.u, 5), pal.glow)

        for ((fx, len, size) in lanterns) {
            val x = ctx.x(fx)
            if (x < -size * ctx.u || x > ctx.w + size * ctx.u) continue
            lantern(b, ctx, x, ctx.y(if (square) len * 0.7 else len), size * ctx.u)
        }

        // Easter egg: "HB" among the stars, top left of the safe area.
        Common.initialsConstellation(b, ctx, ctx.x(0.08), ctx.y(0.36), 60 * ctx.u, alpha = 0.85f)

        val ground = ctx.safeTop + ctx.safeH
        val (body, windows) = NightArt.mosque(ctx, NightArt.Architecture.OTTOMAN, ctx.cx, ground, ctx.random(57))
        b.fill(body, Colors.darken(pal.bgBottom, 0.6f), 0.9f)
        // Below the safe area (a tablet held upright), let the ground fade back into the night.
        if (ctx.h > ground) {
            b.fill(
                Path().rect(0, ground, ctx.w, ctx.h),
                LinearFill(
                    0f, ground.toFloat(), 0f, ctx.h.toFloat(),
                    intArrayOf(Colors.darken(pal.bgBottom, 0.6f), Colors.withAlpha(Colors.darken(pal.bgBottom, 0.6f), 0.35f)),
                ),
                0.9f,
            )
        }
        b.fill(windows, pal.glow, 0.6f)

        val font = when (p.script) {
            CalligraphyArt.Script.NASKH -> FontId.NASKH_BOLD
            CalligraphyArt.Script.RUQAA -> FontId.RUQAA
            CalligraphyArt.Script.KUFI -> FontId.KUFI
        }
        val text = if (p.script == CalligraphyArt.Script.NASKH) p.phrase.arabic else p.phrase.bare
        val ty = ctx.y(if (square) 0.52 else 0.56)
        b.text(
            TextItem(
                text, font, (210 * ctx.u).toFloat(), ctx.cx.toFloat(), ty.toFloat(),
                LinearFill(
                    0f, (ty - 100 * ctx.u).toFloat(), 0f, (ty + 100 * ctx.u).toFloat(),
                    intArrayOf(Colors.lighten(pal.line, 0.4f), pal.line),
                ),
                maxWidth = (ctx.safeW * 0.82).toFloat(),
                glowColor = Colors.withAlpha(pal.glow, 0.6f), glowRadius = (20 * ctx.u).toFloat(),
                maxHeight = (2 * CALLIGRAPHY_HALF_HEIGHT * ctx.u).toFloat(),
                inkCentered = true,
            ),
        )
        if (ctx.options.captions) {
            b.text(
                TextItem(
                    p.phrase.meaning.uppercase(java.util.Locale.forLanguageTag("tr")), FontId.LATIN, (38 * ctx.u).toFloat(), ctx.cx.toFloat(),
                    // The calligraphy's ink ends at ty + CALLIGRAPHY_HALF_HEIGHT; leave a clear gap below it.
                    (ty + (CALLIGRAPHY_HALF_HEIGHT + 55) * ctx.u).toFloat(), SolidFill(Colors.lighten(pal.line, 0.3f)),
                    alpha = 0.9f, letterSpacing = 0.25f,
                ),
            )
        }
        Textures.grainOverlay(b, ctx, 0.35f)
        Common.vignette(b, ctx, 0.4f)
        return b.build()
    }

    /** Centre of the widest horizontal gap between lanterns at [xs], within 0.08..0.92. */
    private fun widestGap(xs: List<Double>): Double {
        val edges = (listOf(0.08) + xs.sorted() + listOf(0.92))
        return edges.zipWithNext().maxBy { (a, b) -> b - a }.let { (a, b) -> (a + b) / 2 }
    }

    /** A fanous: ring, domed cap, three visible glass panes, banded base and a pointed foot. */
    fun lantern(b: SceneBuilder, ctx: RenderContext, x: Double, top: Double, s: Double) {
        val pal = ctx.palette
        val u = ctx.u
        val frame = Colors.mix(pal.line, Colors.darken(pal.line, 0.5f), 0.3f)
        b.stroke(Path().moveTo(x, 0).lineTo(x, top), frame, (3 * u).toFloat(), 0.9f)

        val y0 = top + s * 0.58
        val y1 = top + s * 1.38
        Common.glow(b, x, (y0 + y1) / 2, s * 1.7, pal.glow, 0.5f)

        b.stroke(Path().circle(x, top + s * 0.07, s * 0.07), frame, (4 * u).toFloat())
        b.fill(Common.star(x, top + s * 0.2, s * 0.07, s * 0.03, 8), frame)
        b.fill(Shapes.onionDome(x, top + s * 0.48, s * 0.3, s * 0.30), frame)
        b.fill(Path().rect(x - s * 0.42, top + s * 0.46, x + s * 0.42, y0), frame)

        val pane = RadialFill(
            x.toFloat(), ((y0 + y1) / 2).toFloat(), (s * 0.6).toFloat(),
            intArrayOf(Colors.lighten(pal.glow, 0.4f), pal.glow, Colors.mix(pal.glow, pal.accentC, 0.6f)),
            floatArrayOf(0f, 0.5f, 1f),
        )
        val front = Path().rect(x - s * 0.22, y0, x + s * 0.22, y1)
        val left = Path().moveTo(x - s * 0.40, y0 + s * 0.05).lineTo(x - s * 0.22, y0)
            .lineTo(x - s * 0.22, y1).lineTo(x - s * 0.40, y1 - s * 0.05).close()
        val right = Path().moveTo(x + s * 0.40, y0 + s * 0.05).lineTo(x + s * 0.22, y0)
            .lineTo(x + s * 0.22, y1).lineTo(x + s * 0.40, y1 - s * 0.05).close()
        b.fill(front, pane)
        b.fill(left, pane, 0.8f)
        b.fill(right, pane, 0.8f)
        b.fill(left, Colors.BLACK, 0.18f)
        b.fill(right, Colors.BLACK, 0.18f)
        val sw = (4 * u).toFloat()
        b.stroke(front, frame, sw)
        b.stroke(left, frame, sw)
        b.stroke(right, frame, sw)
        // Fretwork: a pointed arch and a small star in the front pane.
        b.stroke(Shapes.pointedArch(x, y1 - s * 0.06, s * 0.30, s * 0.62), frame, (3 * u).toFloat(), 0.85f)
        b.fill(Common.rubElHizb(x, y0 + s * 0.52, s * 0.07), frame, 0.9f)

        b.fill(Path().rect(x - s * 0.44, y1, x + s * 0.44, y1 + s * 0.09), frame)
        b.fill(
            Path().moveTo(x - s * 0.34, y1 + s * 0.09).lineTo(x + s * 0.34, y1 + s * 0.09)
                .lineTo(x + s * 0.06, y1 + s * 0.38).lineTo(x - s * 0.06, y1 + s * 0.38).close(),
            frame,
        )
        b.fill(Path().circle(x, y1 + s * 0.44, s * 0.06), frame)
        b.fill(Common.star(x, y1 + s * 0.55, s * 0.05, s * 0.02, 4, -PI / 2), pal.glow)
    }
}
