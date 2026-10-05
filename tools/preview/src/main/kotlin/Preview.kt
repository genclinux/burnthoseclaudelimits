package com.noor.wallpapers.preview

import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.RenderContext
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Renders every catalog entry to PNG plus a contact sheet.
 * Args: <outDir> <scale> [idFilter]. Scale 1.0 = OPPO Find X9 Pro native 1272x2772.
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/previews" }).apply { mkdirs() }
    val scale = args.getOrElse(1) { "0.5" }.toDouble()
    val filter = args.getOrElse(2) { "" }
    val renderer = Java2DRenderer(File("../../app/src/main/assets/fonts"))
    val w = (RenderContext.REFERENCE_WIDTH * scale).roundToInt()
    val h = (RenderContext.REFERENCE_HEIGHT * scale).roundToInt()

    val entries = Catalog.entries.filter { filter.isBlank() || it.id.contains(filter) }
    val images = entries.map { e ->
        val t0 = System.nanoTime()
        val scene = e.render(RenderContext(w, h, e.defaultPalette, e.defaultSeed))
        val img = renderer.render(scene)
        ImageIO.write(img, "png", File(out, "${e.id}.png"))
        println("%-28s %4d ms  %d items".format(e.id, (System.nanoTime() - t0) / 1_000_000, scene.items.size))
        e to img
    }

    // Contact sheet: 6 per row at a quarter of the rendered size.
    val cols = 6
    val tw = w / 3
    val th = h / 3
    val label = 28
    val rows = (images.size + cols - 1) / cols
    val sheet = BufferedImage(cols * tw, rows * (th + label), BufferedImage.TYPE_INT_RGB)
    val g = sheet.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.color = Color(24, 24, 24)
    g.fillRect(0, 0, sheet.width, sheet.height)
    images.forEachIndexed { i, (e, img) ->
        val x = (i % cols) * tw
        val y = (i / cols) * (th + label)
        g.drawImage(img, x, y, tw, th, null)
        g.color = Color.WHITE
        g.drawString(e.id, x + 6, y + th + 18)
    }
    g.dispose()
    ImageIO.write(sheet, "png", File(out, "_contact_sheet.png"))
    println("Wrote ${images.size} previews to ${out.absolutePath}")
}
