package com.noor.wallpapers.art

import kotlin.math.PI

enum class PatternStyle {
    /** Gold strapwork bands over coloured stars, like carved and painted wood. */
    STRAPWORK,

    /** Zellige: every region filled with tile colour, thin grout lines. */
    ZELLIGE,

    /** Fine gold lines on near-black, kind to OLED panels. */
    LINEWORK,
}

object GeometricArt {
    class Params(
        val tiling: Tiling,
        val style: PatternStyle,
        val contactDegrees: Double = tiling.defaultContactDegrees,
        /** Polygon edge length in design pixels. */
        val edge: Double = 150.0,
        val rotation: Double = 0.0,
    )

    fun tiles(ctx: RenderContext, p: Params, rotation: Double = p.rotation, edgeScale: Double = 1.0): List<StarTile> {
        val theta = p.contactDegrees * PI / 180
        return Tilings.cover(p.tiling, ctx.w, ctx.h, p.edge * ctx.u * edgeScale, rotation = rotation)
            .map { Hankin.apply(it, theta) }
    }

    fun scene(ctx: RenderContext, p: Params): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        Common.background(b, ctx)
        drawPattern(b, ctx, p, tiles(ctx, p))
        if (p.style != PatternStyle.LINEWORK) {
            Common.glow(b, ctx.cx, ctx.y(0.46), ctx.safeW * 0.7, ctx.palette.glow, 0.12f)
        }
        Textures.grainOverlay(b, ctx, if (p.style == PatternStyle.LINEWORK) 0.25f else 0.45f)
        Common.vignette(b, ctx, if (p.style == PatternStyle.LINEWORK) 0.7f else 0.5f)
        Common.topShade(b, ctx)
        return b.build()
    }

    /** Draws the pattern; also used as a backdrop by the calligraphy and mihrab designs. */
    fun drawPattern(b: SceneBuilder, ctx: RenderContext, p: Params, tiles: List<StarTile>, alpha: Float = 1f) {
        val pal = ctx.palette
        val edgePx = p.edge * ctx.u
        val maxN = tiles.maxOfOrNull { it.n } ?: 0

        val strands = Path()
        for (t in tiles) strands.polygon(t.star)

        when (p.style) {
            PatternStyle.STRAPWORK -> {
                val bigStars = Path(); val smallStars = Path(); val kites = Path()
                for (t in tiles) {
                    (if (t.n == maxN) bigStars else smallStars).polygon(t.star)
                    for (k in 0 until t.n) kites.polygon(t.kite(k))
                }
                b.fill(kites, pal.accentB, 0.55f * alpha)
                b.fill(bigStars, pal.accentA, 0.9f * alpha)
                b.fill(smallStars, Colors.mix(pal.accentA, pal.accentC, 0.35f), 0.75f * alpha)
                b.stroke(strands, Colors.darken(pal.bgBottom, 0.3f), (edgePx * 0.14).toFloat(), 0.6f * alpha)
                b.stroke(strands, pal.line, (edgePx * 0.10).toFloat(), alpha)
                b.stroke(strands, Colors.darken(pal.bgBottom, 0.2f), (edgePx * 0.035).toFloat(), alpha)
                rosetteCentres(b, ctx, tiles, maxN, edgePx, alpha)
            }
            PatternStyle.ZELLIGE -> {
                // Zellige is cut from separately fired batches: every piece differs a little in tone.
                val shades = 5
                val rnd = ctx.random(61)
                fun bucketed() = Array(shades) { Path() }
                val bigStars = bucketed(); val smallStars = bucketed()
                val kiteA = bucketed(); val kiteB = bucketed()
                for (t in tiles) {
                    (if (t.n == maxN) bigStars else smallStars)[rnd.nextInt(shades)].polygon(t.star)
                    for (k in 0 until t.n) (if (k % 2 == 0) kiteA else kiteB)[rnd.nextInt(shades)].polygon(t.kite(k))
                }
                val shadeRnd = ctx.random(62)
                fun paint(paths: Array<Path>, color: Int) {
                    for (i in 0 until shades) b.fill(paths[i], color.jitter(shadeRnd, 0.09f), alpha)
                }
                paint(kiteA, pal.accentB)
                paint(kiteB, Colors.mix(pal.accentB, pal.bgBottom, 0.35f))
                paint(bigStars, pal.accentA)
                paint(smallStars, pal.accentC)
                // Glaze sheen, as from a window across the wall.
                b.fill(
                    Common.fullRect(ctx),
                    LinearFill(
                        0f, 0f, ctx.w.toFloat(), ctx.h.toFloat(),
                        intArrayOf(Colors.withAlpha(Colors.WHITE, 0.10f), 0, Colors.withAlpha(Colors.WHITE, 0.05f), 0),
                        floatArrayOf(0f, 0.35f, 0.55f, 1f),
                    ),
                    alpha, Blend.SCREEN,
                )
                b.stroke(strands, Colors.lighten(pal.line, 0.2f), (edgePx * 0.045).toFloat(), alpha)
                b.stroke(strands, Colors.darken(pal.bgBottom, 0.2f), (edgePx * 0.012).toFloat(), 0.5f * alpha)
            }
            PatternStyle.LINEWORK -> {
                val bigStars = Path()
                for (t in tiles) if (t.n == maxN) bigStars.polygon(t.star)
                b.fill(bigStars, pal.accentA, 0.22f * alpha)
                b.stroke(strands, pal.line, (edgePx * 0.022).toFloat(), 0.9f * alpha)
            }
        }
    }

    private fun rosetteCentres(
        b: SceneBuilder, ctx: RenderContext, tiles: List<StarTile>, maxN: Int, edgePx: Double, alpha: Float,
    ) {
        val pal = ctx.palette
        val dots = Path()
        val rings = Path()
        for (t in tiles) if (t.n == maxN && maxN >= 6) {
            val c = t.polygon.center
            dots.circle(c.x, c.y, edgePx * 0.07)
            rings.circle(c.x, c.y, edgePx * 0.16)
        }
        b.stroke(rings, pal.line, (edgePx * 0.025).toFloat(), 0.8f * alpha)
        b.fill(dots, pal.line, alpha)
    }
}
