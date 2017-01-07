package com.soywiz.korui.geom.len

import com.soywiz.korim.geom.IRectangle
import com.soywiz.korio.util.clamp

//sealed class Length : Comparable<Length> {

// http://www.w3schools.com/cssref/css_units.asp

sealed class Length {
	abstract class Fixed() : Length()
	abstract class Variable() : Length()

	//class Context {
	//	var fontSize: Double = 16.0
	//	var dpp: Double = 1.0
	//	var viewportWidth: Double = 1.0
	//	var viewportHeight: Double = 1.0
	//	var size: Double = 100.0
	//}


	data class MM(val v: Int) : Fixed() {
		override fun calc(size: Int): Int = v
		override fun toString() = "${v}mm"
	}

	//data class PX(val v: Int) : Fixed() {
	//	override fun calc(size: Int): Int = v
	//	override fun toString() = "${v}px"
	//}

	data class PT(val v: Int) : Fixed() {
		override fun calc(size: Int): Int = v
		override fun toString() = "${v}pt"
	}

	data class EM(val v: Int) : Fixed() {
		override fun calc(size: Int): Int = v
		override fun toString() = "${v}em"
	}

	data class Ratio(val ratio: Double) : Variable() {
		override fun calc(size: Int): Int = (ratio * size).toInt()
		override fun toString() = "${ratio * 100}%"
	}

	data class Binop(val a: Length, val b: Length, val op: String, val act: (Int, Int) -> Int) : Length() {
		override fun calc(size: Int): Int = act(a.calc(size), b.calc(size))
		override fun toString() = "($a $op $b)"
	}

	data class Scale(val a: Length?, val scale: Double) : Length() {
		override fun calc(size: Int): Int = (a.calcMax(size) * scale).toInt()
		override fun toString() = "($a * $scale)"
	}

	abstract fun calc(size: Int): Int

	companion object {
		val ZERO = PT(0)

		fun calc(length: Int, default: Length, size: Length?, min: Length? = null, max: Length? = null, ignoreBounds: Boolean = false): Int {
			val sizeCalc = (size ?: default).calc(length)
			val minCalc = min.calcMin(length, if (ignoreBounds) Int.MIN_VALUE else 0)
			val maxCalc = max.calcMax(length, if (ignoreBounds) Int.MAX_VALUE else length)
			return sizeCalc.clamp(minCalc, maxCalc)
		}
	}

	operator fun plus(that: Length): Length = Length.Binop(this, that, "+") { a, b -> a + b }
	operator fun minus(that: Length): Length = Length.Binop(this, that, "-") { a, b -> a - b }
	operator fun times(that: Double): Length = Length.Scale(this, that)
	operator fun times(that: Int): Length = Length.Scale(this, that.toDouble())
}

object MathEx {
	fun <T : Comparable<T>> min(a: T, b: T): T = if (a.compareTo(b) < 0) a else b
	fun <T : Comparable<T>> max(a: T, b: T): T = if (a.compareTo(b) > 0) a else b
}

//fun Length?.calc(size: Int, default: Int): Int = this?.calc(size) ?: default

fun Length?.calcMin(size: Int, default: Int = 0): Int = this?.calc(size) ?: default
fun Length?.calcMax(size: Int, default: Int = size): Int = this?.calc(size) ?: default

//operator fun Length?.plus(that: Length?): Length? = Length.Binop(this, that, "+") { a, b -> a + b }
//operator fun Length?.minus(that: Length?): Length? = Length.Binop(this, that, "-") { a, b -> a - b }
operator fun Length?.times(that: Double): Length? = Length.Scale(this, that)

fun IRectangle.setTo(bounds: IRectangle, x: Length?, y: Length?, width: Length?, height: Length?) = this.setTo(
	x?.calc(bounds.width) ?: bounds.x,
	y?.calc(bounds.height) ?: bounds.y,
	width?.calc(bounds.width) ?: bounds.width,
	height?.calc(bounds.height) ?: bounds.height
)

fun IRectangle.setBoundsTo(bounds: IRectangle, left: Length?, top: Length?, right: Length?, bottom: Length?) = this.setBoundsTo(
	left?.calc(bounds.width) ?: bounds.left,
	top?.calc(bounds.height) ?: bounds.top,
	right?.calc(bounds.width) ?: bounds.right,
	bottom?.calc(bounds.height) ?: bounds.bottom
)
