package com.soywiz.korui

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.spawn
import com.soywiz.korio.vfs.VfsFile

interface Length {
	object AUTO : Length {
		override fun calc(size: Int): Int = size
		override fun scale(ratio: Double): Length = AUTO
		override fun toString() = "auto"
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
}

val Int.px: Length get() = Length.PX(this)
val Int.pt: Length get() = Length.PT(this)
val Int.percent: Length get() = Length.Ratio(this.toDouble() / 100.0)
val Double.ratio: Length get() = Length.Ratio(this)

open class Component(val lc: LightComponents, val handle: Any) {
	var top: Length = 0.percent
	var left: Length = 0.percent
	var width: Length = 100.percent
	var height: Length = 100.percent
	var valid = false
	var actualBounds: Rectangle = Rectangle()

	var parent: Container? = null
		set(newParent) {
			if (newParent != null) {
				newParent.children -= this
			}
			field = newParent
			newParent?.children?.add(this)
			lc.setParent(handle, newParent?.handle)
			newParent?.invalidate()
		}

	val children = arrayListOf<Component>()

	fun relayout() {
		if (valid) return
		//println("$this: relayout")
		valid = true
		relayoutInternal()
		setBoundsInternal(actualBounds)
		for (child in children) {
			child.relayout()
		}
	}

	open protected fun relayoutInternal() {
		//println("relayoutInternal: $this")
	}

	fun invalidate() {
		valid = false
		parent?.invalidate()
		invalidateDescendants()
	}

	fun invalidateDescendants() {
		valid = false
		for (child in children) child.invalidateDescendants()
	}

	open fun setBoundsInternal(bounds: Rectangle) {
		//println("$this($parent): $bounds")
		lc.setBounds(handle, bounds.x, bounds.y, bounds.width, bounds.height)
	}

	fun onClick(handler: suspend () -> Unit) {
		lc.setEventHandler<LightClickEvent>(handle) {
			spawn(handler)
		}
	}

	var visible: Boolean = false
		set(value) {
			field = value
			lc.setVisible(handle, value)
		}

	override fun toString(): String = javaClass.simpleName
}

open class Container(lc: LightComponents, type: String) : Component(lc, lc.create(type)) {
	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}
}

class Frame(lc: LightComponents, title: String) : Container(lc, LightComponents.TYPE_FRAME) {
	var title: String = ""
		get() = field
		set(value) {
			lc.setText(handle, value)
		}

	init {
		this.title = title
	}

	open suspend fun dialogOpenFile(filter: String = ""): VfsFile = asyncFun {
		lc.dialogOpenFile(handle, filter)
	}

	open suspend fun prompt(message: String): String = asyncFun {
		lc.dialogPrompt(handle, message)
	}

	open suspend fun alert(message: String): Unit = asyncFun {
		lc.dialogAlert(handle, message)
	}

	// @TODO: @FIXME: JTransc 0.5.3 treeshaking doesn't include this!
	override fun relayoutInternal() {
		for (child in children) {
			child.actualBounds.set(0, 0, child.width.calc(actualBounds.width), child.height.calc(actualBounds.height))
		}
	}

	override fun setBoundsInternal(bounds: Rectangle) {
	}
}

open class Layout(lc: LightComponents) : Container(lc, LightComponents.TYPE_CONTAINER) {
	val padding = Padding()

	init {
		width = 100.percent
		height = 100.percent
	}
}

open class VerticalLayout(lc: LightComponents) : Layout(lc) {
	@JTranscKeep
	override fun relayoutInternal() {
		//println("vertical: relayout ${children.size}")
		val (_, _, width, height) = actualBounds
		//var y = this.actualBounds.y
		var y2 = 0
		//println("vertical: relayout ${children.size}")
		for (child in children) {
			//println("child: $child ${child.width}x${child.height}")
			child.actualBounds.set(0, y2, child.width.calc(width), child.height.calc(height))
			y2 += child.actualBounds.height
		}
	}
}

data class Padding(var left: Length = 0.px, var top: Length = 0.px, var right: Length = 0.px, var bottom: Length = 0.px) {
}

data class Rectangle(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) {
	fun set(that: Rectangle) = set(that.x, that.y, that.width, that.height)

	fun set(x: Int, y: Int, width: Int, height: Int) {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}
}

class Button(lc: LightComponents, text: String) : Component(lc, lc.create(LightComponents.TYPE_BUTTON)) {
	var text: String = ""
		get() = field
		set(value) {
			field = value
			lc.setText(handle, value)
		}
	init {
		width = 100.percent
		height = 32.pt
		this.text = text
	}
}

class Spacer(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_CONTAINER)) {
	init {
		width = 100.percent
		height = 32.pt
	}
}

class Image(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_IMAGE)) {
	var image: Bitmap? = null
		set(newImage) {
			if (newImage != null) {
				width = newImage.width.px
				height = newImage.height.px
			}
			lc.setImage(handle, newImage)
			invalidate()
		}
}

class Application(val light: LightComponents = defaultLight) {
	val frames = arrayListOf<Frame>()

	init {
		EventLoop.setInterval(16) {
			//println("interval!")
			for (frame in frames.filter { !it.valid }) {
				frame.relayout()
				light.repaint(frame.handle)
			}
		}
	}

	companion object {

	}
}

fun Application.frame(title: String, width: Int = 640, height: Int = 480, callback: Frame.() -> Unit = {}): Frame {
	val frame = Frame(this.light, title).apply {
		actualBounds.width = width
		actualBounds.height = height
		callback()
	}
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	light.setEventHandler<LightResizeEvent>(frame.handle) { e ->
		frame.actualBounds.width = e.width
		frame.actualBounds.height = e.height
		frame.invalidate()
	}
	frames += frame
	frame.visible = true
	frame.invalidate()
	return frame
}

//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.width = width; this.height = height }

fun Container.button(text: String) = add(Button(this.lc, text))
inline fun Container.button(text: String, clickHandler: suspend () -> Unit) = add(Button(this.lc, text).apply { onClick(clickHandler) })
inline fun Container.image(bitmap: Bitmap, callback: Image.() -> Unit) = add(Image(this.lc).apply { image = bitmap; callback() })
inline fun Container.image(bitmap: Bitmap) = add(Image(this.lc).apply { image = bitmap })
inline fun Container.spacer() = add(Spacer(this.lc))

inline fun Container.vertical(callback: VerticalLayout.() -> Unit): VerticalLayout = add(VerticalLayout(this.lc).apply { callback() })
