package com.soywiz.korui

import com.soywiz.korag.AGContainer
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.await
import com.soywiz.korio.async.eventLoop
import com.soywiz.korio.async.spawn
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightResizeHandler
import com.soywiz.korui.light.defaultLight
import com.soywiz.korui.ui.Frame
import com.soywiz.korui.ui.agCanvas

suspend fun Application(light: LightComponents = defaultLight) = withCoroutineContext {
	Application(this@withCoroutineContext, light)
}

class Application(val coroutineContext: CoroutineContext, val light: LightComponents = defaultLight) {
	val frames = arrayListOf<Frame>()
	val lengthContext = Length.Context().apply {
		pixelsPerInch = light.getDpi()
	}

	init {
		//println("pixelsPerInch:${lengthContext.pixelsPerInch}")
		spawn(coroutineContext) {
			while (true) {
				coroutineContext.eventLoop.sleep(16)
				for (frame in frames.filter { !it.valid }) {
					if (!frame.valid) {
						//println("!!!!!!!!!!relayout")
						//Toolkit.getDefaultToolkit().setDynamicLayout(false);
						//println(SwingUtilities.isEventDispatchThread())
						frame.setBoundsAndRelayout(frame.actualBounds)
						light.repaint(frame.handle)
					}
				}
			}
		}
	}
}

suspend fun Application.frame(title: String, width: Int = 640, height: Int = 480, icon: Bitmap? = null, callback: suspend Frame.() -> Unit = {}): Frame {
	val frame = Frame(this, title).apply {
		setBoundsInternal(0, 0, width, height)
	}
	frame.icon = icon
	callback.await(frame)
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	light.addHandler(frame.handle, object : LightResizeHandler() {
		override fun resized(e: Info) {
			frame.setBoundsInternal(0, 0, e.width, e.height)
			frame.invalidate()
		}
	})
	frames += frame
	frame.visible = true
	frame.invalidate()
	return frame
}


suspend fun CanvasApplication(title: String, width: Int = 640, height: Int = 480, icon: Bitmap? = null, light: LightComponents = defaultLight, callback: suspend (AGContainer) -> Unit = {}): Unit = withCoroutineContext {
	//if (agFactory.supportsNativeFrame) {
	//	val win = agFactory.createFastWindow(title, width, height)
	//	callback(win)
	//} else {
	Application(this@withCoroutineContext, light).frame(title, width, height, icon) {
		callback(agCanvas().apply { focus() })
	}
	//}
	Unit
}