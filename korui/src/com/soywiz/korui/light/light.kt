package com.soywiz.korui.light

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.light.awt.AwtLightComponents

open class LightComponents {
	companion object {
		// Types
		val TYPE_FRAME = "frame"
		val TYPE_CONTAINER = "container"
		val TYPE_BUTTON = "button"
		val TYPE_PROGRESS = "progress"
		val TYPE_IMAGE = "image"
		val TYPE_LABEL = "label"
		// Events
		val EVENT_CLICK = "click"
		val EVENT_RESIZED = "resized" // For frames only
	}

	//open fun destroy(obj: Any): Unit {
	//}

	open fun create(type: String): Any = throw UnsupportedOperationException()
	open fun setParent(c: Any, parent: Any?): Unit = throw UnsupportedOperationException()
	open protected fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit): Unit = throw UnsupportedOperationException()

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
	open fun setText(c: Any, text: String): Unit = throw UnsupportedOperationException()
	open fun setAttributeString(c: Any, key: String, value: String): Unit = Unit
	open fun setAttributeInt(c: Any, key: String, value: Int): Unit = Unit
	open fun setAttributeDouble(c: Any, key: String, value: Int): Unit = Unit
	open fun setAttributeBitmap(handle: Any, key: String, value: Bitmap?) = Unit
	open fun setImage(c: Any, bmp: Bitmap?): Unit = Unit
	open fun setVisible(c: Any, visible: Boolean): Unit = Unit
	open fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int): Unit = Unit
	open fun repaint(c: Any): Unit = Unit
	open suspend fun dialogAlert(c: Any, message: String): Unit = Unit
	open suspend fun dialogPrompt(c: Any, message: String): String = throw UnsupportedOperationException()
	open suspend fun dialogOpenFile(c: Any, filter: String): VfsFile = throw UnsupportedOperationException()
}

interface LightEvent
class LightResizeEvent(var width: Int, var height: Int) : LightEvent
class LightClickEvent(var x: Int, var y: Int) : LightEvent
class LightOverEvent(var x: Int, var y: Int) : LightEvent

val defaultLight: LightComponents by lazy { _LightComponents() }

@JTranscMethodBody(target = "js", value = "return {% CONSTRUCTOR com.soywiz.korui.light.html.HtmlLightComponents:()V %}();")
fun _LightComponents(): LightComponents = AwtLightComponents()
