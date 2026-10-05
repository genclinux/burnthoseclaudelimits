package com.noor.wallpapers.art

import org.junit.Assert.assertFalse
import org.junit.Test

class EbruPaletteTest {
    @Test
    fun paletteChangesTheMarbling() {
        val e = Catalog.byId("ebru-battal-klasik")!!
        fun pixels(p: String) = (e.render(RenderContext(120, 240, Palette.byId(p), 3)).items.first() as RasterItem).pixels
        assertFalse(pixels("emerald").contentEquals(pixels("lapis")))
    }
}
