package com.soywiz.korui.async

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*


// @TODO: Do this better! (JS should use requestAnimationFrame)
suspend fun delayNextFrame() = _delayNextFrame()

interface DelayFrame {
    fun delayFrame(continuation: CancellableContinuation<Unit>) {
        //continuation.context.get(ContinuationInterceptor) as? Delay ?: DefaultDelay
        launchImmediately(continuation.context) {
            delay(16)
            continuation.resume(Unit)
        }
    }
}

suspend fun DelayFrame.delayFrame() = suspendCancellableCoroutine<Unit> { c -> delayFrame(c) }

val DefaultDelayFrame: DelayFrame = object : DelayFrame {}

val CoroutineContext.delayFrame: DelayFrame get() = get(ContinuationInterceptor) as? DelayFrame ?: DefaultDelayFrame


private suspend fun _delayNextFrame() = coroutineContext.delayFrame.delayFrame()
suspend fun CoroutineContext.delayNextFrame() = delayFrame.delayFrame()

fun CoroutineScope.animationFrameLoop(callback: suspend (Closeable) -> Unit): Closeable {
    var running = true
    val close = Closeable {
        running = false
    }
    launchImmediately {
        while (running) {
            callback(close)
            delayNextFrame()
        }
    }
    return close
}
