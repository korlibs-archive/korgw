package com.soywiz.korui.ui

import com.soywiz.korim.bitmap.Bitmap
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
import com.soywiz.korui.light.LightProperty
import com.soywiz.korui.light.LightType
import com.soywiz.korui.style.Style
import com.soywiz.korui.style.Styled
import kotlin.reflect.KProperty

open class Component(val lc: LightComponents, val type: LightType) : Styled {
	class lightProperty<T>(val key: LightProperty<T>, val getable: Boolean = false, val setHandler: ((v: T) -> Unit)? = null) {
		inline operator fun getValue(thisRef: Component, property: KProperty<*>): T {
			if (getable) return thisRef.lc.getProperty(thisRef.handle, key)
			return thisRef.getProperty(key)
		}

		inline operator fun setValue(thisRef: Component, property: KProperty<*>, value: T): Unit = run {
			thisRef.setProperty(key, value)
			setHandler?.invoke(value)
		}
	}

	override var style = Style()
	var handle = lc.create(type)
	val properties = LinkedHashMap<LightProperty<*>, Any?>()
	var valid = false
	protected var nativeBounds = IRectangle()
	val actualBounds: IRectangle = IRectangle()

	fun <T> setProperty(key: LightProperty<T>, value: T, reset: Boolean = false) {
		if (reset || (properties[key] != value)) {
			properties[key] = value
			lc.setProperty(handle, key, value)
		}
	}

	fun <T> getProperty(key: LightProperty<T>): T = if (key in properties) properties[key] as T else key.default

	fun setBoundsInternal(bounds: IRectangle) = setBoundsInternal(bounds.x, bounds.y, bounds.width, bounds.height)

	fun setBoundsInternal(x: Int, y: Int, width: Int, height: Int): IRectangle {
		nativeBounds.setTo(x, y, width, height)
		actualBounds.setTo(x, y, width, height)
		lc.setBounds(handle, x, y, width, height)
		//invalidateAncestors()
		return actualBounds
	}

	open fun recreate() {
		handle = lc.create(type)
		lc.setBounds(handle, nativeBounds.x, nativeBounds.y, nativeBounds.width, nativeBounds.height)
		for ((key, value) in properties) {
			lc.setProperty(handle, key, value)
		}
		lc.setParent(handle, parent?.handle)
	}

	var parent: Container? = null
		set(newParent) {
			if (newParent != null) {
				newParent.children -= this
			}
			field = newParent
			newParent?.children?.add(this)
			lc.setParent(handle, newParent?.handle)
			//invalidate()
			newParent?.invalidate()
		}

	fun invalidate() {
		//println("------invalidate")
		invalidateAncestors()
		invalidateDescendants()
	}

	open fun invalidateDescendants() {
		//println("------invalidateDescendants")
		valid = false
	}

	fun invalidateAncestors() {
		//println("------invalidateAncestors")
		if (!valid) return
		valid = false
		parent?.invalidateAncestors()
	}

	open fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): IRectangle {
		if (valid) return actualBounds
		valid = true
		return setBoundsInternal(x, y, width, height)
	}

	fun setBoundsAndRelayout(rect: IRectangle) = setBoundsAndRelayout(rect.x, rect.y, rect.width, rect.height)

	//fun onClick(handler: (LightClickEvent) -> Unit) {
	//	lc.setEventHandler<LightClickEvent>(handle, handler)
	//}

	var mouseX = 0
	var mouseY = 0

	fun _onClickHandler(handler: suspend Component.() -> Unit) {
		lc.setEventHandler<LightClickEvent>(handle) { e ->
			mouseX = e.x
			mouseY = e.y
			spawnAndForget {
				handler.await(this)
			}
		}
	}

	var visible by lightProperty(LightProperty.VISIBLE)

	override fun toString(): String = javaClass.simpleName
}

open class Container(lc: LightComponents, var layout: Layout, type: LightType = LightType.CONTAINER) : Component(lc, type) {
	val children = arrayListOf<Component>()

	override fun recreate() {
		super.recreate()
		for (child in children) child.recreate()
	}

	override fun invalidateDescendants() {
		super.invalidateDescendants()
		for (child in children) child.invalidateDescendants()
	}

	override fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): IRectangle {
		//println("relayout:$valid")
		if (valid) return actualBounds
		//println("$this: relayout")
		valid = true
		return setBoundsInternal(layout.applyLayout(this, children, x, y, width, height, out = actualBounds))
	}

	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}

}

class Frame(lc: LightComponents, title: String) : Container(lc, LayeredLayout, LightType.FRAME) {
	var title by lightProperty(LightProperty.TEXT)
	var icon by lightProperty(LightProperty.ICON)
	var bgcolor by lightProperty(LightProperty.BGCOLOR)

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
}

class Button(lc: LightComponents, text: String) : Component(lc, LightType.BUTTON) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}
}

class Label(lc: LightComponents, text: String) : Component(lc, LightType.LABEL) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}
}

class TextField(lc: LightComponents, text: String) : Component(lc, LightType.TEXT_FIELD) {
	var text by lightProperty(LightProperty.TEXT, getable = true)

	init {
		this.text = text
	}
}

class CheckBox(lc: LightComponents, text: String, initialChecked: Boolean) : Component(lc, LightType.CHECK_BOX) {
	var text by lightProperty(LightProperty.TEXT)
	var checked by lightProperty(LightProperty.CHECKED, getable = true)

	init {
		this.text = text
		this.checked = initialChecked
	}
}

class Progress(lc: LightComponents, current: Int = 0, max: Int = 100) : Component(lc, LightType.PROGRESS) {
	var current by lightProperty(LightProperty.PROGRESS_CURRENT)
	var max by lightProperty(LightProperty.PROGRESS_MAX)

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		set(current, max)
	}
}

class Spacer(lc: LightComponents) : Component(lc, LightType.CONTAINER) {
}

class Image(lc: LightComponents) : Component(lc, LightType.IMAGE) {
	var image by lightProperty(LightProperty.IMAGE) {
		if (it != null) {
			if (this.style.defaultSize.width != it.width.pt || this.style.defaultSize.height != it.height.pt) {
				this.style.defaultSize.setTo(it.width.pt, it.height.pt)
				invalidate()
			}
		}
	}

	var smooth by lightProperty(LightProperty.IMAGE_SMOOTH)

	fun refreshImage() {
		setProperty(LightProperty.IMAGE, image, reset = true)
	}
}

//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.style.size.setTo(width, height) }

suspend fun Container.button(text: String) = add(Button(this.lc, text))
suspend inline fun Container.button(text: String, callback: suspend Button.() -> Unit) = asyncFun {
	add(Button(this.lc, text).apply {
		callback.await(this@apply)
	})
}

suspend inline fun Container.progress(current: Int, max: Int) = add(Progress(this.lc, current, max))
suspend inline fun Container.image(bitmap: Bitmap, callback: suspend Image.() -> Unit) = asyncFun { add(Image(this.lc).apply { image = bitmap; callback.await(this) }) }
suspend inline fun Container.image(bitmap: Bitmap) = add(Image(this.lc).apply {
	image = bitmap
	this.style.defaultSize.width = bitmap.width.pt
	this.style.defaultSize.height = bitmap.height.pt
})

suspend inline fun Container.spacer() = add(Spacer(this.lc))

suspend inline fun Container.label(text: String, callback: suspend Label.() -> Unit = {}) = asyncFun { add(Label(this.lc, text).apply { callback.await(this) }) }

suspend inline fun Container.checkBox(text: String, checked: Boolean = false, callback: suspend CheckBox.() -> Unit = {}) = asyncFun { add(CheckBox(this.lc, text, checked).apply { callback.await(this) }) }

suspend inline fun Container.textField(text: String = "", callback: suspend TextField.() -> Unit = {}) = asyncFun { add(TextField(this.lc, text).apply { callback.await(this) }) }

suspend inline fun Container.layers(callback: suspend Container.() -> Unit): Container = asyncFun { add(Container(this.lc, LayeredLayout).apply { callback.await(this) }) }
suspend inline fun Container.layersKeepAspectRatio(anchor: Anchor = Anchor.MIDDLE_CENTER, scaleMode: ScaleMode = ScaleMode.SHOW_ALL, callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, LayeredKeepAspectLayout(anchor, scaleMode)).apply { callback.await(this) })
}

suspend inline fun Container.vertical(callback: suspend Container.() -> Unit): Container = asyncFun { add(Container(this.lc, VerticalLayout).apply { callback.await(this) }) }
suspend inline fun Container.horizontal(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, HorizontalLayout).apply {
		callback.await(this)
	})
}

suspend inline fun Container.inline(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, InlineLayout).apply {
		callback.await(this)
	})
}

suspend inline fun Container.relative(callback: suspend Container.() -> Unit): Container = asyncFun {
	add(Container(this.lc, RelativeLayout).apply {
		callback.await(this)
	})
}

fun <T : Component> T.click(handler: suspend Component.() -> Unit) = this.apply { _onClickHandler(handler) }
