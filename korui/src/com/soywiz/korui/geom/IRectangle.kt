package com.soywiz.korui.geom

data class IRectangle(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) {
	fun set(that: IRectangle) = set(that.x, that.y, that.width, that.height)

	fun set(x: Int, y: Int, width: Int, height: Int) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun setPosition(x: Int, y: Int) = this.apply {
		this.x = x
		this.y = y
	}

	fun setSize(width: Int, height: Int) = this.apply {
		this.width = width
		this.height = height
	}
}
