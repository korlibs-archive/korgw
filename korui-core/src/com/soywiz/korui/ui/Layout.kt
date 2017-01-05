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
	//open fun calculateSize(parent: Component, out: ISize = ISize(0, 0)): ISize {
	//	//parent.children
	//	return out
	//}

	open fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
	}

	fun applyLayout(parent: Component, children: Iterable<Component>, x: Int, y: Int, width: Int, height: Int, out: IRectangle = IRectangle()): IRectangle {
		applyLayout(parent, children, out.set(x, y, width, height))
		return out
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
	override fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
		val actualBounds = IRectangle().setBounds(
			inoutBounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)

		for (child in children) {
			child.setBoundsAndRelayout(actualBounds)
		}
	}
}

class LayeredKeepAspectLayout(val anchor: Anchor, val scaleMode: ScaleMode = ScaleMode.SHOW_ALL) : Layout() {
	override fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
		val actualBounds = IRectangle().setBounds(
			inoutBounds,
			parent.style.computedPaddingLeft, parent.style.computedPaddingTop,
			100.percent - parent.style.computedPaddingRight, 100.percent - parent.style.computedPaddingBottom
		)

		for (child in children) {
			val width = child.computedWidth.calcMax(actualBounds.width)
			val height = child.computedHeight.calcMax(actualBounds.height)

			val asize = ISize(width, height).applyScaleMode(actualBounds.size, scaleMode)

			val endSize = asize.anchoredIn(actualBounds, anchor)
			child.setBoundsAndRelayout(endSize)
			child.setBoundsInternal(endSize)
		}
	}
}

abstract class VerticalHorizontalLayout(val vertical: Boolean) : Layout() {
	override fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
		//val width2 = width - parent.style.computedPaddingRight.calc(width)
		//val height2 = height - parent.style.computedPaddingBottom.calc(height)

		val paddingPrev = if (vertical) parent.style.computedPaddingTop else parent.style.computedPaddingLeft
		val paddingNext = if (vertical) parent.style.computedPaddingBottom else parent.style.computedPaddingRight
		val inboundsSide = if (vertical) inoutBounds.height else inoutBounds.width

		val posList = genAxisBounds(
			inboundsSide, children,
			{ if (vertical) this.computedCalcHeight(it) else this.computedCalcWidth(it) },
			{ paddingPrev },
			{ paddingNext },
			scaled = if (vertical) ScaleMode2.SHRINK else ScaleMode2.ALWAYS
		)

		for ((child, range) in posList) {
			val rangeStart = range.start
			val rangeLen = range.endInclusive - range.start
			if (vertical) {
				child.setBoundsAndRelayout(0, rangeStart, inoutBounds.width, rangeLen)
			} else {
				child.setBoundsAndRelayout(rangeStart, 0, rangeLen, inoutBounds.height)
			}
		}

		if (vertical) {
			inoutBounds.setSize(inoutBounds.width, posList.last().second.endInclusive)
		} else {
			inoutBounds.setSize(posList.last().second.endInclusive, inoutBounds.height)
		}
	}
}

object VerticalLayout : VerticalHorizontalLayout(vertical = true) {
}

object HorizontalLayout : VerticalHorizontalLayout(vertical = false) {
}

object InlineLayout : Layout() {
	override fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
		val posList = genAxisBounds(
			inoutBounds.width, children,
			{ this.computedCalcWidth(it) },
			{ parent.style.computedPaddingLeft },
			{ parent.style.computedPaddingRight },
			scaled = ScaleMode2.NEVER
		)

		var maxheight = 0
		for ((child, range) in posList) {
			val rangeStart = range.start
			val rangeLen = range.endInclusive - range.start
			val cheight = child.computedHeight.calcMax(inoutBounds.height)
			maxheight = Math.max(maxheight, cheight)
			child.setBoundsAndRelayout(rangeStart, 0, rangeLen, cheight)
		}

		inoutBounds.set(inoutBounds.x, inoutBounds.y, inoutBounds.width, maxheight)
	}
}

object RelativeLayout : Layout() {
	override fun applyLayout(parent: Component, children: Iterable<Component>, inoutBounds: IRectangle) {
		val parentWidth = inoutBounds.width
		val parentHeight = inoutBounds.height

		val childrenSet = HashSet(children.toList())
		val computed = hashSetOf<Component>()

		var maxHeight = parentHeight

		fun compute(c: Component): IRectangle {
			if (c in computed) return c.actualBounds
			computed += c
			if (c !in childrenSet) return c.actualBounds

			val relativeTo = c.computedRelativeTo
			val cw = c.computedCalcWidth(parentWidth)
			val ch = c.computedCalcHeight(parentHeight)
			val cComputedLeft = c.computedLeft
			val cComputedTop = c.computedTop
			val cComputedRight = c.computedRight
			val cComputedBottom = c.computedBottom

			val cActualBounds = c.actualBounds
			cActualBounds.setSize(cw, ch)

			if (cComputedLeft != null) {
				val leftRelative = if (relativeTo != null) compute(relativeTo).right else 0
				cActualBounds.x = leftRelative + cComputedLeft.calc(parentWidth)
			} else if (cComputedRight != null) {
				val rightRelative = if (relativeTo != null) compute(relativeTo).left else parentWidth
				cActualBounds.x = rightRelative - cComputedRight.calc(parentWidth) - c.actualBounds.width
			} else {
				cActualBounds.x = if (relativeTo != null) compute(relativeTo).x else 0
			}

			if (cComputedTop != null) {
				val topRelative = if (relativeTo != null) compute(relativeTo).bottom else 0
				cActualBounds.y = topRelative + cComputedTop.calc(parentHeight)
			} else if (cComputedBottom != null) {
				val bottomRelative = if (relativeTo != null) compute(relativeTo).top else parentHeight
				cActualBounds.y = bottomRelative - cComputedBottom.calc(parentHeight) - c.actualBounds.height
			} else {
				cActualBounds.y = if (relativeTo != null) compute(relativeTo).y else 0
			}

			c.setBoundsAndRelayout(cActualBounds)

			maxHeight = Math.max(maxHeight, cActualBounds.height)

			return c.actualBounds
		}

		for (c in children) {
			compute(c)
		}

		//inoutBounds.setSize(parentWidth, maxHeight)
	}
}