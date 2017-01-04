package com.soywiz.korui.ui

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.IRectangle
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.await
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.light.LightClickEvent
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.style.Style
import com.soywiz.korui.style.Styled
import com.soywiz.korui.style.height
import com.soywiz.korui.style.width

open class Component(val lc: LightComponents, val handle: Any) : Styled {
	override var style = Style()

	var valid = false
	val actualBounds: IRectangle = IRectangle()

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
		for (child in children) child.relayout()
		setBoundsInternal(actualBounds)
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

	open fun setBoundsInternal(bounds: IRectangle) {
		//println("$this($parent): $bounds")
		lc.setBounds(handle, bounds.x, bounds.y, bounds.width, bounds.height)
	}

	//fun onClick(handler: (LightClickEvent) -> Unit) {
	//	lc.setEventHandler<LightClickEvent>(handle, handler)
	//}

	var mouseX = 0
	var mouseY = 0

	fun onClick(handler: suspend Component.() -> Unit) {
		lc.setEventHandler<LightClickEvent>(handle) { e ->
			mouseX = e.x
			mouseY = e.y
			spawnAndForget {
				handler.await(this)
			}
		}
	}

	var visible: Boolean = false
		set(value) {
			field = value
			lc.setVisible(handle, value)
		}

	override fun toString(): String = javaClass.simpleName
}

open class Container(lc: LightComponents, var layout: Layout, type: LightComponents.Type = LightComponents.Type.CONTAINER) : Component(lc, lc.create(type)) {
	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}

	// @TODO: @FIXME: JTransc 0.5.3 treeshaking doesn't include this!
	override final fun relayoutInternal() {
		layout.applyLayout(actualBounds, this, children)
	}
}

class Frame(lc: LightComponents, title: String) : Container(lc, LayeredLayout, LightComponents.Type.FRAME) {
	var title: String = ""
		set(value) {
			field = value
			lc.setText(handle, value)
		}

	var icon: Bitmap? = null
		set(value) {
			field = value
			lc.setAttributeBitmap(handle, "icon", value)
		}

	var bgcolor: Int = RGBA(0xf0, 0xf0, 0xf0, 0xff)
		set(value) {
			field = value
			lc.setAttributeInt(handle, "background", value)
		}

	init {
		this.title = title
	}

	open suspend fun dialogOpenFile(filter: String = ""): VfsFile = asyncFun {
		if (!lc.insideEventHandler) throw IllegalStateException("Can't open file dialog outside an event")
		lc.dialogOpenFile(handle, filter)
	}

	open suspend fun prompt(message: String): String = asyncFun {
		lc.dialogPrompt(handle, message)
	}

	open suspend fun alert(message: String): Unit = asyncFun {
		lc.dialogAlert(handle, message)
	}

	open fun openURL(url: String): Unit {
		lc.openURL(url)
	}

	override fun setBoundsInternal(bounds: IRectangle) {
	}
}

class Button(lc: LightComponents, text: String) : Component(lc, lc.create(LightComponents.Type.BUTTON)) {
	var text: String = ""
		get() = field
		set(value) {
			field = value
			lc.setText(handle, value)
		}

	init {
		this.text = text
	}
}

class Progress(lc: LightComponents, current: Int = 0, max: Int = 100) : Component(lc, lc.create(LightComponents.Type.PROGRESS)) {
	var current: Int = 0
		get() = field
		set(value) {
			field = value
			lc.setAttributeInt(handle, "current", value)
		}

	var max: Int = 0
		get() = field
		set(value) {
			field = value
			lc.setAttributeInt(handle, "max", value)
		}

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		set(current, max)
	}
}

class Spacer(lc: LightComponents) : Component(lc, lc.create(LightComponents.Type.CONTAINER)) {
}

class Image(lc: LightComponents) : Component(lc, lc.create(LightComponents.Type.IMAGE)) {
	var image: Bitmap? = null
		set(newImage) {
			if (newImage != null) {
				style.size.setTo(newImage.width.pt, newImage.height.pt)
			}
			lc.setImage(handle, newImage)
			invalidate()
		}

	var smooth: Boolean = true
		set(value) {
			field = value
			lc.setAttributeBoolean(handle, "smooth", value)
		}

	init {
		smooth = true
	}

	fun refreshImage() {
		this.image = image
		//lc.setImage(handle, image)
		//invalidate()
	}
}

//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.style.size.setTo(width, height) }

suspend fun Container.button(text: String) = add(Button(this.lc, text))
suspend inline fun Container.button(text: String, clickHandler: suspend Button.() -> Unit) = add(Button(this.lc, text).apply {
	onClick {
		clickHandler.await(this@apply)
	}
})

suspend inline fun Container.progress(current: Int, max: Int) = add(Progress(this.lc, current, max))
suspend inline fun Container.image(bitmap: Bitmap, callback: suspend Image.() -> Unit) = asyncFun { add(Image(this.lc).apply { image = bitmap; callback.await(this) }) }
suspend inline fun Container.image(bitmap: Bitmap) = add(Image(this.lc).apply { image = bitmap })
suspend inline fun Container.spacer() = add(Spacer(this.lc))

suspend inline fun Container.layers(callback: suspend Container.() -> Unit): Container = asyncFun { add(Container(this.lc, LayeredLayout).apply { callback.await(this) }) }
suspend inline fun Container.layersKeepAspectRatio(anchor: Anchor = Anchor.MIDDLE_CENTER, scaleMode: ScaleMode = ScaleMode.SHOW_ALL, callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, LayeredKeepAspectLayout(anchor, scaleMode)).apply { callback.await(this) })
}
suspend inline fun Container.vertical(callback: suspend Container.() -> Unit): Container = asyncFun { add(Container(this.lc, VerticalLayout).apply { callback.await(this) }) }
suspend inline fun Container.horizontal(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, HorizontalLayout).apply {
		style.height = 32.pt
		callback.await(this)
	})
}

suspend inline fun Container.inline(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, InlineLayout).apply {
		style.height = 32.pt
		callback.await(this)
	})
}

suspend inline fun Container.relative(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, RelativeLayout).apply {
		callback.await(this)
	})
}
