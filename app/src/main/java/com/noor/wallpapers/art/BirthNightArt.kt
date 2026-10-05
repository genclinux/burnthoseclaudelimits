package com.noor.wallpapers.art

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The moon's phase from the mean synodic month: good to a few hours, plenty for a picture. */
object MoonPhase {
    const val SYNODIC_DAYS = 29.530588853

    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private val NEW_MOON: Instant = LocalDateTime.of(2000, 1, 6, 18, 14).toInstant(ZoneOffset.UTC)

    /** Days since the last new moon, 0 until [SYNODIC_DAYS]. */
    fun age(at: Instant): Double {
        val days = (at.epochSecond - NEW_MOON.epochSecond) / 86_400.0
        return ((days % SYNODIC_DAYS) + SYNODIC_DAYS) % SYNODIC_DAYS
    }

    /** Lit fraction of the disc, 0 (new) to 1 (full). */
    fun illumination(at: Instant): Double = (1 - cos(2 * PI * age(at) / SYNODIC_DAYS)) / 2

    fun waxing(at: Instant): Boolean = age(at) < SYNODIC_DAYS / 2

    /**
     * The lit part of a moon of radius [r] at ([cx], [cy]): the bright limb on
     * one side and the terminator, an ellipse, on the other. Waxing moons are
     * lit on the right (as seen from Türkiye); [tilt] turns the whole disc.
     */
    fun litPath(cx: Double, cy: Double, r: Double, illumination: Double, waxing: Boolean, tilt: Double = 0.0): Path {
        val side = if (waxing) 1.0 else -1.0
        val steps = 64
        val pts = ArrayList<Vec>()
        // Bright limb, top to bottom.
        for (i in 0..steps) {
            val a = -PI / 2 + PI * i / steps
            pts += Vec(side * r * cos(a), r * sin(a))
        }
        // Terminator, bottom to top: at 0 it hugs the limb, at 1 it is the far limb.
        val k = 1 - 2 * illumination
        for (i in 0..steps) {
            val a = PI / 2 - PI * i / steps
            pts += Vec(side * r * k * cos(a), r * sin(a))
        }
        val c = Vec(cx, cy)
        return Path().polygon(pts.map { it.rotated(tilt) + c })
    }
}

/**
 * Easter egg, for Hanife Betül: the night she was born, 12 September 2000,
 * over the Marmara from Pendik. The moon is drawn as it really was that
 * evening, a day before full; her initials are among the stars.
 */
object BirthNightArt {
    val BIRTH_DATE: LocalDate = LocalDate.of(2000, 9, 12)

    /** 21:00 in İstanbul (UTC+3 in September 2000) that evening. */
    val EVENING: Instant = LocalDateTime.of(2000, 9, 12, 21, 0).toInstant(ZoneOffset.ofHours(3))

    fun scene(ctx: RenderContext): Scene {
        val b = SceneBuilder(ctx.width, ctx.height)
        val pal = ctx.palette
        val u = ctx.u
        val horizon = ctx.y(0.64)

        // Sky: deep at the top, a little moonlit haze at the horizon.
        val top = Colors.darken(pal.bgBottom, 0.45f)
        val low = Colors.mix(pal.bgTop, pal.accentA, 0.3f)
        b.fill(
            Path().rect(0, 0, ctx.w, horizon),
            LinearFill(0f, 0f, 0f, horizon.toFloat(), intArrayOf(top, pal.bgBottom, low), floatArrayOf(0f, 0.6f, 1f)),
        )
        Common.starfield(b, ctx, 0.0, horizon * 0.85, 240, salt = 912)

        // The moon that night: waxing gibbous, almost full.
        val illum = MoonPhase.illumination(EVENING)
        val moonX = ctx.x(0.66)
        val moonY = ctx.y(0.19)
        val moonR = 120 * u
        val moonLight = Colors.lighten(pal.glow, 0.55f)
        Common.glow(b, moonX, moonY, moonR * 5, pal.glow, 0.38f)
        b.fill(Path().circle(moonX, moonY, moonR), Colors.lighten(pal.bgBottom, 0.25f), 0.35f)
        b.fill(MoonPhase.litPath(moonX, moonY, moonR, illum, MoonPhase.waxing(EVENING), tilt = -0.35), moonLight)
        // A few soft maria so it reads as the moon, not a coin.
        val maria = Path()
            .circle(moonX + 30 * u, moonY - 28 * u, 26 * u)
            .circle(moonX - 10 * u, moonY + 22 * u, 34 * u)
            .circle(moonX + 46 * u, moonY + 40 * u, 16 * u)
        b.fill(maria, Colors.darken(moonLight, 0.25f), 0.25f)

        // Her initials, opposite the moon.
        Common.initialsConstellation(b, ctx, ctx.x(0.12), ctx.y(0.27), 74 * u)

        // The Princes' Islands low on the horizon, and a shore mosque of Pendik.
        val haze = Colors.mix(top, low, 0.55f)
        val islands = Path()
        for ((fx, half, h) in listOf(Triple(0.08, 0.10, 46.0), Triple(0.30, 0.14, 70.0), Triple(0.52, 0.08, 38.0))) {
            val x = ctx.x(fx)
            val w = ctx.safeW * half
            islands.moveTo(x - w, horizon).cubicTo(x - w * 0.5, horizon - h * u, x + w * 0.4, horizon - h * u * 1.1, x + w, horizon).close()
        }
        b.fill(islands, haze, 0.85f)

        val silhouette = Colors.darken(pal.bgBottom, 0.6f)
        val mx = ctx.x(0.78)
        val (body0, windows0) = NightArt.mosque(ctx, NightArt.Architecture.OTTOMAN, mx, horizon, ctx.random(2000))
        val s = 0.42
        val far: (Float, Float) -> Pair<Float, Float> = { x, y -> (mx + (x - mx) * s).toFloat() to (horizon + (y - horizon) * s).toFloat() }
        val body = body0.transformed(far)
        val windows = windows0.transformed(far)
        b.fill(body, silhouette)
        b.fill(windows, pal.glow, 0.9f)

        // The Marmara, with the moon's road on it.
        b.fill(
            Path().rect(0, horizon, ctx.w, ctx.h),
            LinearFill(0f, horizon.toFloat(), 0f, ctx.h.toFloat(), intArrayOf(Colors.darken(low, 0.5f), Colors.darken(pal.bgBottom, 0.65f))),
        )
        b.group(clip = Path().rect(0, horizon, ctx.w, ctx.h)) {
            val mirror: (Float, Float) -> Pair<Float, Float> = { x, y -> x to (2 * horizon - y).toFloat() }
            fill(body.transformed(mirror), silhouette, 0.5f)
            fill(windows.transformed(mirror), pal.glow, 0.22f)
            val road = Path()
            val r = ctx.random(12)
            repeat(30) {
                val y = horizon + (ctx.h - horizon) * (it / 30.0) + r.nextDouble() * 8 * u
                val half = (30 + r.nextDouble() * 110) * u * (0.4 + it / 30.0)
                road.rect(moonX - half, y, moonX + half, y + 3 * u)
            }
            fill(road, moonLight, 0.22f)
        }

        if (ctx.options.captions) {
            val y = ctx.y(0.80)
            b.text(
                TextItem(
                    "Doğduğun gece", FontId.LATIN, (52 * u).toFloat(), ctx.cx.toFloat(), y.toFloat(),
                    SolidFill(pal.line), letterSpacing = 0.06f,
                    glowColor = Colors.withAlpha(pal.glow, 0.5f), glowRadius = (14 * u).toFloat(),
                ),
            )
            b.text(
                TextItem(
                    "12 Eylül 2000 · 14 Cemâziyelâhir 1421", FontId.LATIN, (32 * u).toFloat(), ctx.cx.toFloat(), (y + 70 * u).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.4f)), maxWidth = (ctx.safeW * 0.86).toFloat(), alpha = 0.9f,
                ),
            )
            b.text(
                TextItem(
                    "Ay o gece %${Math.round(illum * 100)} doluydu; ertesi gün dolunay", FontId.LATIN, (26 * u).toFloat(),
                    ctx.cx.toFloat(), (y + 122 * u).toFloat(),
                    SolidFill(Colors.lighten(pal.line, 0.5f)), maxWidth = (ctx.safeW * 0.86).toFloat(), alpha = 0.65f,
                ),
            )
        }
        Textures.grainOverlay(b, ctx, 0.35f)
        Common.vignette(b, ctx, 0.45f)
        return b.build()
    }
}
