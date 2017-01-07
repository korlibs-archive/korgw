package com.soywiz.korui.light

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korio.vfs.VfsFile
import java.util.*

open class LightComponents {
	open fun create(type: LightType): Any = Unit
	open fun setParent(c: Any, parent: Any?): Unit = Unit
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
}

interface LightEvent
class LightResizeEvent(var width: Int, var height: Int) : LightEvent
class LightClickEvent(var x: Int, var y: Int) : LightEvent
class LightOverEvent(var x: Int, var y: Int) : LightEvent

val defaultLight: LightComponents by lazy {
	ServiceLoader.load(LightComponents::class.java).firstOrNull()
		?: throw UnsupportedOperationException("LightComponents not defined")
}

enum class LightType {
	FRAME, CONTAINER, BUTTON, PROGRESS, IMAGE, LABEL, TEXT_FIELD, CHECK_BOX, SCROLL_PANE
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

	operator fun get(v: Any?): T = v as T
	fun getOrDefault(v: Any?): T = if (v == null) default else v as T
	override fun toString(): String = "LightProperty[$name]"
}
