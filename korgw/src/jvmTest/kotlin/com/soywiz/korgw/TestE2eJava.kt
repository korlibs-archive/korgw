package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.test.*

class TestE2eJava {
    @Test
    fun test() {
        runBlocking {
            val WIDTH = 64
            val HEIGHT = 64
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
            gameWindow.setSize(WIDTH, HEIGHT)
            gameWindow.title = "HELLO WORLD"
            var step = 0
            val bmp = Bitmap32(64, 64)
            var exception: Throwable? = null
            gameWindow.loop {
                val ag = gameWindow.ag
                ag.onRender {
                    try {
                        ag.clear(Colors.DARKGREY)
                        ag.createVertexBuffer(floatArrayOf(
                            -1f, -1f,
                            -1f, +1f,
                            +1f, +1f
                        )).use { vertices ->
                            ag.draw(
                                vertices,
                                program = DefaultShaders.PROGRAM_DEBUG,
                                type = AG.DrawType.TRIANGLES,
                                vertexLayout = DefaultShaders.LAYOUT_DEBUG,
                                vertexCount = 3,
                                uniforms = AG.UniformValues(
                                    //DefaultShaders.u_ProjMat to Matrix3D().setToOrtho(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), -1f, +1f)
                                )
                            )
                        }
                        ag.readColor(bmp)
                        step++
                    } catch (e: Throwable) {
                        exception = e
                    } finally {
                        gameWindow.close()
                    }
                }
                //println("HELLO")
            }
            assertEquals(1, step)

            // @TODO: Ignore colors for now. Just ensure that

            //assertEquals(Colors.RED, bmp[0, 63])
            //assertEquals(Colors.DARKGREY, bmp[63, 0])
            if (exception != null) {
                throw exception!!
            }
        }
    }
}
