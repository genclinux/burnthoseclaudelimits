package com.noor.wallpapers.art

/*
 * Platform-neutral drawing primitives.
 *
 * Everything under `art` is plain Kotlin with no Android imports, so the same
 * scene code drives the Android renderer (app) and the Java2D renderer used to
 * preview wallpapers on a desktop JVM (tools/preview).
 */

sealed interface Fill

data class SolidFill(val color: Int) : Fill

class LinearFill(
    val x0: Float, val y0: Float, val x1: Float, val y1: Float,
    val colors: IntArray, val stops: FloatArray? = null,
) : Fill

class RadialFill(
    val cx: Float, val cy: Float, val radius: Float,
    val colors: IntArray, val stops: FloatArray? = null,
) : Fill

class Path {
    sealed interface Op
    data class MoveTo(val x: Float, val y: Float) : Op
    data class LineTo(val x: Float, val y: Float) : Op
    data class QuadTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : Op
    data class CubicTo(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float,
    ) : Op
    data object Close : Op

    val ops = ArrayList<Op>()
    var evenOdd = false

    val isEmpty get() = ops.isEmpty()

    fun moveTo(x: Number, y: Number) = apply { ops += MoveTo(x.toFloat(), y.toFloat()) }
    fun lineTo(x: Number, y: Number) = apply { ops += LineTo(x.toFloat(), y.toFloat()) }
    fun quadTo(x1: Number, y1: Number, x2: Number, y2: Number) =
        apply { ops += QuadTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat()) }

    fun cubicTo(x1: Number, y1: Number, x2: Number, y2: Number, x3: Number, y3: Number) = apply {
        ops += CubicTo(
            x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), x3.toFloat(), y3.toFloat(),
        )
    }

    fun close() = apply { ops += Close }

    fun polygon(points: List<Vec>, closed: Boolean = true) = apply {
        if (points.isEmpty()) return@apply
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        if (closed) close()
    }

    fun rect(left: Number, top: Number, right: Number, bottom: Number) = apply {
        moveTo(left, top); lineTo(right, top); lineTo(right, bottom); lineTo(left, bottom); close()
    }

    fun circle(cx: Number, cy: Number, r: Number) = apply {
        val x = cx.toDouble(); val y = cy.toDouble(); val rr = r.toDouble()
        val k = 0.5522847498 * rr
        moveTo(x + rr, y)
        cubicTo(x + rr, y + k, x + k, y + rr, x, y + rr)
        cubicTo(x - k, y + rr, x - rr, y + k, x - rr, y)
        cubicTo(x - rr, y - k, x - k, y - rr, x, y - rr)
        cubicTo(x + k, y - rr, x + rr, y - k, x + rr, y)
        close()
    }

    fun append(other: Path) = apply { ops += other.ops }

    /** Returns a copy with every point passed through [f]; exact for affine maps. */
    fun transformed(f: (Float, Float) -> Pair<Float, Float>): Path {
        val out = Path()
        out.evenOdd = evenOdd
        for (op in ops) {
            out.ops += when (op) {
                is MoveTo -> f(op.x, op.y).let { MoveTo(it.first, it.second) }
                is LineTo -> f(op.x, op.y).let { LineTo(it.first, it.second) }
                is QuadTo -> {
                    val a = f(op.x1, op.y1); val b = f(op.x2, op.y2)
                    QuadTo(a.first, a.second, b.first, b.second)
                }
                is CubicTo -> {
                    val a = f(op.x1, op.y1); val b = f(op.x2, op.y2); val c = f(op.x3, op.y3)
                    CubicTo(a.first, a.second, b.first, b.second, c.first, c.second)
                }
                Close -> Close
            }
        }
        return out
    }
}

enum class FontId { NASKH, NASKH_BOLD, RUQAA, KUFI, LATIN }

sealed interface Item

class FillItem(val path: Path, val fill: Fill, val alpha: Float = 1f) : Item

class StrokeItem(
    val path: Path,
    val fill: Fill,
    val width: Float,
    val alpha: Float = 1f,
    val round: Boolean = true,
) : Item

/**
 * A single line of text centred horizontally on [cx]. Renderers shrink [size]
 * until the text fits [maxWidth].
 *
 * Vertically, plain text is centred on [cy] using the font's ascent/descent.
 * With [inkCentered], the glyphs actually drawn (including Arabic vowel marks and
 * the long descending strokes of Ruqaa and Naskh) are centred on [cy] and shrunk
 * to at most [maxHeight] tall, so the text is guaranteed to stay inside
 * cy ± maxHeight / 2 and nothing placed outside that band can collide with it.
 */
class TextItem(
    val text: String,
    val font: FontId,
    val size: Float,
    val cx: Float,
    val cy: Float,
    val fill: Fill,
    val maxWidth: Float = Float.MAX_VALUE,
    val alpha: Float = 1f,
    val glowColor: Int = 0,
    val glowRadius: Float = 0f,
    val letterSpacing: Float = 0f,
    val maxHeight: Float = Float.MAX_VALUE,
    val inkCentered: Boolean = false,
) : Item

class GroupItem(val items: List<Item>, val clip: Path? = null, val alpha: Float = 1f) : Item

class Scene(val width: Int, val height: Int, val items: List<Item>)

/** Small helper so scene builders read top-to-bottom. */
class SceneBuilder(val width: Int, val height: Int) {
    val items = ArrayList<Item>()

    fun fill(path: Path, fill: Fill, alpha: Float = 1f) {
        if (!path.isEmpty) items += FillItem(path, fill, alpha)
    }

    fun fill(path: Path, color: Int, alpha: Float = 1f) = fill(path, SolidFill(color), alpha)

    fun stroke(path: Path, color: Int, width: Float, alpha: Float = 1f, round: Boolean = true) {
        if (!path.isEmpty) items += StrokeItem(path, SolidFill(color), width, alpha, round)
    }

    fun text(item: TextItem) { items += item }

    fun group(clip: Path? = null, alpha: Float = 1f, block: SceneBuilder.() -> Unit) {
        val inner = SceneBuilder(width, height).apply(block)
        items += GroupItem(inner.items, clip, alpha)
    }

    fun build() = Scene(width, height, items)
}
