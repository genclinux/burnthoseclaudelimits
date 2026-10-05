package com.noor.wallpapers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    @Test
    fun idsAreUniqueAndNewestComesFirst() {
        val ids = WhatsNew.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val versions = WhatsNew.ALL.map { v -> v.version.split('.').map(String::toInt) }
        for ((a, b) in versions.zipWithNext()) {
            val cmp = a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) } ?: 0
            assertTrue("WhatsNew.ALL must be newest first", cmp >= 0)
        }
    }

    @Test
    fun theLatestVersionMatchesTheBuild() {
        // Every release that adds a feature adds its popup; the newest entry is this version's.
        val gradle = java.io.File("build.gradle.kts").takeIf { it.exists() } ?: java.io.File("app/build.gradle.kts")
        val versionName = Regex("versionName = \"([^\"]+)\"").find(gradle.readText())!!.groupValues[1]
        assertEquals(versionName, WhatsNew.latest)
    }

    @Test
    fun someoneUpdatingSeesEverythingTheyMissed() {
        val shown = WhatsNew.toShow(setOf("rotation"), firstInstall = false)
        assertEquals(WhatsNew.ALL.filter { it.id != "rotation" }, shown)
        assertEquals(WhatsNew.ALL.map { it.id }.toSet(), WhatsNew.seenAfter(setOf("rotation"), shown, firstInstall = false))
        assertTrue(WhatsNew.toShow(WhatsNew.ALL.map { it.id }.toSet(), firstInstall = false).isEmpty())
    }

    @Test
    fun aFirstInstallHearsOnlyAboutThisVersion() {
        val shown = WhatsNew.toShow(emptySet(), firstInstall = true)
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.version == WhatsNew.latest })
        // And the older ones never come up later.
        assertEquals(WhatsNew.ALL.map { it.id }.toSet(), WhatsNew.seenAfter(emptySet(), shown, firstInstall = true))
    }
}
