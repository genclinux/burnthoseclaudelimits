package com.noor.wallpapers.preview

import com.noor.wallpapers.art.Catalog
import com.noor.wallpapers.art.RenderContext
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Phone = OPPO Find X9 Pro; tablet = a typical 7:5 tablet (2800 x 2000) both ways up. */
enum class Device(val width: Int, val height: Int, val tablet: Boolean) {
    PHONE(RenderContext.REFERENCE_WIDTH, RenderContext.REFERENCE_HEIGHT, false),
    TABLET_PORTRAIT(2000, 2800, true),
    TABLET_LANDSCAPE(2800, 2000, true),
}

/**
 * Renders every catalog entry to PNG plus a contact sheet per device.
 * Args: <outDir> <scale> [idFilter] [device|all]. Scale 1.0 = native resolution.
 */
fun main(args: Array<String>) {
    val root = File(args.getOrElse(0) { "build/previews" })
    val scale = args.getOrElse(1) { "0.5" }.toDouble()
    val filter = args.getOrElse(2) { "" }
    val devices = args.getOrElse(3) { "all" }.let { d ->
        if (d == "all" || d.isBlank()) Device.entries else listOf(Device.valueOf(d.uppercase()))
    }
    val renderer = Java2DRenderer(File("../../app/src/main/assets/fonts"))
    for (device in devices) {
        val out = if (device == Device.PHONE) root else File(root, device.name.lowercase())
        render(renderer, device, out.apply { mkdirs() }, scale, filter)
    }
}

private fun render(renderer: Java2DRenderer, device: Device, out: File, scale: Double, filter: String) {
    val w = (device.width * scale).roundToInt()
    val h = (device.height * scale).roundToInt()

    val entries = Catalog.entries.filter { filter.isBlank() || it.id.contains(filter) }
    val images = entries.map { e ->
        val t0 = System.nanoTime()
        val ctx = if (device.tablet) {
            RenderContext.tablet(w, h, minOf(w, h).toDouble(), e.defaultPalette, e.defaultSeed)
        } else {
            RenderContext(w, h, e.defaultPalette, e.defaultSeed)
        }
        val scene = e.render(ctx)
        val img = renderer.render(scene)
        ImageIO.write(img, "png", File(out, "${e.id}.png"))
        println("%-18s %-28s %4d ms".format(device, e.id, (System.nanoTime() - t0) / 1_000_000))
        e to img
    }

    // Contact sheet at a third of the rendered size.
    val cols = if (w > h) 4 else 6
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
