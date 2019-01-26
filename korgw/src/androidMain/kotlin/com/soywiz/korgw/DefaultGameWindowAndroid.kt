package com.soywiz.korgw

import com.soywiz.korag.AG
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL

actual val DefaultGameWindow: GameWindow = NodeJsGameWindow()

class NodeJsGameWindow : GameWindow() {
    override val ag: AG
        get() = super.ag
    override val coroutineDispatcher: GameWindowCoroutineDispatcher
        get() = super.coroutineDispatcher
    override var fps: Int
        get() = super.fps
        set(value) {}
    override var title: String
        get() = super.title
        set(value) {}
    override val width: Int
        get() = super.width
    override val height: Int
        get() = super.height
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = super.fullscreen
        set(value) {}
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    override fun setSize(width: Int, height: Int) {
        super.setSize(width, height)
    }

    override suspend fun browse(url: URL) {
        super.browse(url)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        return super.openFileDialog(filter, write, multi)
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        super.loop(entry)
    }
}

