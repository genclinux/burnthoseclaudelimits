package com.noor.wallpapers.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniJsonTest {
    @Test
    fun parsesNestedValues() {
        val v = MiniJson.parse("""{"a": [1, 2.5, -3e2], "b": {"c": "x\"yç\n"}, "d": true, "e": null}""") as Map<*, *>
        assertEquals(listOf(1.0, 2.5, -300.0), v["a"])
        assertEquals("x\"yç\n", (v["b"] as Map<*, *>)["c"])
        assertEquals(true, v["d"])
        assertTrue(v.containsKey("e"))
        assertNull(v["e"])
    }

    @Test
    fun toleratesByteOrderMarkAndWhitespace() {
        assertEquals(emptyList<Any>(), MiniJson.parse("﻿  [ ]  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTrailingGarbage() {
        MiniJson.parse("[1] x")
    }

    @Test
    fun numbersAndStringsReadAsText() {
        val m = MiniJson.parse("""{"id": 9541, "s": " 2 ", "f": 3.5}""") as Map<*, *>
        assertEquals("9541", m.text("id"))
        assertEquals("2", m.text("s"))
        assertEquals("3.5", m.text("f"))
        assertNull(m.text("missing"))
    }
}
