package com.soywiz.korui.ui

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korui.geom.IRectangle
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.geom.len.px
import com.soywiz.korui.light.LightComponents

interface LayoutBase {
	fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {

	}
}

open class Layout(lc: LightComponents) : Container(lc, LightComponents.TYPE_CONTAINER) {
	init {
		style.size.setTo(100.percent, 100.percent)
		style.padding.setTo(16.px)
	}
}

open class VerticalLayout(lc: LightComponents) : Layout(lc) {
	init {
		style.size.setTo(100.percent, 100.percent)
	}

	@JTranscKeep
	override fun relayoutInternal() {
		//println("vertical: relayout ${children.size}")
		val (_, _, width, height) = actualBounds
		//var y = this.actualBounds.y
		var y2 = 0
		//println("vertical: relayout ${children.size}")
		for (child in children) {
			//println("child: $child ${child.width}x${child.height}")
			child.actualBounds.set(0, y2, child.style.computedWidth.calc(width), child.style.computedHeight.calc(height))
			y2 += child.actualBounds.height
		}
	}
}

open class HorizontalLayout(lc: LightComponents) : Layout(lc) {
	init {
		style.size.setTo(100.percent, 32.pt)
	}

	@JTranscKeep
	override fun relayoutInternal() {
		//println("vertical: relayout ${children.size}")
		val (_, _, width, height) = actualBounds
		//var y = this.actualBounds.y
		//println("vertical: relayout ${children.size}")

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
		actualBounds.width = x2
		actualBounds.height = maxy
	}
}
