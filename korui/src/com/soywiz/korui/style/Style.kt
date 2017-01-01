package com.soywiz.korui.style

import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.Padding
import com.soywiz.korui.geom.len.Position
import com.soywiz.korui.geom.len.Size

class Style(var parent: Style? = null) {
	var position = Position(null, null)
	val size = Size(null, null)
	val padding = Padding(null)

	var width: Length? get() = size.width; set(v) = run  { size.width = v }
	var height: Length? get() = size.height; set(v) = run  { size.height = v }

	val computedPaddingTop: Length get() = padding.top ?: parent?.computedPaddingTop ?: Length.AUTO
	val computedPaddingRight: Length get() = padding.right ?: parent?.computedPaddingRight ?: Length.AUTO
	val computedPaddingBottom: Length get() = padding.bottom ?: parent?.computedPaddingBottom ?: Length.AUTO
	val computedPaddingLeft: Length get() = padding.left ?: parent?.computedPaddingLeft ?: Length.AUTO

	val computedX: Length get() = position.x ?: parent?.computedX ?: Length.ZERO
	val computedY: Length get() = position.y ?: parent?.computedY ?: Length.ZERO

	val computedWidth: Length get() = size.width ?: parent?.computedWidth ?: Length.AUTO
	val computedHeight: Length get() = size.height ?: parent?.computedHeight ?: Length.AUTO
}

fun Style(callback: Style.() -> Unit): Style = Style(callback).apply(callback)

