package com.noor.wallpapers.prayer

/**
 * A small JSON reader: objects become Map<String, Any?>, arrays List<Any?>,
 * numbers Double, plus String, Boolean and null.
 *
 * The prayer-time code is plain Kotlin so it can be tested on a desktop JVM,
 * where Android's org.json is only a stub, and the responses are tiny.
 */
object MiniJson {
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWs()
        val v = p.value()
        p.skipWs()
        if (p.i != text.length) throw IllegalArgumentException("Trailing data at ${p.i}")
        return v
    }

    private class Parser(val s: String) {
        var i = 0

        fun skipWs() {
            while (i < s.length && (s[i].isWhitespace() || s[i] == '﻿')) i++
        }

        fun value(): Any? {
            skipWs()
            if (i >= s.length) throw IllegalArgumentException("Unexpected end of JSON")
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) num() else throw IllegalArgumentException("Unexpected '$c' at $i")
            }
        }

        private fun obj(): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            i++ // {
            skipWs()
            if (peek() == '}') { i++; return out }
            while (true) {
                skipWs()
                if (peek() != '"') throw IllegalArgumentException("Expected key at $i")
                val key = str()
                skipWs()
                expect(':')
                out[key] = value()
                skipWs()
                when (peek()) {
                    ',' -> i++
                    '}' -> { i++; return out }
                    else -> throw IllegalArgumentException("Expected , or } at $i")
                }
            }
        }

        private fun arr(): List<Any?> {
            val out = ArrayList<Any?>()
            i++ // [
            skipWs()
            if (peek() == ']') { i++; return out }
            while (true) {
                out += value()
                skipWs()
                when (peek()) {
                    ',' -> i++
                    ']' -> { i++; return out }
                    else -> throw IllegalArgumentException("Expected , or ] at $i")
                }
            }
        }

        private fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw IllegalArgumentException("Unterminated string")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) throw IllegalArgumentException("Bad escape")
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw IllegalArgumentException("Bad unicode escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> throw IllegalArgumentException("Bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun num(): Double {
            val start = i
            if (peek() == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
            return s.substring(start, i).toDouble()
        }

        private fun literal(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw IllegalArgumentException("Unexpected token at $i")
            i += word.length
            return v
        }

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

        private fun expect(c: Char) {
            if (peek() != c) throw IllegalArgumentException("Expected '$c' at $i")
            i++
        }
    }
}

/** A field as text, whether the API sent it as a string or a number. */
internal fun Map<*, *>.text(key: String): String? = when (val v = this[key]) {
    null -> null
    is String -> v.trim().takeIf { it.isNotEmpty() }
    is Double -> if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
    else -> v.toString()
}
