package com.soywiz.korui.geom.len

data class Padding(var top: Length?, var right: Length?, var bottom: Length?, var left: Length?) {
	constructor(vertical: Length?, horizontal: Length?) : this(vertical, horizontal, vertical, horizontal)
	constructor(pad: Length?) : this(pad, pad, pad, pad)
	constructor() : this(0.px, 0.px, 0.px, 0.px)

	fun setTo(top: Length?, right: Length?, bottom: Length?, left: Length?) = this.apply {
		this.top = top
		this.right = right
		this.bottom = bottom
		this.left = left
	}

	fun setTo(vertical: Length?, horizontal: Length?) = setTo(vertical, horizontal, vertical, horizontal)
	fun setTo(pad: Length?) = setTo(pad, pad, pad, pad)
}
