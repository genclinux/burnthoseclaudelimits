package com.noor.wallpapers.prayer

import com.noor.wallpapers.art.Colors
import com.noor.wallpapers.art.Common
import com.noor.wallpapers.art.FontId
import com.noor.wallpapers.art.Item
import com.noor.wallpapers.art.Path
import com.noor.wallpapers.art.RenderContext
import com.noor.wallpapers.art.SceneBuilder
import com.noor.wallpapers.art.SolidFill
import com.noor.wallpapers.art.TextItem
import java.time.Duration
import java.time.Instant

/** What the wallpaper's prayer panel says at one moment. */
data class OverlayInfo(
    val next: Prayer,
    val nextTime: String,
    val countdown: String,
    val today: List<Pair<Prayer, String>>,
    val current: Prayer?,
    val place: String,
) {
    companion object {
        fun of(schedule: PrayerSchedule, now: Instant): OverlayInfo? {
            val next = schedule.next(now) ?: return null
            val today = schedule.day(schedule.today(now)) ?: return null
            return OverlayInfo(
                next = next.prayer,
                nextTime = TurkishText.hhmm(next.time),
                countdown = TurkishText.countdown(Duration.between(now, next.instant)),
                today = Prayer.entries.map { it to TurkishText.hhmm(today.time(it)) },
                current = schedule.current(now)?.prayer,
                place = schedule.location.label,
            )
        }
    }
}

enum class OverlayPosition(val title: String, val y: Double) {
    TOP("Üstte", 0.115),
    MIDDLE("Ortada", 0.5),
    BOTTOM("Altta", 0.8),
}

enum class OverlayStyle(val title: String) { COMPACT("Sade"), DETAILED("Ayrıntılı") }

/**
 * The prayer panel drawn on the live wallpaper: the next time and how long until
 * it, and in the detailed style all six times of the day with the current one lit.
 * Drawn in the design's own palette, like a gilded cartouche.
 */
object PrayerOverlay {
    fun items(ctx: RenderContext, info: OverlayInfo, style: OverlayStyle, position: OverlayPosition): List<Item> {
        val b = SceneBuilder(ctx.width, ctx.height)
        when (style) {
            OverlayStyle.COMPACT -> compact(b, ctx, info, ctx.y(position.y))
            OverlayStyle.DETAILED -> detailed(b, ctx, info, ctx.y(position.y))
        }
        return b.items
    }

    /** Design pixels for the panel; a tablet's square safe area gets it a little larger so it stays legible. */
    private fun unit(ctx: RenderContext) = ctx.u * if (ctx.safeH < ctx.safeW * 1.5) 1.3 else 1.0

    private fun panel(b: SceneBuilder, ctx: RenderContext, l: Double, t: Double, r: Double, bt: Double, radius: Double) {
        val pal = ctx.palette
        val u = unit(ctx)
        Common.glow(b, (l + r) / 2, (t + bt) / 2, (r - l) * 0.62, pal.glow, 0.10f)
        b.fill(Path().roundRect(l, t, r, bt, radius), Colors.darken(pal.bgBottom, 0.35f), 0.72f)
        b.stroke(Path().roundRect(l, t, r, bt, radius), pal.line, (2.5 * u).toFloat(), 0.6f)
        val i = 9 * u
        b.stroke(Path().roundRect(l + i, t + i, r - i, bt - i, radius - i), pal.line, (1.0 * u).toFloat(), 0.28f)
    }

    private fun label(text: String, size: Double, x: Double, y: Double, color: Int, alpha: Float, maxWidth: Double, spacing: Float = 0f) =
        TextItem(
            text, FontId.LATIN, size.toFloat(), x.toFloat(), y.toFloat(), SolidFill(color),
            maxWidth = maxWidth.toFloat(), alpha = alpha, letterSpacing = spacing,
        )

    private fun headline(info: OverlayInfo) = "${info.next.title.uppercase(TurkishText.TR)}  ${info.nextTime}"

    private fun compact(b: SceneBuilder, ctx: RenderContext, info: OverlayInfo, cy: Double) {
        val pal = ctx.palette
        val u = unit(ctx)
        val hw = 330 * u
        val hh = 68 * u
        val l = ctx.cx - hw
        val r = ctx.cx + hw
        panel(b, ctx, l, cy - hh, r, cy + hh, hh)
        for (x in listOf(l + 62 * u, r - 62 * u)) {
            b.fill(Common.rubElHizb(x, cy, 17 * u), pal.line, 0.85f)
            b.fill(Path().circle(x, cy, 6 * u), pal.glow, 0.9f)
        }
        val textW = 2 * hw - 190 * u
        b.text(label(headline(info), 42 * u, ctx.cx, cy - 19 * u, pal.line, 0.98f, textW, 0.1f))
        b.text(label("vaktine ${info.countdown}", 29 * u, ctx.cx, cy + 30 * u, Colors.lighten(pal.line, 0.55f), 0.82f, textW))
    }

    private fun detailed(b: SceneBuilder, ctx: RenderContext, info: OverlayInfo, cy: Double) {
        val pal = ctx.palette
        val u = unit(ctx)
        val hw = minOf(500 * u, ctx.safeW * 0.46)
        val hh = 132 * u
        val l = ctx.cx - hw
        val r = ctx.cx + hw
        val t = cy - hh
        panel(b, ctx, l, t, r, cy + hh, 40 * u)

        b.text(label(headline(info), 42 * u, ctx.cx, t + 58 * u, pal.line, 0.98f, 2 * hw - 120 * u, 0.1f))
        b.text(
            label(
                "vaktine ${info.countdown}  ·  ${info.place}", 26 * u, ctx.cx, t + 102 * u,
                Colors.lighten(pal.line, 0.55f), 0.8f, 2 * hw - 120 * u,
            ),
        )
        // A gilded rule with a small star at its centre.
        val ruleY = t + 136 * u
        b.stroke(Path().moveTo(l + 50 * u, ruleY).lineTo(ctx.cx - 24 * u, ruleY), pal.line, (1.2 * u).toFloat(), 0.45f)
        b.stroke(Path().moveTo(ctx.cx + 24 * u, ruleY).lineTo(r - 50 * u, ruleY), pal.line, (1.2 * u).toFloat(), 0.45f)
        b.fill(Common.rubElHizb(ctx.cx, ruleY, 10 * u), pal.line, 0.75f)

        val colW = (2 * hw - 60 * u) / 6
        info.today.forEachIndexed { i, (p, time) ->
            val x = l + 30 * u + colW * (i + 0.5)
            val lit = p == info.current
            if (lit) {
                b.fill(Path().roundRect(x - colW * 0.46, ruleY + 16 * u, x + colW * 0.46, cy + hh - 18 * u, 18 * u), pal.line, 0.16f)
            }
            val nameColor = if (lit) pal.glow else Colors.lighten(pal.line, 0.4f)
            b.text(label(p.title.uppercase(TurkishText.TR), 20 * u, x, ruleY + 42 * u, nameColor, if (lit) 1f else 0.7f, colW * 0.92, 0.06f))
            b.text(label(time, 30 * u, x, ruleY + 84 * u, if (lit) pal.glow else pal.line, if (lit) 1f else 0.9f, colW * 0.92))
        }
    }
}
