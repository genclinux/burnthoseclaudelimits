package com.noor.wallpapers.art

/** A prayer niche: a tiled wall, a gilded pointed arch with an alfiz frame, and a hanging lamp. */
object MihrabArt {
    class Params(
        val wall: Tiling,
        val wallStyle: PatternStyle,
        val niche: Tiling,
        val inscription: Phrase = Phrases.BISMILLAH,
    )

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        Common.background(b, ctx)
        val wall = GeometricArt.Params(p.wall, p.wallStyle, edge = 80.0)
        GeometricArt.drawPattern(b, ctx, wall, GeometricArt.tiles(ctx, wall), alpha = 0.85f)
        b.fill(Common.fullRect(ctx), Colors.BLACK, 0.25f)

        val cx = ctx.cx
        val bottom = ctx.y(0.95)
        val outerW = ctx.safeW * 0.80
        val outerH = ctx.safeH * 0.70
        val band = 34 * u

        // Alfiz: the rectangular frame around the arch.
        val alfizTop = bottom - outerH - 170 * u
        val alfiz = Path().rect(cx - outerW / 2 - 50 * u, alfizTop, cx + outerW / 2 + 50 * u, bottom + 40 * u)
        b.fill(alfiz, Colors.darken(pal.bgBottom, 0.3f), 0.85f)
        b.stroke(alfiz, pal.line, (8 * u).toFloat())
        val inner = Path().rect(cx - outerW / 2 - 30 * u, alfizTop + 20 * u, cx + outerW / 2 + 30 * u, bottom + 40 * u)
        b.stroke(inner, pal.line, (2.5 * u).toFloat(), 0.8f)

        // Inscription band at the top of the frame.
        b.text(
            TextItem(
                p.inscription.arabic, FontId.NASKH_BOLD, (72 * u).toFloat(), cx.toFloat(),
                (alfizTop + 90 * u).toFloat(), SolidFill(pal.line), maxWidth = (outerW * 0.95).toFloat(),
                glowColor = Colors.withAlpha(pal.glow, 0.4f), glowRadius = (8 * u).toFloat(),
                // Keep clear of the arch's apex below the band.
                maxHeight = (110 * u).toFloat(),
                inkCentered = true,
            ),
        )

        // Spandrel medallions.
        for (s in listOf(-1.0, 1.0)) {
            val mx = cx + s * outerW * 0.38
            val my = bottom - outerH + 40 * u
            b.fill(Common.star(mx, my, 56 * u, 40 * u, 8), pal.line)
            b.fill(Common.star(mx, my, 44 * u, 30 * u, 8, Math.PI / 8 - Math.PI / 2), pal.accentA)
            b.fill(Path().circle(mx, my, 12 * u), pal.glow)
        }

        // The arch: gilded outer band, then the deep niche.
        val outer = Shapes.pointedArch(cx, bottom, outerW, outerH)
        val niche = Shapes.pointedArch(cx, bottom, outerW - 2 * band, outerH - band * 1.4)
        b.fill(outer, LinearFill(0f, (bottom - outerH).toFloat(), 0f, bottom.toFloat(), intArrayOf(Colors.lighten(pal.line, 0.25f), pal.line, Colors.darken(pal.line, 0.3f))))
        b.stroke(Shapes.pointedArch(cx, bottom, outerW - band, outerH - band * 0.7), Colors.darken(pal.line, 0.45f), (3 * u).toFloat(), 0.8f)
        b.fill(
            niche,
            RadialFill(
                cx.toFloat(), (bottom - outerH * 0.45).toFloat(), (outerH * 0.7).toFloat(),
                intArrayOf(Colors.mix(pal.accentB, pal.glow, 0.15f), Colors.darken(pal.bgBottom, 0.35f)),
            ),
        )
        val nicheTiles = GeometricArt.Params(p.niche, PatternStyle.LINEWORK, edge = 64.0)
        b.group(clip = niche, alpha = 0.55f) {
            GeometricArt.drawPattern(this, ctx, nicheTiles, GeometricArt.tiles(ctx, nicheTiles))
        }

        // Hanging lamp.
        val apexY = bottom - outerH + band * 1.4
        val lampY = apexY + outerH * 0.33
        lamp(b, ctx, cx, apexY, lampY, 150 * u)
        Textures.grainOverlay(b, ctx, 0.45f)
        Common.vignette(b, ctx, 0.5f)
        Common.topShade(b, ctx, 0.25f)
        return b.build()
    }

    /** Mamluk-style glass mosque lamp hung on three chains. */
    fun lamp(b: SceneBuilder, ctx: RenderContext, x: Double, hookY: Double, top: Double, s: Double) {
        val pal = ctx.palette
        val u = ctx.u
        Common.glow(b, x, top + s * 0.6, s * 2.4, pal.glow, 0.55f)
        val chains = Path()
        for (dx in listOf(-0.42, 0.0, 0.42)) chains.moveTo(x, hookY).lineTo(x + dx * s, top)
        b.stroke(chains, pal.line, (2.5 * u).toFloat(), 0.9f)
        val vessel = Path().moveTo(x - s * 0.45, top)
            .cubicTo(x - s * 0.40, top + s * 0.25, x - s * 0.22, top + s * 0.35, x - s * 0.24, top + s * 0.5)
            .cubicTo(x - s * 0.70, top + s * 0.62, x - s * 0.62, top + s * 1.05, x - s * 0.28, top + s * 1.12)
            .lineTo(x - s * 0.20, top + s * 1.32).lineTo(x + s * 0.20, top + s * 1.32).lineTo(x + s * 0.28, top + s * 1.12)
            .cubicTo(x + s * 0.62, top + s * 1.05, x + s * 0.70, top + s * 0.62, x + s * 0.24, top + s * 0.5)
            .cubicTo(x + s * 0.22, top + s * 0.35, x + s * 0.40, top + s * 0.25, x + s * 0.45, top)
            .close()
        b.fill(
            vessel,
            RadialFill(
                x.toFloat(), (top + s * 0.8).toFloat(), (s * 0.8).toFloat(),
                intArrayOf(Colors.WHITE, Colors.lighten(pal.glow, 0.2f), Colors.mix(pal.glow, pal.accentC, 0.5f)),
                floatArrayOf(0f, 0.35f, 1f),
            ),
        )
        b.stroke(vessel, pal.line, (4 * u).toFloat())
        // Enamel band with medallions.
        b.fill(Path().rect(x - s * 0.52, top + s * 0.70, x + s * 0.52, top + s * 0.80), pal.accentA, 0.8f)
        for (i in -1..1) b.fill(Path().circle(x + i * s * 0.3, top + s * 0.75, s * 0.07), pal.line)
        b.fill(Path().rect(x - s * 0.36, top + s * 0.18, x + s * 0.36, top + s * 0.24), pal.accentA, 0.7f)
    }
}
