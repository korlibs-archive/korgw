package com.soywiz.korgw

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import com.soywiz.korio.android.androidContext
import com.soywiz.korag.AG
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.soywiz.korio.util.redirected
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

actual fun CreateDefaultGameWindow(): GameWindow = TODO()

class AndroidGameWindow() : GameWindow() {
    lateinit var activity: KorgwActivity

    override val ag: AG get() = activity.ag
    override var fps: Int by { activity::fps }.redirected()
    override var title: String; get() = activity.title.toString(); set(value) = run { activity.title = value }
    override val width: Int get() = activity.window.decorView.width
    override val height: Int get() = activity.window.decorView.height
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean = false
        set(value) {
            field = value
            activity.window.decorView.apply {
                if (value) {
                    systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                } else {
                    systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }
            }
        }
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    override fun setSize(width: Int, height: Int) {
    }

    override suspend fun browse(url: URL) {
        super.browse(url)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("message")
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, which -> deferred.complete(which == DialogInterface.BUTTON_POSITIVE) }
            .setNegativeButton("No", null)
            .show()
        return deferred.await()
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        return super.openFileDialog(filter, write, multi)
    }

    lateinit var coroutineContext: CoroutineContext
    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        activity = (androidContext() as KorgwActivity)
        activity.gameWindow = this
        entry(this)
    }
}

