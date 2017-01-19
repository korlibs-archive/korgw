package com.soywiz.korui.light.awt

import com.soywiz.korio.async.EventLoop
import java.io.Closeable
import javax.swing.SwingUtilities
import javax.swing.Timer

class EventLoopAwt : EventLoop() {
	override val available: Boolean = true
	override val priority: Int = 500

	override fun init() {
	}

	override fun setImmediate(handler: () -> Unit) {
		if (SwingUtilities.isEventDispatchThread()) {
			handler()
		} else {
			SwingUtilities.invokeLater { handler() }
		}
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val timer = Timer(ms, {
			if (SwingUtilities.isEventDispatchThread()) {
				callback()
			} else {
				SwingUtilities.invokeLater { callback() }
			}
		})
		timer.isRepeats = false
		timer.start()
		return Closeable { timer.stop() }
	}
}