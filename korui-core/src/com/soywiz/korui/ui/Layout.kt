package com.soywiz.korui.ui

import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.IRectangle
import com.soywiz.korim.geom.ISize
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.calcMax
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.setBounds
import com.soywiz.korui.style.*

open class Layout {
	open fun calculateSize(parent: Component, out: ISize = ISize(0, 0)): ISize {
		//parent.children
		return out
	}

	open fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
	}

	enum class ScaleMode2 { NEVER, SHRINK, ALWAYS }

	fun <T> genAxisBounds(size: Int, list: Iterable<T>, itemSize: T.(Int) -> Int, paddingPrev: T.() -> Length?, paddingNext: T.() -> Length?, scaled: ScaleMode2): List<Pair<T, IntRange>> {
		var pos = 0
		var lastPadding = 0
		val out = arrayListOf<Pair<T, IntRange>>()
		for (item in list) {
			val itemPaddingPrev = Math.max(lastPadding, item.paddingPrev().calcMax(size))
			val itemSizeSize = item.itemSize(size)
			if (lastPadding != 0) pos += itemPaddingPrev
			val start = pos
			pos += itemSizeSize
			val end = pos
			out += item to start..end
			lastPadding = item.paddingNext().calcMax(size)
		}

		val scaleFit = size.toDouble() / pos.toDouble()

		val scale = when (scaled) {
			ScaleMode2.SHRINK -> if (pos > size) scaleFit else 1.0
			ScaleMode2.ALWAYS -> scaleFit
			ScaleMode2.NEVER -> 1.0
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

class LayeredKeepAspectLayout(val anchor: Anchor, val scaleMode: ScaleMode = ScaleMode.SHOW_ALL) : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val actualBounds = IRectangle().setBounds(
			bounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)

		for (child in children) {
			val width = child.computedWidth.calcMax(actualBounds.width)
			val height = child.computedHeight.calcMax(actualBounds.height)

			val asize = ISize(width, height).applyScaleMode(actualBounds.size, scaleMode)

			child.actualBounds.set(asize.anchoredIn(actualBounds, anchor))
		}
	}
}

abstract class VerticalHorizontalLayout(val vertical: Boolean) : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val (_, _, width, height) = bounds
		//val width2 = width - parent.style.computedPaddingRight.calc(width)
		//val height2 = height - parent.style.computedPaddingBottom.calc(height)

		val paddingPrev = if (vertical) parent.style.computedPaddingTop else parent.style.computedPaddingLeft
		val paddingNext = if (vertical) parent.style.computedPaddingBottom else parent.style.computedPaddingRight
		val side = if (vertical) height else width

		val posList = genAxisBounds(
			width, children,
			{ if (vertical) this.computedCalcHeight(it) else this.computedCalcWidth(it) },
			{ paddingPrev },
			{ paddingNext },
			scaled = if (vertical) ScaleMode2.SHRINK else ScaleMode2.ALWAYS
		)
		//val maxSide: Int = children.map {
		//	if (vertical) {
		//		parent.style.computedPaddingLeftPlusRight.calc(width) + parent.style.computedWidth.calc(width)
		//	} else {
		//		parent.style.computedPaddingTopPlusBottom.calc(height) + parent.style.computedHeight.calc(height)
		//	}
		//}.max() ?: 0

		for ((child, range) in posList) {
			val rangeStart = range.start
			val rangeLen = range.endInclusive - range.start
			if (vertical) {
				child.actualBounds.set(0, rangeStart, width, rangeLen)
			} else {
				child.actualBounds.set(rangeStart, 0, rangeLen, height)
			}
		}

		if (vertical) {
			parent.actualBounds.width = width
		} else {
			parent.actualBounds.height = height
		}
	}
}

object VerticalLayout : VerticalHorizontalLayout(vertical = true) {
}

object HorizontalLayout : VerticalHorizontalLayout(vertical = false) {

}

object InlineLayout : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val (_, _, width, height) = bounds

		val posList = genAxisBounds(
			width, children,
			{ this.computedCalcWidth(it) },
			{ parent.style.computedPaddingLeft },
			{ parent.style.computedPaddingRight },
			scaled = ScaleMode2.NEVER
		)

		var maxheight = 0
		for ((child, range) in posList) {
			val rangeStart = range.start
			val rangeLen = range.endInclusive - range.start
			val cheight = child.computedHeight.calcMax(height)
			maxheight = Math.max(maxheight, cheight)
			child.actualBounds.set(rangeStart, 0, rangeLen, cheight)
		}

		parent.actualBounds.width = width
		parent.actualBounds.height = maxheight
	}
}

object RelativeLayout : Layout() {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		val (_, _, parentWidth, parentHeight) = bounds

		val childrenSet = HashSet(children.toList())
		val computed = hashSetOf<Component>()

		fun compute(c: Component): Component {
			if (c in computed) return c
			computed += c
			if (c !in childrenSet) return c

			val relativeTo = c.computedRelativeTo
			val cw = c.computedCalcWidth(bounds.width)
			val ch = c.computedCalcHeight(bounds.width)
			val cComputedLeft = c.computedLeft
			val cComputedTop = c.computedTop
			val cComputedRight = c.computedRight
			val cComputedBottom = c.computedBottom

			c.actualBounds.setSize(cw, ch)

			if (cComputedLeft != null) {
				val leftRelative = if (relativeTo != null) compute(relativeTo).actualBounds.right else 0
				c.actualBounds.x = leftRelative + cComputedLeft.calc(parentWidth)
			} else if (cComputedRight != null) {
				val rightRelative = if (relativeTo != null) compute(relativeTo).actualBounds.left else parentWidth
				c.actualBounds.x = rightRelative - cComputedRight.calc(parentWidth) - c.actualBounds.width
			} else {
				c.actualBounds.x = if (relativeTo != null) compute(relativeTo).actualBounds.x else 0
			}

			if (cComputedTop != null) {
				val topRelative = if (relativeTo != null) compute(relativeTo).actualBounds.bottom else 0
				c.actualBounds.y = topRelative + cComputedTop.calc(parentHeight)
			} else if (cComputedBottom != null) {
				val bottomRelative = if (relativeTo != null) compute(relativeTo).actualBounds.top else parentHeight
				c.actualBounds.y = bottomRelative - cComputedBottom.calc(parentHeight) - c.actualBounds.height
			} else {
				c.actualBounds.y = if (relativeTo != null) compute(relativeTo).actualBounds.y else 0
			}

			return c
		}

		for (c in children) {
			compute(c)
		}

		parent.actualBounds.width = parentWidth
		parent.actualBounds.height = parentHeight
	}
}