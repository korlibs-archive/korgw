package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*

class IosGameWindow : GameWindow() {
    override val ag: AG = AGNative(gles = true)

    //override var fps: Int get() = 60; set(value) = Unit
    //override var title: String get() = ""; set(value) = Unit
    //override val width: Int get() = 512
    //override val height: Int get() = 512
    //override var icon: Bitmap? get() = null; set(value) = Unit
    //override var fullscreen: Boolean get() = false; set(value) = Unit
    //override var visible: Boolean get() = false; set(value) = Unit
    //override var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        entry(this)
    }

    override fun frame() {
        coroutineDispatcher.executePending()
        ag.onRender(ag)
        dispatch(renderEvent)
    }

    companion object {
        fun getGameWindow() = MyIosGameWindow
    }
}

var MyIosGameWindow = IosGameWindow()

actual val DefaultGameWindow: GameWindow get() = MyIosGameWindow
