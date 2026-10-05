package com.noor.wallpapers.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Navigation icons drawn in the app's own vocabulary: crescent, star, compass, beads, calendar. */
object NoorIcons {
    private fun icon(name: String, build: PathBuilder.() -> Unit, evenOdd: Boolean = false) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black), pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero, pathBuilder = build)
            .build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        close()
    }

    private fun PathBuilder.polygon(points: List<Pair<Float, Float>>) {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        close()
    }

    private fun star(cx: Float, cy: Float, outer: Float, inner: Float, n: Int, rot: Double = -PI / 2) =
        (0 until n * 2).map { i ->
            val r = if (i % 2 == 0) outer else inner
            val a = rot + PI * i / n
            (cx + r * cos(a)).toFloat() to (cy + r * sin(a)).toFloat()
        }

    /** Prayer times: a crescent and a star. */
    val Vakit: ImageVector = icon("Vakit", {
        // Crescent as the outer disc minus an offset disc, traced as a polygon.
        val pts = ArrayList<Pair<Float, Float>>()
        val ox = 11f; val oy = 13f; val r1 = 8.5f
        val ix = 15f; val iy = 9.6f; val r2 = 7.2f
        fun inInner(x: Float, y: Float) = (x - ix) * (x - ix) + (y - iy) * (y - iy) < r2 * r2
        fun inOuter(x: Float, y: Float) = (x - ox) * (x - ox) + (y - oy) * (y - oy) <= r1 * r1 + 0.01f
        val outer = (0 until 180).map { val a = 2 * PI * it / 180; (ox + r1 * cos(a)).toFloat() to (oy + r1 * sin(a)).toFloat() }
        val start = outer.indices.first { !inInner(outer[it].first, outer[it].second) && inInner(outer[(it + 179) % 180].first, outer[(it + 179) % 180].second) }
        var k = start
        while (!inInner(outer[k % 180].first, outer[k % 180].second)) { pts += outer[k % 180]; k++ }
        val inner = (0 until 180).map { val a = 2 * PI * it / 180; (ix + r2 * cos(a)).toFloat() to (iy + r2 * sin(a)).toFloat() }
        val run = ArrayList<Pair<Float, Float>>()
        val s2 = inner.indices.first { inOuter(inner[it].first, inner[it].second) && !inOuter(inner[(it + 179) % 180].first, inner[(it + 179) % 180].second) }
        k = s2
        while (inOuter(inner[k % 180].first, inner[k % 180].second)) { run += inner[k % 180]; k++ }
        val last = pts.last()
        fun d(a: Pair<Float, Float>, b: Pair<Float, Float>) = (a.first - b.first) * (a.first - b.first) + (a.second - b.second) * (a.second - b.second)
        if (d(run.first(), last) > d(run.last(), last)) run.reverse()
        polygon(pts + run)
        polygon(star(18.2f, 5.6f, 2.6f, 1.05f, 5))
    })

    /** Gallery: an eight-pointed rub el hizb. */
    val Galeri: ImageVector = icon("Galeri", {
        polygon(listOf(5f to 5f, 19f to 5f, 19f to 19f, 5f to 19f))
        polygon(listOf(12f to 2.1f, 21.9f to 12f, 12f to 21.9f, 2.1f to 12f))
        circle(12f, 12f, 2.6f)
    }, evenOdd = true)

    /** Qibla: a compass needle in a ring. */
    val Kible: ImageVector = icon("Kible", {
        circle(12f, 12f, 10f)
        circle(12f, 12f, 8.4f)
        polygon(listOf(12f to 4.6f, 14.4f to 12f, 12f to 19.4f, 9.6f to 12f))
    }, evenOdd = true)

    /** Tesbih: a ring of beads with a tassel. */
    val Tesbih: ImageVector = icon("Tesbih", {
        for (i in 0 until 11) {
            val a = -PI / 2 + 2 * PI * (i + 0.5) / 12
            circle((12 + 7.5 * cos(a)).toFloat(), (10.5 + 7.5 * sin(a)).toFloat(), 1.55f)
        }
        polygon(listOf(11f to 18.5f, 13f to 18.5f, 13.6f to 22.5f, 10.4f to 22.5f))
        circle(12f, 3f, 1.9f)
    })

    /** Religious days: a calendar page with a crescent. */
    val Takvim: ImageVector = icon("Takvim", {
        polygon(listOf(3f to 5f, 21f to 5f, 21f to 21.5f, 3f to 21.5f))
        polygon(listOf(4.8f to 9.2f, 19.2f to 9.2f, 19.2f to 19.7f, 4.8f to 19.7f))
        polygon(listOf(6.5f to 2.5f, 8.3f to 2.5f, 8.3f to 5f, 6.5f to 5f))
        polygon(listOf(15.7f to 2.5f, 17.5f to 2.5f, 17.5f to 5f, 15.7f to 5f))
        polygon(star(12f, 14.4f, 3.4f, 1.4f, 8, -PI / 2))
    }, evenOdd = true)
}
