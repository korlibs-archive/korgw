package com.soywiz.korui.style

import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.Padding
import com.soywiz.korui.geom.len.Position
import com.soywiz.korui.geom.len.Size

class Style(var parent: Style? = null) : Styled {
	var position = Position(null, null)
	val size = Size(null, null)
	val padding = Padding(null)

	override val style: Style = this
}

fun Style(callback: Style.() -> Unit): Style = Style().apply(callback)

interface Styled {
	val style: Style
}

val Styled.computedX: Length get() = style.position.x ?: style.parent?.computedX ?: Length.ZERO
val Styled.computedY: Length get() = style.position.y ?: style.parent?.computedY ?: Length.ZERO

var Styled.width: Length? get() = style.size.width; set(v) = run { style.size.width = v }
var Styled.height: Length? get() = style.size.height; set(v) = run { style.size.height = v }

var Styled.padding: Padding get() = style.padding; set(value) = run { style.padding.setTo(value) }

val Styled.computedPaddingTop: Length get() = padding.top ?: style.parent?.computedPaddingTop ?: Length.ZERO
val Styled.computedPaddingRight: Length get() = padding.right ?: style.parent?.computedPaddingRight ?: Length.ZERO
val Styled.computedPaddingBottom: Length get() = padding.bottom ?: style.parent?.computedPaddingBottom ?: Length.ZERO
val Styled.computedPaddingLeft: Length get() = padding.left ?: style.parent?.computedPaddingLeft ?: Length.ZERO
val Styled.computedPaddingLeftPlusRight: Length get() = computedPaddingLeft + computedPaddingRight
val Styled.computedPaddingTopPlusBottom: Length get() = computedPaddingTop + computedPaddingBottom
val Styled.computedWidth: Length get() = style.size.width ?: style.parent?.computedWidth ?: Length.AUTO
val Styled.computedHeight: Length get() = style.size.height ?: style.parent?.computedHeight ?: Length.AUTO
