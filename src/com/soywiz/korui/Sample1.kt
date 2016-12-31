package com.soywiz.korui

import com.soywiz.kimage.bitmap.Bitmap32
import com.soywiz.kimage.color.Colors
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Promise
import kotlin.coroutines.startCoroutine

fun main(args: Array<String>) = EventLoop.main {
    Application().frame {
        vertical {
            width = 50.percent
            button("hello") {
                onClick { spawn { alert("hello") } }
            }.apply {
                width = 50.percent
            }
            button("world") { onClick { spawn { alert("world") } } }
            image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) Colors.WHITE else Colors.BLACK })) {
            }
            button("test") { onClick { spawn { alert("world") } } }
            image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) Colors.WHITE else Colors.BLACK })) {
            }
        }
        image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) Colors.WHITE else Colors.BLACK })) {
            setSize(100.percent, 100.percent)
        }

        //image(Bitmap32(50, 50, { _, _ -> Colors.WHITE })) {
        //    setBoundsInternal(0, 0, 100, 100)
        //}
        /*
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
        */
    }
}

fun <T> spawn(task: suspend () -> T): Promise<T> {
    val deferred = Promise.Deferred<T>()
    task.startCoroutine(deferred.toContinuation())
    return deferred.promise
}
