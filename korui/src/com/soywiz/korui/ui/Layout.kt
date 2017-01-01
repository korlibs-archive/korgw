package com.soywiz.korui.ui

import com.soywiz.korui.geom.IRectangle

interface Layout {
	fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {

	}
}

object EmptyLayout : Layout {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
	}
}

object LayeredLayout : Layout {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		for (child in children) {
			child.actualBounds.set(0, 0, child.style.computedWidth.calc(bounds.width), child.style.computedHeight.calc(bounds.height))
		}
	}
}

object VerticalLayout : Layout {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		//println("vertical: relayout ${children.size}")
		//var y = this.actualBounds.y
		var y2 = 0
		//println("vertical: relayout ${children.size}")
		for (child in children) {
			//println("child: $child ${child.width}x${child.height}")
			child.actualBounds.set(0, y2, child.style.computedWidth.calc(bounds.width), child.style.computedHeight.calc(bounds.height))
			y2 += child.actualBounds.height
		}
	}
}

object HorizontalLayout : Layout {
	override fun applyLayout(bounds: IRectangle, parent: Component, children: Iterable<Component>) {
		//println("vertical: relayout ${children.size}")
		val (_, _, width, height) = bounds
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
		parent.actualBounds.width = x2
		parent.actualBounds.height = maxy
	}
}
