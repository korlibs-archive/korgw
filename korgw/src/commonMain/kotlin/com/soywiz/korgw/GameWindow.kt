package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
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
class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val ms: DateTime, val continuation: CancellableContinuation<Unit>)

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask> { a, b -> a.ms.compareTo(b.ms) }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(DateTime.now() + timeMillis.milliseconds, continuation)
        continuation.invokeOnCancellation {
            timedTasks.remove(task)
        }
        timedTasks.add(task)
    }

    fun executePending() {
        val now = DateTime.now()
        while (timedTasks.isNotEmpty() && now >= timedTasks.head.ms) {
            timedTasks.removeHead().continuation.resume(Unit)
        }

        while (tasks.isNotEmpty()) {
            val task = tasks.dequeue()
            task.run()
        }
    }

    override fun close() {

    }

    override fun toString(): String = "MyNativeCoroutineDispatcher"
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
    open var title: String get() = ""; set(value) = Unit
    open val width: Int = 0
    open val height: Int = 0
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    val timePerFrame: TimeSpan get() = (1000.0 / fps).milliseconds

    enum class Quality {
        PERFORMANCE,
        QUALITY,
        //AUTO // @TODO: FAILS ON NATIVE! Because converted into .auto that is a C/C++/Objective-C keyword
        AUTOMATIC
    }

    open fun setSize(width: Int, height: Int) {
    }

    override suspend fun browse(url: URL): Unit = unsupported()
    override suspend fun alert(message: String): Unit = unsupported()
    override suspend fun confirm(message: String): Boolean = unsupported()
    override suspend fun prompt(message: String, default: String): String = unsupported()
    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> = unsupported()

    open suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }
        while (true) {
            frame()
            delay(16.milliseconds)
        }
    }

    open fun frame() {
        coroutineDispatcher.executePending()
        ag.onRender(ag)
        dispatch(renderEvent)
    }

    fun dispatchInitEvent() {
        dispatch(initEvent)
    }

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        ag.setViewport(0, 0, width, height)
        dispatch(reshapeEvent.apply {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        })
    }
}

fun GameWindow.mainLoop(entry: suspend GameWindow.() -> Unit) = Korio { loop(entry) }

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
