package com.noor.wallpapers.art

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vec(val x: Double, val y: Double) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(s: Double) = Vec(x * s, y * s)
    operator fun div(s: Double) = Vec(x / s, y / s)
    operator fun unaryMinus() = Vec(-x, -y)

    infix fun dot(o: Vec) = x * o.x + y * o.y
    infix fun cross(o: Vec) = x * o.y - y * o.x

    val length get() = hypot(x, y)
    val angle get() = atan2(y, x)
    fun normalized(): Vec = length.let { if (it == 0.0) this else this / it }

    fun rotated(a: Double): Vec {
        val c = cos(a); val s = sin(a)
        return Vec(x * c - y * s, x * s + y * c)
    }

    companion object {
        val ZERO = Vec(0.0, 0.0)
        fun polar(r: Double, a: Double) = Vec(r * cos(a), r * sin(a))
    }
}

/** Intersection of the lines p1 + t*d1 and p2 + u*d2, or null when parallel. */
fun intersectLines(p1: Vec, d1: Vec, p2: Vec, d2: Vec): Vec? {
    val c = d1 cross d2
    if (abs(c) < 1e-9) return null
    val t = ((p2 - p1) cross d2) / c
    return p1 + d1 * t
}

fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
