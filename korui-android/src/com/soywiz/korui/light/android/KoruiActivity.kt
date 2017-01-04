package com.soywiz.korui.light.android

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Signal

open class KoruiActivity : Activity() {
	override final fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		com.soywiz.korio.android.KorioAndroidInit(this)
		EventLoop.main {
			main(arrayOf())
		}
	}

	val rotated = Signal<Unit>()

	override fun onConfigurationChanged(newConfig: Configuration) {
		println(newConfig.orientation)
		rotated(Unit)
	}

	suspend protected open fun main(args: Array<String>) {
	}
}