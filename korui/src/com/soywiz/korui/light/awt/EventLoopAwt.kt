package com.soywiz.korui.light.awt

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.EventLoopFactory
import java.io.Closeable
import javax.swing.SwingUtilities
import javax.swing.Timer

class EventLoopFactoryAwt : EventLoopFactory() {
	override val available: Boolean = true
	override val priority: Int = 500

	override fun createEventLoop(): EventLoop = EventLoopAwt()
}

class EventLoopAwt : EventLoop() {
	override fun setImmediateInternal(handler: () -> Unit) {
		if (SwingUtilities.isEventDispatchThread()) {
			handler()
		} else {
			SwingUtilities.invokeLater { handler() }
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
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

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		val timer = Timer(ms, {
			if (SwingUtilities.isEventDispatchThread()) {
				callback()
			} else {
				SwingUtilities.invokeLater { callback() }
			}
		})
		timer.isRepeats = true
		timer.start()
		return Closeable { timer.stop() }
	}
}