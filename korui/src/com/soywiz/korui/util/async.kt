package com.soywiz.korui.util

import com.soywiz.korio.async.Promise
import kotlin.coroutines.startCoroutine

// @TODO: Next version of korio already has this!
fun <T> spawn(task: suspend () -> T): Promise<T> {
    val deferred = Promise.Deferred<T>()
    task.startCoroutine(deferred.toContinuation())
    return deferred.promise
}
