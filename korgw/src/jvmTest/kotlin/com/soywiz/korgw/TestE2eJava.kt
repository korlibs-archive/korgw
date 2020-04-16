package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korim.color.*
import kotlinx.coroutines.*
import kotlin.test.*

class TestE2eJava {
    @Test
    fun test() {
        runBlocking {
            val gameWindow = CreateDefaultGameWindow()
            //val gameWindow = Win32GameWindow()
            //val gameWindow = AwtGameWindow()
            gameWindow.addEventListener<MouseEvent> {
                if (it.type == MouseEvent.Type.CLICK) {
                    //println("MOUSE EVENT $it")
                    gameWindow.toggleFullScreen()
                }
            }
            //gameWindow.toggleFullScreen()
            gameWindow.setSize(320, 240)
            gameWindow.title = "HELLO WORLD"
            var step = 0
            gameWindow.loop {
                val ag = gameWindow.ag
                ag.onRender {
                    ag.clear(RGBA(64, 96, step % 256, 255))
                    step++
                    gameWindow.close()
                }
                //println("HELLO")
            }
            assertEquals(1, step)
        }
    }
}
