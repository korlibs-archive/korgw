package com.soywiz.korui

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.*
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightResizeEvent
import com.soywiz.korui.light.defaultLight
import com.soywiz.korui.ui.Frame

class Application(val light: LightComponents = defaultLight) {
	val frames = arrayListOf<Frame>()

	init {
		spawn {
			while (true) {
				//println("step")
				sleep(16)
				for (frame in frames.filter { !it.valid }) {
					if (!frame.valid) {
						//println("!!!!!!!!!!relayout")
						frame.setBoundsAndRelayout(frame.actualBounds)
						light.repaint(frame.handle)
					}
				}
			}
		}
	}
}

suspend fun Application.frame(title: String, width: Int = 640, height: Int = 480, icon: Bitmap? = null, callback: suspend Frame.() -> Unit = {}): Frame = asyncFun {
	val frame = Frame(this.light, title).apply {
		setBoundsInternal(0, 0, width, height)
	}
	frame.icon = icon
	callback.await(frame)
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	light.setEventHandler<LightResizeEvent>(frame.handle) { e ->
		frame.setBoundsInternal(0, 0, e.width, e.height)
		frame.invalidate()
	}
	frames += frame
	frame.visible = true
	frame.invalidate()
	frame
}
