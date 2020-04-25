package com.soywiz.korgw

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

inline class KorgwPerformanceCounter(val microseconds: Double) : Comparable<KorgwPerformanceCounter> {
    val nanoseconds: Double get() = microseconds * 1000.0
    val milliseconds: Double get() = microseconds / 1000.0
    val timeSpan get() = TimeSpan(milliseconds)
    companion object {
        fun now() = KorgwPerformanceCounter(PerformanceCounter.microseconds)
        operator fun invoke(time: TimeSpan) = KorgwPerformanceCounter(time.microseconds)
    }

    operator fun minus(other: KorgwPerformanceCounter) = KorgwPerformanceCounter(this.microseconds - other.microseconds)
    operator fun plus(other: KorgwPerformanceCounter) = KorgwPerformanceCounter(this.microseconds + other.microseconds)
    operator fun plus(other: TimeSpan) = KorgwPerformanceCounter(this.microseconds + other.microseconds)

    override fun compareTo(other: KorgwPerformanceCounter): Int = this.microseconds.compareTo(other.microseconds)
}
fun max(a: KorgwPerformanceCounter, b: KorgwPerformanceCounter) = if (a > b) a else b
fun min(a: KorgwPerformanceCounter, b: KorgwPerformanceCounter) = if (a < b) a else b

suspend fun delay(time: KorgwPerformanceCounter) {
    if (time.microseconds <= 0) return // don't delay
    val dispatcher = coroutineContext[ContinuationInterceptor] as? GameWindowCoroutineDispatcher
    if (dispatcher != null) {
        return suspendCancellableCoroutine sc@ { cont: CancellableContinuation<Unit> ->
            dispatcher.scheduleResumeAfterDelay(time, cont)
        }
    } else {
        // Use default delay dispatcher
        delay(time.milliseconds.toLong())
    }
}
