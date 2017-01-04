package com.soywiz.korui.light

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korio.vfs.VfsFile
import java.util.*

open class LightComponents {


	//enum class Property {
	//	VISIBLE, IMAGE, TEXT, VALUE, MAX
	//}

	//open fun destroy(obj: Any): Unit {
	//}

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

	inline fun <reified T : LightEvent> setEventHandler(c: Any, noinline handler: (T) -> Unit): Unit = setEventHandler(c, T::class.java, handler)
	open fun <T> setProperty(c: Any, key: LightProperty<T>, value: T): Unit = Unit
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
	FRAME, CONTAINER, BUTTON, PROGRESS, IMAGE, LABEL
}

class LightProperty<out T>(val default: T) {
	companion object {
		val VISIBLE = LightProperty<Boolean>(true)
		val TEXT = LightProperty<String>("")
		val ICON = LightProperty<Bitmap?>(null)
		val BGCOLOR = LightProperty<Int>(Colors.BLACK)
		val PROGRESS_CURRENT = LightProperty<Int>(0)
		val PROGRESS_MAX = LightProperty<Int>(100)
		val IMAGE = LightProperty<Bitmap?>(null)
		val IMAGE_SMOOTH = LightProperty<Boolean>(true)
	}
}
