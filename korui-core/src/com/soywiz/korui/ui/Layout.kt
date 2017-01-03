package com.soywiz.korui.ui

import com.soywiz.korui.geom.IRectangle
import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.setBounds
import com.soywiz.korui.style.*

open class Layout {
	open fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
	}

	fun <T> genAxisBounds(size: Int, list: Iterable<T>, itemSize: T.() -> Length, paddingPrev: T.() -> Length, paddingNext: T.() -> Length): List<Pair<T, IntRange>> {
		var pos = 0
		var lastPadding = 0
		val out = arrayListOf<Pair<T, IntRange>>()
		for (item in list) {
			val itemPaddingPrev = Math.max(lastPadding, item.paddingPrev().calc(size))
			val itemSizeSize = item.itemSize().calc(size)
			if (lastPadding != 0) pos += itemPaddingPrev
			val start = pos
			pos += itemSizeSize
			val end = pos
			out += item to start..end
			lastPadding = item.paddingNext().calc(size)
		}

		val scale = if (pos > size) {
			size.toDouble() / pos.toDouble()
			//1.0
		} else {
			1.0
		}

		return out.map { (item, range) ->
			item to IntRange((range.start * scale).toInt(), (range.endInclusive * scale).toInt())
		}
	}
}

object LayeredLayout : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val actualBounds = IRectangle().setBounds(
			bounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)

		for (child in children) {
			child.actualBounds.set(actualBounds)
		}
	}
}

abstract class VerticalHorizontalLayout(val vertical: Boolean) : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val (_, _, width, height) = bounds

		val posList = genAxisBounds(width, children, {
			if (vertical) this.style.computedHeight else this.style.computedWidth
		}, {
			if (vertical) parent.style.computedPaddingTop else parent.style.computedPaddingLeft
		}, {
			if (vertical) parent.style.computedPaddingBottom else parent.style.computedPaddingRight
		})
		//val maxSide: Int = children.map {
		//	if (vertical) {
		//		parent.style.computedPaddingLeftPlusRight.calc(width) + parent.style.computedWidth.calc(width)
		//	} else {
		//		parent.style.computedPaddingTopPlusBottom.calc(height) + parent.style.computedHeight.calc(height)
		//	}
		//}.max() ?: 0

		for ((child, range) in posList) {
			if (vertical) {
				child.actualBounds.set(
					0,
					range.start,
					width,
					range.endInclusive - range.start
				)
			} else {
				child.actualBounds.set(
					range.start,
					0,
					range.endInclusive - range.start,
					height
				)
			}
		}

		//parent.actualBounds.width =
		if (vertical) {
			parent.actualBounds.width = width
		} else{
			parent.actualBounds.height = height
		}

		/*
		val widths = children.map { it.style.computedWidth.calc(width) }
		val totalWidth = widths.sum()
		val scaleRatio = if (totalWidth > width) width.toDouble() / totalWidth.toDouble() else 1.0

		var x2 = 0
		var maxy = 0
		for (child in children) {
			child.actualBounds.set(x2, 0, (child.style.computedWidth.calc(width) * scaleRatio).toInt(), child.style.computedHeight.calc(height))
			x2 += child.actualBounds.width
			maxy = Math.max(maxy, child.actualBounds.height)
		}
		*/
	}
}

object VerticalLayout : VerticalHorizontalLayout(vertical = true) {
}

object HorizontalLayout : VerticalHorizontalLayout(vertical = false) {
}
