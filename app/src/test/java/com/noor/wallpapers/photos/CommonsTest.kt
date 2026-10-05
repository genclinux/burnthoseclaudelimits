package com.noor.wallpapers.photos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonsTest {
    private val place = Places.byId("selimiye")!!

    private fun page(
        id: Int,
        title: String,
        w: Int = 3000,
        h: Int = 4000,
        mime: String = "image/jpeg",
        license: String = "CC BY-SA 4.0",
        artist: String = "<a href=\\\"//commons.wikimedia.org/wiki/User:Ali\\\">Ali &amp; Veli</a>",
        cats: List<String> = emptyList(),
    ): String {
        val name = title.removePrefix("File:").replace(' ', '_')
        val thumb = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/$name/1920px-$name"
        val catJson = cats.joinToString(",") { "{\"ns\":14,\"title\":\"$it\"}" }
        return """{"pageid":$id,"ns":6,"title":"$title","index":$id,
            "categories":[$catJson],
            "imageinfo":[{"width":$w,"height":$h,"mime":"$mime",
              "url":"https://upload.wikimedia.org/wikipedia/commons/a/ab/$name",
              "thumburl":"$thumb","thumbwidth":1920,"thumbheight":2560,
              "descriptionurl":"https://commons.wikimedia.org/wiki/$title",
              "extmetadata":{"Artist":{"value":"$artist","source":"commons-desc-page"},
                "LicenseShortName":{"value":"$license","source":"commons-desc-page"},
                "LicenseUrl":{"value":"https://creativecommons.org/licenses/by-sa/4.0","source":"commons-desc-page"}}}]}"""
    }

    private fun response(vararg pages: String) = """{"batchcomplete":true,"query":{"pages":[${pages.joinToString(",")}]}}"""

    @Test
    fun parsesCreditsAndRanksFeaturedThenQualityThenUpright() {
        val photos = Commons.parse(
            response(
                page(1, "File:Selimiye wide.jpg", w = 6000, h = 4000),
                page(2, "File:Selimiye upright.jpg", w = 3000, h = 4500),
                page(3, "File:Selimiye QI.jpg", w = 4000, h = 3000, cats = listOf("Category:Quality images")),
                page(4, "File:Selimiye FP.jpg", w = 4000, h = 3000, cats = listOf("Category:Featured pictures on Wikimedia Commons")),
            ),
            place,
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), photos.map { it.pageId })
        val first = photos.first()
        assertEquals("Ali & Veli", first.artist)
        assertEquals("CC BY-SA 4.0", first.license)
        assertEquals("photo:4", first.id)
        assertEquals(place, first.place)
    }

    @Test
    fun dropsUnfreeTinyOddShapedNonJpegAndMaps() {
        val photos = Commons.parse(
            response(
                page(1, "File:Ok.jpg"),
                page(2, "File:NC.jpg", license = "CC BY-NC-SA 2.0"),
                page(3, "File:ND.jpg", license = "CC BY-ND 4.0"),
                page(4, "File:Small.jpg", w = 800, h = 600),
                page(5, "File:Panorama.jpg", w = 9000, h = 2000),
                page(6, "File:Png.png", mime = "image/png"),
                page(7, "File:Selimiye map of Edirne.jpg"),
                page(8, "File:GFDL.jpg", license = "GFDL"),
                page(9, "File:PD.jpg", license = "Public domain"),
                page(10, "File:Zero.jpg", license = "CC0"),
            ),
            place,
        )
        assertEquals(setOf(1L, 9L, 10L), photos.map { it.pageId }.toSet())
    }

    @Test
    fun picksAThumbnailWidthThatCoversTheScreen() {
        val p = Commons.parse(response(page(1, "File:Landscape.jpg", w = 6000, h = 4000)), place).single()
        // A 1272 x 2772 portrait screen needs the landscape photo 4158 px wide: more than 3840, so the original.
        assertEquals(4158, p.widthToCover(1272, 2772))
        assertEquals(p.url, p.urlForWidth(4158))
        assertTrue(p.urlForWidth(500).endsWith("/500px-Landscape.jpg"))
        assertTrue(p.urlForWidth(1500).endsWith("/1920px-Landscape.jpg"))
        assertTrue(p.urlForWidth(2000).endsWith("/3840px-Landscape.jpg"))
    }

    @Test
    fun choosesEachPlacesBestOnceInPlaceOrder() {
        val a = Place("a", "A", Region.PENDIK, "a", 2)
        val b = Place("b", "B", Region.WORLD, "b", 1)
        val pa = Commons.parse(response(page(1, "File:One.jpg"), page(2, "File:Two.jpg"), page(3, "File:Three.jpg")), a)
        val pb = Commons.parse(response(page(1, "File:One.jpg"), page(4, "File:Four.jpg")), b)
        val chosen = Commons.choose(listOf(a, b), mapOf("a" to pa, "b" to pb))
        assertEquals(3, chosen.size)
        assertEquals(listOf("a", "a", "b"), chosen.map { it.place.id })
        assertFalse(chosen.count { it.pageId == 1L } > 1)
    }

    @Test
    fun licencesAndBrokenResponses() {
        assertTrue(Commons.freeLicense("CC BY 2.0"))
        assertTrue(Commons.freeLicense("CC-BY-SA-3.0"))
        assertFalse(Commons.freeLicense("CC BY-NC 2.0"))
        assertFalse(Commons.freeLicense("All rights reserved"))
        assertTrue(Commons.parse("{\"batchcomplete\":true}", place).isEmpty())
        assertNull(Places.byId("nope"))
    }

    @Test
    fun pendikComesFirstAndMost() {
        assertEquals(Region.PENDIK, Places.ALL.first().region)
        val pendik = Places.ALL.filter { it.region == Region.PENDIK }.sumOf { it.take }
        val istanbul = Places.ALL.filter { it.region == Region.ISTANBUL }.sumOf { it.take }
        assertTrue(pendik > istanbul)
        assertEquals(Places.ALL.size, Places.ALL.map { it.id }.toSet().size)
        assertTrue(Commons.searchUrl(place).contains("filetype%3Abitmap"))
    }
}
