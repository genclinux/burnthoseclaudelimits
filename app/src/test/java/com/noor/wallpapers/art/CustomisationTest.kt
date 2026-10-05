package com.noor.wallpapers.art

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CustomisationTest {
    @Test
    fun ebcedOfHerNamesAndTheTulip() {
        assertEquals(153, Ebced.value(Ebced.HANIFE))
        assertEquals(438, Ebced.value(Ebced.BETUL))
        assertEquals(591, Ebced.value(Ebced.HANIFE + " " + Ebced.BETUL))
        // Lâle, hilâl and Allah share their letters, and so their value.
        assertEquals(66, Ebced.value("الله"))
        assertEquals(66, Ebced.value("لاله"))
        assertEquals(66, Ebced.value("هلال"))
        // Vowel marks count nothing.
        assertEquals(Ebced.value("الله"), Ebced.value("ٱللَّٰه") + 0)
    }

    @Test
    fun customPalettesLiveInTheirIds() {
        val p = PaletteMaker.make(Colors.hex("#123A44"), Colors.hex("#E8B4A0"), Colors.hex("#C97B84"))
        assertEquals("c-123A44-E8B4A0-C97B84", p.id)
        assertTrue(p.isCustom)
        val back = Palette.byId(p.id)
        assertEquals(p, back)
        assertEquals("emerald", Palette.byId("emerald").id)
        assertEquals(Palette.ALL[0], Palette.byId("c-nonsense"))
        val r = PaletteMaker.random(Random(3))
        assertEquals(r, Palette.byId(r.id))
    }

    @Test
    fun optionsRoundTrip() {
        val o = DesignOptions(captions = false, texture = 0.4f, dim = 0.25f, text = "Rabbim | kolaylaştır; ✨")
        val enc = o.encode()
        assertTrue('|' !in enc)
        assertEquals(o, DesignOptions.decode(enc))
        assertEquals("", DesignOptions.DEFAULT.encode())
        assertEquals(DesignOptions.DEFAULT, DesignOptions.decode(null))
        assertEquals(DesignOptions.MAX_DIM, DesignOptions.decode("d95").dim)
    }

    @Test
    fun wrapsHerTextIntoBalancedLines() {
        assertEquals(listOf("Yolun nur olsun"), CustomTextArt.wrap("Yolun nur olsun"))
        val lines = CustomTextArt.wrap("Rabbim göğsümü aç, işimi kolaylaştır, dilimdeki düğümü çöz")
        assertTrue(lines.size in 2..4)
        assertTrue(lines.all { it.length <= 26 })
        assertEquals(listOf("Bir", "iki"), CustomTextArt.wrap("Bir\n\niki"))
        assertEquals(5, CustomTextArt.wrap((1..40).joinToString(" ") { "kelime" }).size)
        assertEquals(listOf(CustomTextArt.DEFAULT_TEXT), CustomTextArt.wrap("   "))
    }

    @Test
    fun secretsStayHiddenUntilFound() {
        assertTrue(Catalog.visible(false).none { it.secret })
        assertTrue(Catalog.visible(true).any { it.id == Catalog.SECRET })
        assertTrue(Catalog.entries.map { it.id }.toSet().size == Catalog.entries.size)
    }

    @Test
    fun dimDarkensTheWholeScene() {
        val e = Catalog.byId("geo-khatam")!!
        val plain = e.render(RenderContext(200, 400, e.defaultPalette, 1))
        val dimmed = e.render(RenderContext(200, 400, e.defaultPalette, 1, options = DesignOptions(dim = 0.3f)))
        assertEquals(plain.items.size + 1, dimmed.items.size)
    }
}
