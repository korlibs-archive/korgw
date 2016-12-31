package com.soywiz.korui

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Promise
import java.util.concurrent.CancellationException
import kotlin.coroutines.startCoroutine

fun main(args: Array<String>) = EventLoop.main {
    light.frame {
        button {
            top = 50.percent
            width = 50.percent
            height = 50.percent
            setBoundsInternal(0, 0, 100, 100)
            onClick {
                println("click!")
                spawn {
                    println("click [work]!")
                    alert("Button pressed!")
                    try {
                        val file = dialogOpenFile()
                        println(file.readString())
                    } catch (t: CancellationException) {
                        println("cancelled!")
                    }
                }
            }
        }
        setBoundsInternal(0, 0, 100, 100)
    }
}

fun <T> spawn(task: suspend () -> T): Promise<T> {
    val deferred = Promise.Deferred<T>()
    task.startCoroutine(deferred.toContinuation())
    return deferred.promise
}
