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
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

expect fun CreateDefaultGameWindow(): GameWindow

interface DialogInterface {
    suspend fun browse(url: URL): Unit = unsupported()
    suspend fun alert(message: String): Unit = unsupported()
    suspend fun confirm(message: String): Boolean = unsupported()
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    // @TODO: Provide current directory
    suspend fun openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> =
        unsupported()
}

@UseExperimental(InternalCoroutinesApi::class)
open class GameWindowCoroutineDispatcher : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val time: KorgwPerformanceCounter, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask> { a, b -> a.time.compareTo(b.time) }

    fun queue(block: () -> Unit) {
        tasks.enqueue(Runnable { block() })
    }

    fun queue(block: Runnable?) {
        if (block != null) {
            tasks.enqueue(block)
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    open fun now() = KorgwPerformanceCounter.now()

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(KorgwPerformanceCounter(timeMillis.milliseconds.microseconds), continuation)
    }

    fun scheduleResumeAfterDelay(time: KorgwPerformanceCounter, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now() + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        timedTasks.add(task)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
        val task = TimedTask(now() + timeMillis.milliseconds, null, block)
        timedTasks.add(task)
        return object : DisposableHandle {
            override fun dispose() {
                timedTasks.remove(task)
            }
        }
    }

    @Deprecated("")
    open fun executePending() {
        executePending(KorgwPerformanceCounter(1.seconds))
    }

    fun executePending(availableTime: KorgwPerformanceCounter) {
        try {
            val startTime = now()
            while (timedTasks.isNotEmpty() && startTime >= timedTasks.head.time) {
                val item = timedTasks.removeHead()
                if (item.exception != null) {
                    item.continuation?.resumeWithException(item.exception!!)
                    if (item.callback != null) {
                        item.exception?.printStackTrace()
                    }
                } else {
                    item.continuation?.resume(Unit)
                    item.callback?.run()
                }
                if ((now() - startTime) >= availableTime) break
            }

            while (tasks.isNotEmpty()) {
                val task = tasks.dequeue()
                task?.run()
                if ((now() - startTime) >= availableTime) break
            }
        } catch (e: Throwable) {
            println("Error in GameWindowCoroutineDispatcher.executePending:")
            e.printStackTrace()
        }
    }

    override fun close() {
        executePending(KorgwPerformanceCounter(1.seconds))
        println("GameWindowCoroutineDispatcher.close")
        while (timedTasks.isNotEmpty()) {
            timedTasks.removeHead().continuation?.resume(Unit)
        }
        while (tasks.isNotEmpty()) {
            tasks.dequeue()?.run()
        }
    }

    override fun toString(): String = "GameWindowCoroutineDispatcher"
}

open class GameWindow : EventDispatcher.Mixin(), DialogInterface, Closeable, CoroutineContext.Element, AGWindow {
    override val key: CoroutineContext.Key<*> get() = CoroutineKey
    companion object CoroutineKey : CoroutineContext.Key<GameWindow>

    override val ag: AG = LogAG()
    open val coroutineDispatcher: GameWindowCoroutineDispatcher = GameWindowCoroutineDispatcher()

    fun queue(callback: () -> Unit) = coroutineDispatcher.queue(callback)
    fun queue(callback: Runnable) = coroutineDispatcher.queue(callback)

    protected val pauseEvent = PauseEvent()
    protected val resumeEvent = ResumeEvent()
    protected val stopEvent = StopEvent()
    protected val destroyEvent = DestroyEvent()
    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    private val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()
    @Deprecated("") protected val gamePadButtonEvent = GamePadButtonEvent()
    @Deprecated("") protected val gamePadStickEvent = GamePadStickEvent()
    protected val gamePadUpdateEvent = GamePadUpdateEvent()
    protected val gamePadConnectionEvent = GamePadConnectionEvent()

    protected open fun _setFps(fps: Int): Int {
        return if (fps <= 0) 60 else fps
    }

    var counterTimePerFrame: KorgwPerformanceCounter = KorgwPerformanceCounter(0.0); private set
    val timePerFrame: TimeSpan get() = counterTimePerFrame.timeSpan

    var fps: Int = 60
        set(value) {
            val value = _setFps(value)
            field = value
            counterTimePerFrame = KorgwPerformanceCounter(1_000_000.0 / value)
        }

    init {
        fps = 60
    }

    open var title: String get() = ""; set(value) = Unit
    open val width: Int = 0
    open val height: Int = 0
    open var icon: Bitmap? = null
    open var fullscreen: Boolean = false
    open var visible: Boolean = false
    open var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    /**
     * Describes if the rendering should focus on performance or quality.
     * [PERFORMANCE] will use lower resolutions, while [QUALITY] will use the devicePixelRatio
     * to render high quality images.
     */
    enum class Quality {
        /** Will render to lower resolutions, ignoring devicePixelRatio on retina-like screens */
        PERFORMANCE,
        /** Will render to higher resolutions, using devicePixelRatio on retina-like screens */
        QUALITY,
        /** Will choose [PERFORMANCE] or [QUALITY] based on some heuristics */
        AUTOMATIC;

        private val UPPER_BOUND_RENDERED_PIXELS = 4_000_000

        fun computeTargetScale(
            width: Int,
            height: Int,
            devicePixelRatio: Double,
            targetPixels: Int = UPPER_BOUND_RENDERED_PIXELS
        ): Double = when (this) {
            PERFORMANCE -> 1.0
            QUALITY -> devicePixelRatio
            AUTOMATIC -> {
                listOf(devicePixelRatio, 2.0, 1.0)
                    .firstOrNull { width * height * it <= targetPixels }
                    ?: 1.0
            }
        }
    }

    open fun setSize(width: Int, height: Int): Unit = Unit
    // Alias for close
    fun exit(): Unit = close()

    var running = true; protected set
    override fun close() = run {
        running = false
        println("GameWindow.close")
        coroutineDispatcher.close()
        coroutineDispatcher.cancelChildren()
    }

    override fun repaint() {
    }

    open suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }
        while (running) {
            val start = KorgwPerformanceCounter.now()
            frame()
            val elapsed = KorgwPerformanceCounter.now() - start
            val available = counterTimePerFrame - elapsed
            delay(available)
        }
    }

    fun frame(doUpdate: Boolean = true, startTime: KorgwPerformanceCounter = KorgwPerformanceCounter.now()) {
        try {
            ag.onRender(ag)
            dispatchRenderEvent()
            if (doUpdate) {
                val elapsed = KorgwPerformanceCounter.now() - startTime
                val available = counterTimePerFrame - elapsed
                coroutineDispatcher.executePending(available)
            }
        } catch (e: Throwable) {
            println("ERROR GameWindow.frame:")
            println(e)
        }
    }

    fun dispatchInitEvent() {
        dispatch(initEvent)
    }

    fun dispatchPauseEvent() {
        dispatch(pauseEvent)
    }

    fun dispatchResumeEvent() {
        dispatch(resumeEvent)
    }

    fun dispatchStopEvent() {
        dispatch(stopEvent)
    }

    fun dispatchDestroyEvent() {
        dispatch(destroyEvent)
    }

    fun dispatchDisposeEvent() {
        dispatch(disposeEvent)
    }

    fun dispatchRenderEvent() {
        dispatch(renderEvent)
    }

    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) {
        dispatch(dropFileEvent.apply {
            this.type = type
            this.files = files
        })
    }

    fun dispatchFullscreenEvent(fullscreen: Boolean) {
        dispatch(fullScreenEvent.apply {
            this.fullscreen = fullscreen
        })
    }

    fun dispatchReshapeEvent(x: Int, y: Int, width: Int, height: Int) {
        ag.resized(width, height)
        dispatch(reshapeEvent.apply {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
        })
    }

    fun dispatchKeyEvent(type: KeyEvent.Type, id: Int, character: Char, key: Key, keyCode: Int) {
        dispatch(keyEvent.apply {
            this.id = id
            this.character = character
            this.key = key
            this.keyCode = keyCode
            this.type = type
        })
    }

    fun dispatchSimpleMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, simulateClickOnUp: Boolean = false
    ) {
        val buttons = 0
        val scrollDeltaX: Double = 0.0
        val scrollDeltaY: Double = 0.0
        val scrollDeltaZ: Double = 0.0
        val isShiftDown: Boolean = false
        val isCtrlDown: Boolean = false
        val isAltDown: Boolean = false
        val isMetaDown: Boolean = false
        val scaleCoords = false
        dispatchMouseEvent(type, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords)
        if (simulateClickOnUp && type == MouseEvent.Type.UP) {
            dispatchMouseEvent(MouseEvent.Type.CLICK, id, x, y, button, buttons, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, scaleCoords)
        }
    }

    fun dispatchMouseEvent(
        type: MouseEvent.Type, id: Int, x: Int, y: Int, button: MouseButton, buttons: Int,
        scrollDeltaX: Double, scrollDeltaY: Double, scrollDeltaZ: Double,
        isShiftDown: Boolean, isCtrlDown: Boolean, isAltDown: Boolean, isMetaDown: Boolean,
        scaleCoords: Boolean
    ) {
        dispatch(mouseEvent.apply {
            this.type = type
            this.id = id
            this.x = x
            this.y = y
            this.button = button
            this.buttons = buttons
            this.scrollDeltaX = scrollDeltaX
            this.scrollDeltaY = scrollDeltaY
            this.scrollDeltaZ = scrollDeltaZ
            this.isShiftDown = isShiftDown
            this.isCtrlDown = isCtrlDown
            this.isAltDown = isAltDown
            this.isMetaDown = isMetaDown
            this.scaleCoords = scaleCoords
        })
    }

    fun dispatchTouchEventStartStart() = dispatchTouchEventStart(TouchEvent.Type.START)
    fun dispatchTouchEventStartMove() = dispatchTouchEventStart(TouchEvent.Type.MOVE)
    fun dispatchTouchEventStartEnd() = dispatchTouchEventStart(TouchEvent.Type.END)

    fun dispatchTouchEventStart(type: TouchEvent.Type) {
        touchEvent.startFrame(type)
    }

    fun dispatchTouchEventAddTouch(id: Int, x: Double, y: Double) {
        touchEvent.touch(id, x, y)
    }

    fun dispatchTouchEventEnd() {
        dispatch(touchEvent)
    }

    fun entry(callback: suspend () -> Unit) {
        launch(coroutineDispatcher) {
            try {
                callback()
            } catch (e: Throwable) {
                println("ERROR GameWindow.entry:")
                e.printStackTrace()
            }
        }
        /*
        callback.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext get() = coroutineDispatcher
            override fun resumeWith(result: Result<Unit>) {
                if (result.isFailure) {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        })
        */
    }
}

open class EventLoopGameWindow : GameWindow() {
    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        // Required here so setSize is called
        launchImmediately(coroutineDispatcher) {
            entry()
        }

        doInitialize()
        dispatchInitEvent()

        while (running) {
            doHandleEvents()
            if (elapsedSinceLastRenderTime() >= counterTimePerFrame) {
                render(doUpdate = true)
            }
            // Here we can trigger a GC if we have enough time, and we can try to disable GC all the other times.
            doSmallSleep()
        }
        dispatchStopEvent()
        dispatchDestroyEvent()

        doDestroy()
    }

    var lastRenderTime = KorgwPerformanceCounter.now()
    fun elapsedSinceLastRenderTime() = KorgwPerformanceCounter.now() - lastRenderTime
    fun render(doUpdate: Boolean) {
        lastRenderTime = KorgwPerformanceCounter.now()
        doInitRender()
        frame(doUpdate, lastRenderTime)
        doSwapBuffers()
    }

    protected open fun doSmallSleep() {
    }

    protected open fun doHandleEvents() {
    }

    protected open fun doInitRender() {
    }

    protected open fun doSwapBuffers() {
    }

    protected open fun doInitialize() {
    }

    protected open fun doDestroy() {

    }
}

open class ZenityDialogs : DialogInterface {
    open suspend fun exec(vararg args: String): String {
        return localCurrentDirVfs.execToString(args.toList())
    }

    override suspend fun browse(url: URL) {
        exec("xdg-open", url.toString())
    }

    override suspend fun alert(message: String) {
        exec("zenity", "--warning", "--text=$message")
    }

    override suspend fun confirm(message: String): Boolean =
        try {
            exec("zenity", "--question", "--text=$message")
            true
        } catch (e: Throwable) {
            false
        }

    override suspend fun prompt(message: String, default: String): String = try {
        exec(
            "zenity",
            "--question",
            "--text=$message",
            "--entry-text=$default"
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        ""
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        return exec(*com.soywiz.korio.util.buildList<String> {
            add("zenity")
            add("--file-selection")
            if (multi) add("--multiple")
            if (write) add("--save")
            if (filter != null) {
                //add("--file-filter=$filter")
            }
        }.toTypedArray())
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { localVfs(it.trim()) }
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
