package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

expect val DefaultGameWindow: GameWindow

interface DialogInterface {
    suspend fun browse(url: URL): Unit = unsupported()
    suspend fun alert(message: String): Unit = unsupported()
    suspend fun confirm(message: String): Boolean = unsupported()
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    suspend fun openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> =
        unsupported()
}

@UseExperimental(InternalCoroutinesApi::class)
open class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay {
    data class TimedTask(val time: DateTime, val task: () -> Unit) : Comparable<TimedTask> {
        override fun compareTo(other: TimedTask): Int = this.time.compareTo(other.time)
    }

    private val tasks = Queue<Runnable>()
    private val timedTasks = PriorityQueue<TimedTask>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val timedTask = TimedTask(DateTime.now() + timeMillis.milliseconds) {
            continuation.resume(Unit)
        }
        timedTasks += timedTask
        continuation.invokeOnCancellation {
            timedTasks -= timedTask
        }
    }

    fun executePending() {
        while (tasks.isNotEmpty()) {
            val task = tasks.dequeue()
            task.run()
        }
    }
}

open class GameWindow : EventDispatcher.Mixin(), DialogInterface {
    open val ag: AG = LogAG()

    open val coroutineDispatcher: GameWindowCoroutineDispatcher = GameWindowCoroutineDispatcher()

    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()

    open var fps: Int = 60
    open var title: String = ""
    open val width: Int = 0
    open val height: Int = 0
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open var quality: Quality = Quality.AUTO

    val timePerFrame: TimeSpan get() = (1000.0 / fps).milliseconds

    enum class Quality { PERFORMANCE, QUALITY, AUTO }

    open fun setSize(width: Int, height: Int) {
    }

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
    this.setSize(width, height)
    if (title != null) this.title = title
    this.icon = icon
    if (fullscreen != null) this.fullscreen = fullscreen
    this.visible = true
}
