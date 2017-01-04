package com.soywiz.korui.light.android

import android.app.Activity
import android.os.Bundle
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Signal
import com.soywiz.korio.util.Once

object KuroiApp {
	val initOnce = Once()
	val resized = Signal<Unit>()
}

open class KoruiActivity : Activity() {
	lateinit var rootLayout: RootKoruiAbsoluteLayout

	override final fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		com.soywiz.korio.android.KorioAndroidInit(this)
		KuroiApp.initOnce {
			EventLoop.main {
				println()
				main(arrayOf())
			}
		}
	}

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		KuroiApp.resized(Unit)
	}

	/*
	override fun onConfigurationChanged(newConfig: Configuration) {
		//println(newConfig.orientation)
		resized(Unit)
	}
	*/

	suspend protected open fun main(args: Array<String>) {
	}
}