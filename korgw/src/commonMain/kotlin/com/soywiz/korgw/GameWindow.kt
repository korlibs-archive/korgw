package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

expect val DefaultGameWindow: GameWindow

open class GameWindow : EventDispatcher by EventDispatcher.Mixin() {
    open var title: String = ""
    open var size: Size = Size(0, 0)
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open suspend fun loop(entry: suspend () -> Unit) = Unit
}

fun GameWindow.configure(
    width: Int,
    height: Int,
    title: String = "GameWindow",
    icon: Bitmap? = null,
    fullscreen: Boolean = false
) {
    this.size = Size(width, height)
    this.title = title
    this.icon = icon
    this.fullscreen = fullscreen
    this.visible = true
}
