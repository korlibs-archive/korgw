package com.soywiz.korui

import com.soywiz.korio.async.EventLoop
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
