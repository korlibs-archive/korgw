package com.soywiz.korui.light

import com.soywiz.korag.AG
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korio.service.Services
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.extraProperty
import com.soywiz.korio.vfs.VfsFile
import java.io.File
import java.net.URI
import java.net.URL

abstract class LightComponentsFactory : Services.Impl() {
	abstract fun create(): LightComponents
}

open class LightComponents {
	class LightComponentInfo(val handle: Any) : Extra by Extra.Mixin()

	open fun create(type: LightType): LightComponentInfo = LightComponentInfo(Unit)
	open fun setParent(c: Any, parent: Any?): Unit = Unit

	@Suppress("UNCHECKED_CAST")
	open protected fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit): Unit = Unit

	var insideEventHandler: Boolean = false; private set

	fun <T : LightEvent> setEventHandler(c: Any, type: Class<T>, handler: (T) -> Unit): Unit {
		setEventHandlerInternal(c, type, {
			insideEventHandler = true
			try {
				handler(it)
			} finally {
				insideEventHandler = false
			}
		})
	}

	open fun getDpi(): Double = 96.0

	inline fun <reified T : LightEvent> setEventHandler(c: Any, noinline handler: (T) -> Unit): Unit = setEventHandler(c, T::class.java, handler)
	open fun <T> setProperty(c: Any, key: LightProperty<T>, value: T): Unit = Unit
	open fun <T> getProperty(c: Any, key: LightProperty<T>): T = key.default
	open fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int): Unit = Unit
	open fun repaint(c: Any): Unit = Unit
	open suspend fun dialogAlert(c: Any, message: String): Unit = Unit
	open suspend fun dialogPrompt(c: Any, message: String): String = throw UnsupportedOperationException()
	open suspend fun dialogOpenFile(c: Any, filter: String): VfsFile = throw UnsupportedOperationException()
	open fun openURL(url: String): Unit = Unit
	open fun open(file: File): Unit = openURL(file.toURI().toString())
}

interface LightEvent
object LightChangeEvent : LightEvent
class LightResizeEvent(var width: Int, var height: Int) : LightEvent

data class LightKeyEvent(
	var type: Type = Type.TYPED,
	var keyCode: Int = 0
) : LightEvent {
	enum class Type {
		TYPED, DOWN, UP
	}
}

data class LightGamepadEvent(
	var type: Type = Type.DOWN,
	var button: Int = 0,
	var buttons: Int = 0,
	val axis: DoubleArray = DoubleArray(16)
) : LightEvent {
	enum class Type {
		DOWN, UP
	}
}

data class LightMouseEvent(
	var type: Type = Type.OVER,
	var x: Int = 0,
	var y: Int = 0,
	var buttons: Int = 0,
	var isShiftDown: Boolean = false,
	var isCtrlDown: Boolean = false,
	var isAltDown: Boolean = false,
	var isMetaDown: Boolean = false
) : LightEvent {
	enum class Type {
		OVER, CLICK, UP, DOWN, ENTER, EXIT
	}
}

data class LightTouchEvent(
	var type: Type = Type.START,
	var x: Int = 0,
	var y: Int = 0,
	var id: Int = 0
) : LightEvent {
	enum class Type {
		START, END, MOVE
	}
}

val defaultLightFactory: LightComponentsFactory by lazy { Services.load<LightComponentsFactory>() }
val defaultLight: LightComponents by lazy { defaultLightFactory.create() }

enum class LightType {
	FRAME, CONTAINER, BUTTON, PROGRESS, IMAGE, LABEL, TEXT_FIELD, TEXT_AREA, CHECK_BOX, SCROLL_PANE, AGCANVAS
}

class LightProperty<out T>(val name: String, val default: T) {
	companion object {
		val VISIBLE = LightProperty<Boolean>("VISIBLE", true)
		val TEXT = LightProperty<String>("TEXT", "")
		val ICON = LightProperty<Bitmap?>("ICON", null)
		val BGCOLOR = LightProperty<Int>("BGCOLOR", Colors.BLACK)
		val PROGRESS_CURRENT = LightProperty<Int>("PROGRESS_CURRENT", 0)
		val PROGRESS_MAX = LightProperty<Int>("PROGRESS_MAX", 100)
		val IMAGE = LightProperty<Bitmap?>("IMAGE", null)
		val IMAGE_SMOOTH = LightProperty<Boolean>("IMAGE_SMOOTH", true)
		val CHECKED = LightProperty<Boolean>("CHECKED", false)
	}

	@Suppress("UNCHECKED_CAST")
	operator fun get(v: Any?): T = v as T

	@Suppress("UNCHECKED_CAST")
	fun getOrDefault(v: Any?): T = if (v == null) default else v as T

	override fun toString(): String = "LightProperty[$name]"
}

var LightComponents.LightComponentInfo.ag: AG? by extraProperty("ag", null)
