package com.noor.wallpapers.art

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class StarPatternTest {
    private val w = RenderContext.REFERENCE_WIDTH.toDouble()
    private val h = RenderContext.REFERENCE_HEIGHT.toDouble()

    @Test
    fun everyTilingIsEdgeToEdge() {
        // In an edge-to-edge tiling every interior edge midpoint is shared by exactly two polygons.
        for (t in Tiling.entries) {
            val polys = Tilings.cover(t, w, h, 100.0)
            val counts = HashMap<Pair<Long, Long>, Int>()
            for (p in polys) for (k in 0 until p.n) {
                val m = (p.vertices[k] + p.vertices[(k + 1) % p.n]) / 2.0
                val key = Math.round(m.x * 10) to Math.round(m.y * 10)
                counts[key] = (counts[key] ?: 0) + 1
            }
            val interior = counts.filterKeys { (x, y) -> x in 3000..9000 && y in 10000..17000 }
            assertTrue("$t has interior edges", interior.isNotEmpty())
            interior.forEach { (k, c) -> assertEquals("$t edge at $k", 2, c) }
        }
    }

    @Test
    fun polygonsAreRegularWithRequestedEdge() {
        for (t in Tiling.entries) {
            for (p in Tilings.cover(t, w, h, 80.0).take(20)) {
                for (k in 0 until p.n) {
                    val e = (p.vertices[(k + 1) % p.n] - p.vertices[k]).length
                    assertEquals("$t edge", 80.0, e, 1e-6)
                }
            }
        }
    }

    @Test
    fun coverReachesEveryCorner() {
        for (t in Tiling.entries) {
            val polys = Tilings.cover(t, w, h, 150.0, rotation = 0.3)
            for (corner in listOf(Vec(0.0, 0.0), Vec(w, 0.0), Vec(0.0, h), Vec(w, h), Vec(w / 2, h / 2))) {
                val nearest = polys.minOf { (it.center - corner).length }
                assertTrue("$t near $corner", nearest < 150.0 * 2.5)
            }
        }
    }

    @Test
    fun hankinStarIsSymmetricAndInside() {
        val square = Tilings.regular(Vec.ZERO, 4, 1.0, 0.0)
        val tile = Hankin.apply(square, 67.5 * PI / 180)
        assertEquals(8, tile.star.size)
        val inner = tile.star.filterIndexed { i, _ -> i % 2 == 1 }.map { it.length }
        inner.forEach { assertEquals(inner[0], it, 1e-9) }
        // Inner points sit between the centre and the corners.
        assertTrue(inner[0] > 0 && inner[0] < square.vertices[0].length)
    }

    @Test
    fun parallelRaysDoNotBlowUp() {
        // For a square, theta = 45 degrees makes the two rays collinear.
        val tile = Hankin.apply(Tilings.regular(Vec.ZERO, 4, 1.0, 0.0), PI / 4)
        tile.star.forEach { assertTrue(it.length.isFinite() && it.length < 1.0) }
    }

    @Test
    fun crescentStaysInsideItsCircle() {
        val p = Common.crescent(100.0, 100.0, 50.0)
        for (op in p.ops) if (op is Path.LineTo) {
            val d = Vec(op.x - 100.0, op.y - 100.0).length
            assertTrue(d <= 50.0 + 1e-3)
        }
    }

    @Test
    fun selectionKeysRoundTripAndEveryEntryRenders() {
        for (e in Catalog.entries) {
            val scene = e.render(RenderContext(254, 554, e.defaultPalette, e.defaultSeed))
            assertTrue("${e.id} is empty", scene.items.isNotEmpty())
        }
        assertEquals(Catalog.entries.size, Catalog.entries.map { it.id }.toSet().size)
    }

    @Test
    fun renderingIsDeterministic() {
        val e = Catalog.byId("night-istanbul")!!
        fun count() = e.render(RenderContext(300, 650, e.defaultPalette, 42)).items.sumOf { (it as? FillItem)?.path?.ops?.size ?: 0 }
        assertEquals(count(), count())
        assertTrue(abs(count()) > 0)
    }
}
