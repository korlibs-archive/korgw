package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korma.geom.*

expect val DefaultGameWindow: GameWindow

interface DialogInterface {
    suspend fun browse(url: URL): Unit = unsupported()
    suspend fun alert(message: String): Unit = unsupported()
    suspend fun confirm(message: String): Boolean = unsupported()
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    suspend fun openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> = unsupported()
}

open class GameWindow : EventDispatcher.Mixin(), DialogInterface {
    open val ag: AG = LogAG()

    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()

    open var title: String = ""
    open var size: SizeInt = SizeInt(0, 0)
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open var quality: Quality = Quality.AUTO

    enum class Quality { PERFORMANCE, QUALITY, AUTO }

    override suspend fun browse(url: URL): Unit = unsupported()
    override suspend fun alert(message: String): Unit = unsupported()
    override suspend fun confirm(message: String): Boolean = unsupported()
    override suspend fun prompt(message: String, default: String): String = unsupported()
    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> = unsupported()

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
