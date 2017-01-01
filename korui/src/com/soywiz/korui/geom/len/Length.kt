package com.soywiz.korui.geom.len

interface Length {
	data class MM(val v: Int) : Length {
		override fun calc(size: Int): Int = v
		override fun scale(ratio: Double): Length = PT((this.v * ratio).toInt())
		override fun toString() = "${v}mm"
	}

	data class PX(val v: Int) : Length {
		override fun calc(size: Int): Int = v
		override fun scale(ratio: Double): Length = PT((this.v * ratio).toInt())
		override fun toString() = "${v}px"
	}

	data class PT(val v: Int) : Length {
		override fun calc(size: Int): Int = v
		override fun scale(ratio: Double): Length = PT((this.v * ratio).toInt())
		override fun toString() = "${v}pt"
	}

	data class Ratio(val ratio: Double) : Length {
		override fun calc(size: Int): Int = (ratio * size).toInt()
		override fun scale(ratio: Double): Length = Ratio(this.ratio * ratio)
		override fun toString() = "${ratio * 100}%"
	}

	fun calc(size: Int): Int
	fun scale(ratio: Double): Length

	companion object {
		val AUTO = Ratio(1.0)
		val ZERO = PX(0)
	}
}
