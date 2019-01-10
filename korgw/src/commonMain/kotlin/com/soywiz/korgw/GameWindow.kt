package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

expect val DefaultGameWindow: GameWindow

open class GameWindow : EventDispatcher.Mixin() {
    open val ag: AG = LogAG()

    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()

    open var title: String = ""
    open var size: SizeInt = SizeInt(0, 0)
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false

    open suspend fun loop(entry: suspend GameWindow.() -> Unit) = Unit
}

fun GameWindow.toggleFullScreen() = run { fullscreen = !fullscreen }

fun GameWindow.configure(
    width: Int,
    height: Int,
    title: String? = "GameWindow",
    icon: Bitmap? = null,
    fullscreen: Boolean? = null
) {
    this.size = SizeInt(width, height)
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.visible = true
}
