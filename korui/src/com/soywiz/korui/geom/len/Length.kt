package com.soywiz.korui.geom.len

import com.soywiz.korui.geom.IRectangle

//sealed class Length : Comparable<Length> {
sealed class Length {
	abstract class Fixed() : Length()
	abstract class Variable() : Length()

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

	data class Ratio(val ratio: Double) : Variable() {
		override fun calc(size: Int): Int = (ratio * size).toInt()
		override fun toString() = "${ratio * 100}%"
	}

	data class Binop(val a: Length, val b: Length, val op: String, val act: (Int, Int) -> Int) : Length() {
		override fun calc(size: Int): Int = act(a.calc(size), b.calc(size))
		override fun toString() = "($a $op $b)"
	}

	data class Scale(val a: Length, val scale: Double) : Length() {
		override fun calc(size: Int): Int = (a.calc(size) * scale).toInt()
		override fun toString() = "($a * $scale)"
	}

	abstract fun calc(size: Int): Int

	companion object {
		val AUTO = Ratio(1.0)
		val ZERO = PT(0)
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

fun Length?.calcMin(size: Int): Int = this?.calc(size) ?: 0
fun Length?.calcMax(size: Int): Int = this?.calc(size) ?: size

//operator fun Length?.plus(that: Length?): Length? = Length.Binop(this, that, "+") { a, b -> a + b }
//operator fun Length?.minus(that: Length?): Length? = Length.Binop(this, that, "-") { a, b -> a - b }
operator fun Length?.times(that: Double): Length? = Length.Scale(this ?: Length.AUTO, that)

fun IRectangle.set(bounds: IRectangle, x: Length?, y: Length?, width: Length?, height: Length?) = this.set(
	x?.calc(bounds.width) ?: bounds.x,
	y?.calc(bounds.height) ?: bounds.y,
	width?.calc(bounds.width) ?: bounds.width,
	height?.calc(bounds.height) ?: bounds.height
)

fun IRectangle.setBounds(bounds: IRectangle, left: Length?, top: Length?, right: Length?, bottom: Length?) = this.setBounds(
	left?.calc(bounds.width) ?: bounds.left,
	top?.calc(bounds.height) ?: bounds.top,
	right?.calc(bounds.width) ?: bounds.right,
	bottom?.calc(bounds.height) ?: bounds.bottom
)
