package com.soywiz.korag.format.internal

import com.soywiz.korio.util.*
import kotlin.math.*

internal fun StrReader.skipSpaces2() = this.apply { this.skipWhile { it == ' ' || it == '\t' } }

internal fun StrReader.tryReadInt(default: Int): Int {
    var digitCount = 0
    var integral = 0
    var mult = 1
    loop@ while (!eof) {
        when (val c = peek()) {
            '-' -> {
                skip(1)
                mult *= -1
            }
            in '0'..'9' -> {
                val digit = c - '0'
                skip(1)
                digitCount++
                integral *= 10
                integral += digit
            }
            else -> {
                break@loop
            }
        }
    }
    return if (digitCount == 0) default else integral
}

internal fun StrReader.tryReadNumber(default: Double = Double.NaN): Double {
    var digitCount = 0
    var integral = 0.0
    var decimal = 0.0
    var decimalCount = 0
    var dec = false
    var mult = 1.0
    loop@ while (!eof) {
        when (val c = peek()) {
            '-' -> {
                skip(1)
                mult *= -1.0
            }
            '.' -> {
                skip(1)
                dec = true
            }
            in '0'..'9' -> {
                val digit = c - '0'
                skip(1)
                digitCount++
                if (dec) {
                    decimal *= 10
                    decimal += digit
                    decimalCount++
                } else {
                    integral *= 10
                    integral += digit
                }
            }
            else -> {
                break@loop
            }
        }
    }
    return if (digitCount == 0) default else (integral + (decimal / 10.0.pow(decimalCount))) * mult
}

// Allocation-free matching
internal fun StrReader.tryExpect2(str: String): Boolean {
    for (n in 0 until str.length) {
        if (this.peekOffset(n) != str[n]) return false
    }
    skip(str.length)
    return true
}

internal fun StrReader.peekOffset(offset: Int = 0): Char = this.str.getOrElse(pos + offset) { '\u0000' }
