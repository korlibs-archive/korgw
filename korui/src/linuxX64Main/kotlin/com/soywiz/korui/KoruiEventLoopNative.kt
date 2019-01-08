package com.soywiz.korui

import GL.*
import com.soywiz.kds.PriorityQueue
import com.soywiz.kds.Queue
import com.soywiz.klock.DateTime
import com.soywiz.klock.milliseconds
import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGOpenglFactory
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.DummyCloseable
import com.soywiz.korui.event.Event
import com.soywiz.korui.event.EventDispatcher
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightType
import com.soywiz.korui.light.ag
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass

@UseExperimental(InternalCoroutinesApi::class)
class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val ms: DateTime, val continuation: CancellableContinuation<Unit>)

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
        val task = TimedTask(DateTime.now() + timeMillis.milliseconds, continuation)
        continuation.invokeOnCancellation {
            timedTasks.remove(task)
        }
        timedTasks.add(task)
    }

    fun executeStep() {
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

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

class NativeKoruiContext(
    val ag: AG,
    val light: LightComponents
    //, val app: NSApplication
) : KoruiContext()

class NativeLightComponents(val nkcAg: AG) : LightComponents() {
    val frameHandle = Any()

    override fun create(type: LightType, config: Any?): LightComponentInfo {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val handle: Any = when (type) {
            LightType.FRAME -> frameHandle
            LightType.CONTAINER -> Any()
            LightType.BUTTON -> Any()
            LightType.IMAGE -> Any()
            LightType.PROGRESS -> Any()
            LightType.LABEL -> Any()
            LightType.TEXT_FIELD -> Any()
            LightType.TEXT_AREA -> Any()
            LightType.CHECK_BOX -> Any()
            LightType.SCROLL_PANE -> Any()
            LightType.AGCANVAS -> nkcAg.nativeComponent
            else -> throw UnsupportedOperationException("Type: $type")
        }
        return LightComponentInfo(handle).apply {
            this.ag = nkcAg
        }
    }

    val eds = arrayListOf<Pair<KClass<*>, EventDispatcher>>()

    fun <T : Event> dispatch(clazz: KClass<T>, e: T) {
        for ((eclazz, ed) in eds) {
            if (eclazz == clazz) {
                ed.dispatch(clazz, e)
            }
        }
    }

    inline fun <reified T : Event> dispatch(e: T) = dispatch(T::class, e)

    override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
        val pair = Pair(clazz, ed)

        if (c === frameHandle || c === nkcAg.nativeComponent) {
            eds += pair
            return Closeable { eds -= pair }
        }

        return DummyCloseable
    }

    override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
        TODO()
    }
}

data class WindowConfig(
    val width: Int = 640,
    val height: Int = 480,
    val title: String = "Sample"
)

@ThreadLocal
val agNativeComponent = Any()

@ThreadLocal
val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

@ThreadLocal
val light = NativeLightComponents(ag)

@ThreadLocal
val ctx = NativeKoruiContext(ag, light)

@ThreadLocal
val windowConfig = WindowConfig()

fun glutDisplay() {
    myNativeCoroutineDispatcher.executeStep()
    ag.onRender(ag)
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
    memScoped {
        val argc = alloc<IntVar>().apply { value = 0 }
        glutInit(argc.ptr, null) // TODO: pass real args
    }

    glutInitDisplayMode((GLUT_RGB or GLUT_DOUBLE or GLUT_DEPTH).convert())
    glutInitWindowSize(windowConfig.width, windowConfig.height)
    glutCreateWindow(windowConfig.title)

    glutDisplayFunc(staticCFunction(::glutDisplay))
    glutIdleFunc(staticCFunction(::glutDisplay))

    var running = true
    CoroutineScope(coroutineContext).launch(KoruiDispatcher) {
        try {
            entry(ctx)
        } catch (e: Throwable) {
            println(e)
            running = false
        }
    }

    glutMainLoop()
}
