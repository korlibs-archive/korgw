package com.soywiz.korui

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.await
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightResizeEvent
import com.soywiz.korui.light.defaultLight
import com.soywiz.korui.ui.Frame

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
}

suspend fun Application.frame(title: String, width: Int = 640, height: Int = 480, icon: Bitmap? = null, callback: suspend Frame.() -> Unit = {}): Frame = asyncFun {
	val frame = Frame(this.light, title).apply {
		actualBounds.width = width
		actualBounds.height = height
	}
	frame.icon = icon
	callback.await(frame)
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	light.setEventHandler<LightResizeEvent>(frame.handle) { e ->
		frame.actualBounds.width = e.width
		frame.actualBounds.height = e.height
		frame.invalidate()
	}
	frames += frame
	frame.visible = true
	frame.invalidate()

	frame
}
