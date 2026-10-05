package com.noor.wallpapers.art

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Islamic star patterns built with Hankin's "polygons in contact" method:
 * take a tiling of regular polygons, and from the midpoint of every edge send
 * two rays into each polygon at a fixed contact angle. Where neighbouring rays
 * meet they form the star; across shared edges they cross to form the
 * interlaced strapwork.
 */

class RegularPolygon(val center: Vec, val n: Int, val vertices: List<Vec>)

/** A polygon after Hankin construction: [star] alternates edge midpoints and inner points. */
class StarTile(val polygon: RegularPolygon, val star: List<Vec>) {
    val n get() = polygon.n

    /** The kite at vertex k: the region between the star and the polygon corner. */
    fun kite(k: Int): List<Vec> {
        val v = polygon.vertices[k]
        val n = polygon.n
        // star[2*i] = M_i (midpoint of edge i, from V_i to V_{i+1}); star[2*i+1] = P_{i+1}.
        val mBefore = star[2 * ((k - 1 + n) % n)]
        val p = star[2 * ((k - 1 + n) % n) + 1]
        val mAfter = star[2 * k]
        return listOf(mBefore, v, mAfter, p)
    }
}

enum class Tiling(val label: String, val defaultContactDegrees: Double) {
    SQUARE("Hâtem", 67.5),
    HEXAGON("Altı Köşeli Yıldız", 60.0),
    OCTAGON_SQUARE("Rub-ül Hizb", 67.5),
    DODECAGON_TRIANGLE("Şemse", 72.0),
    DODECAGON_HEXAGON_SQUARE("On İki Kollu Gül", 60.0),
}

object Tilings {
    fun regular(center: Vec, n: Int, edge: Double, normalAngle: Double): RegularPolygon {
        val r = edge / (2 * sin(PI / n))
        val vs = (0 until n).map { k -> center + Vec.polar(r, normalAngle - PI / n + 2 * PI * k / n) }
        return RegularPolygon(center, n, vs)
    }

    private class UnitCell(val a: Vec, val b: Vec, val polys: List<RegularPolygon>)

    private fun unitCell(t: Tiling): UnitCell {
        fun dir(v: Vec) = v.angle
        return when (t) {
            Tiling.SQUARE -> UnitCell(
                Vec(1.0, 0.0), Vec(0.0, 1.0),
                listOf(regular(Vec.ZERO, 4, 1.0, 0.0)),
            )
            Tiling.HEXAGON -> UnitCell(
                Vec(sqrt(3.0), 0.0), Vec(sqrt(3.0) / 2, 1.5),
                listOf(regular(Vec.ZERO, 6, 1.0, 0.0)),
            )
            Tiling.OCTAGON_SQUARE -> {
                val w = 1 + sqrt(2.0)
                UnitCell(
                    Vec(w, 0.0), Vec(0.0, w),
                    listOf(
                        regular(Vec.ZERO, 8, 1.0, 0.0),
                        regular(Vec(w / 2, w / 2), 4, 1.0, PI / 4),
                    ),
                )
            }
            Tiling.DODECAGON_TRIANGLE -> {
                val d = 2 * apothem(12)
                val a = Vec(d, 0.0)
                val b = Vec(d / 2, d * sqrt(3.0) / 2)
                val up = (a + b) / 3.0
                val down = (a + b) * (2.0 / 3.0)
                UnitCell(
                    a, b,
                    listOf(
                        regular(Vec.ZERO, 12, 1.0, 0.0),
                        regular(up, 3, 1.0, dir(-up)),
                        regular(down, 3, 1.0, dir((a + b) - down)),
                    ),
                )
            }
            Tiling.DODECAGON_HEXAGON_SQUARE -> {
                val d = 2 * apothem(12) + 1
                val a = Vec(d, 0.0)
                val b = Vec(d / 2, d * sqrt(3.0) / 2)
                val up = (a + b) / 3.0
                val down = (a + b) * (2.0 / 3.0)
                UnitCell(
                    a, b,
                    listOf(
                        regular(Vec.ZERO, 12, 1.0, 0.0),
                        regular(a / 2.0, 4, 1.0, 0.0),
                        regular(b / 2.0, 4, 1.0, PI / 3),
                        regular((b - a) / 2.0, 4, 1.0, 2 * PI / 3),
                        regular(up, 6, 1.0, dir(-up)),
                        regular(down, 6, 1.0, dir((a + b) - down)),
                    ),
                )
            }
        }
    }

    fun apothem(n: Int) = 1 / (2 * tan(PI / n))

    /**
     * All polygons of [tiling] that touch a [width]x[height] canvas, with edge
     * length [edgePx], a polygon centred on ([cx], [cy]) and the whole tiling
     * rotated by [rotation] radians.
     */
    fun cover(
        tiling: Tiling,
        width: Double,
        height: Double,
        edgePx: Double,
        cx: Double = width / 2,
        cy: Double = height / 2,
        rotation: Double = 0.0,
    ): List<RegularPolygon> {
        val cell = unitCell(tiling)
        val reach = hypot(width, height) / 2 + edgePx * 4
        val step = minOf(cell.a.length, cell.b.length) * edgePx * 0.8
        val n = ceil(reach / step).toInt() + 2
        val center = Vec(cx, cy)
        val out = ArrayList<RegularPolygon>()
        val margin = edgePx * 3
        for (i in -n..n) for (j in -n..n) {
            val offset = cell.a * i.toDouble() + cell.b * j.toDouble()
            for (p in cell.polys) {
                val c = ((p.center + offset) * edgePx).rotated(rotation) + center
                if (c.x < -margin || c.x > width + margin || c.y < -margin || c.y > height + margin) continue
                if ((c - center).length > reach + margin) continue
                val vs = p.vertices.map { ((it + offset) * edgePx).rotated(rotation) + center }
                out += RegularPolygon(c, p.n, vs)
            }
        }
        return out
    }
}

object Hankin {
    /** Applies Hankin's construction with contact angle [theta] (radians, 0..PI/2). */
    fun apply(poly: RegularPolygon, theta: Double): StarTile {
        val vs = poly.vertices
        val n = vs.size
        val mids = (0 until n).map { (vs[it] + vs[(it + 1) % n]) / 2.0 }
        val star = ArrayList<Vec>(2 * n)
        for (k in 0 until n) {
            val corner = vs[(k + 1) % n]
            val m1 = mids[k]
            val m2 = mids[(k + 1) % n]
            val d1 = inward((corner - vs[k]).normalized(), theta, m1, poly.center)
            val d2 = inward((corner - vs[(k + 2) % n]).normalized(), theta, m2, poly.center)
            val p = intersectLines(m1, d1, m2, d2) ?: ((m1 + m2) / 2.0)
            star += m1
            star += p
        }
        return StarTile(poly, star)
    }

    /** Rotates [d] by +-theta, whichever way points into the polygon. */
    private fun inward(d: Vec, theta: Double, from: Vec, center: Vec): Vec {
        val a = d.rotated(theta)
        return if (a dot (center - from) > 0) a else d.rotated(-theta)
    }
}
