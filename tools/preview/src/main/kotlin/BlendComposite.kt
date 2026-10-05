package com.noor.wallpapers.preview

import com.noor.wallpapers.art.Blend
import java.awt.Composite
import java.awt.CompositeContext
import java.awt.RenderingHints
import java.awt.image.ColorModel
import java.awt.image.Raster
import java.awt.image.WritableRaster

/** Multiply / screen blending for Java2D, which only has Porter-Duff modes built in. */
class BlendComposite(private val mode: Blend, private val alpha: Float) : Composite {
    override fun createContext(src: ColorModel, dst: ColorModel, hints: RenderingHints?): CompositeContext =
        object : CompositeContext {
            override fun dispose() {}
            override fun compose(s: Raster, d: Raster, out: WritableRaster) {
                val w = minOf(s.width, d.width); val h = minOf(s.height, d.height)
                val sp = IntArray(4); val dp = IntArray(4)
                for (y in 0 until h) for (x in 0 until w) {
                    s.getPixel(x, y, sp); d.getPixel(x, y, dp)
                    // Source colour model may be premultiplied or not; read via the colour model.
                    val sa = src.getAlpha(s.getDataElements(x, y, null)) / 255f * alpha
                    val sr = src.getRed(s.getDataElements(x, y, null))
                    val sg = src.getGreen(s.getDataElements(x, y, null))
                    val sb = src.getBlue(s.getDataElements(x, y, null))
                    fun blend(a: Int, b: Int): Int = when (mode) {
                        Blend.MULTIPLY -> a * b / 255
                        Blend.SCREEN -> 255 - (255 - a) * (255 - b) / 255
                        Blend.NORMAL -> a
                    }
                    dp[0] = (dp[0] + (blend(sr, dp[0]) - dp[0]) * sa).toInt()
                    dp[1] = (dp[1] + (blend(sg, dp[1]) - dp[1]) * sa).toInt()
                    dp[2] = (dp[2] + (blend(sb, dp[2]) - dp[2]) * sa).toInt()
                    out.setPixel(x, y, dp)
                }
            }
        }
}
