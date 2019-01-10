package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.*

fun main(args: Array<String>) = Korio {
    DefaultGameWindow.loop {
        configure(640, 480, "hello", fullscreen = false)
        addEventListener<MouseEvent> { e ->
            if (e.type == MouseEvent.Type.CLICK) {
                toggleFullScreen()
            }
            //    //fullscreen = !fullscreen
            //    configure(1280, 720, "KORGW!", fullscreen = false)
            //}
            //println(e)
        }
        addEventListener<RenderEvent> {
            ag.clear(Colors.GREEN)
            //ag.flip()
        }
    }
}
