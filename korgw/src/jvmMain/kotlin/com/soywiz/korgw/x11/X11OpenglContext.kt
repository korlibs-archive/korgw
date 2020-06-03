package com.soywiz.korgw.x11

import com.soywiz.korgw.platform.*
import com.soywiz.korio.lang.*
import com.sun.jna.platform.unix.*

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: X11.Display?, val w: X11.Window?, val scr: Int, val vi: XVisualInfo? = chooseVisuals(d, scr), val doubleBuffered: Boolean = true) : BaseOpenglContext {
    companion object {
        fun chooseVisuals(d: X11.Display?, scr: Int = X.XDefaultScreen(d)): XVisualInfo? {
            val attrs = intArrayOf(
                GLX_RGBA,
                GLX_DOUBLEBUFFER,
                //GLX_RED_SIZE, 8,
                //GLX_GREEN_SIZE, 8,
                //GLX_BLUE_SIZE, 8,
                //GLX_DEPTH_SIZE, 16,
                GLX_DEPTH_SIZE, 24,
                // *(if (doubleBuffered) intArrayOf(GLX_DOUBLEBUFFER) else intArrayOf()),
                X11.None
            )//.map { it.toLong() }.toLongArray()
            return X.glXChooseVisual(d, scr, attrs).also {
                println("VI: $it")
            }
        }
    }
    init {
        println("Preparing OpenGL context. Screen: $scr")
    }
    init {
        println("VI: $vi")
    }
    val glc = if (vi != null) X.glXCreateContext(d, vi, null, true) else null

    init {
        if (vi == null || glc == null) {
            println("WARNING! Visuals or GLC are NULL! This will probably cause a white window")
        }
        println("d: $d, w: $w, s: $scr, VI: $vi, glc: $glc")
        makeCurrent()
        println("GL_RENDERER: '" + X.glGetString(GL.GL_RENDERER) + "'")
        println("GL_VENDOR: '" + X.glGetString(GL.GL_VENDOR) + "'")
        println("GL_VERSION: '" + X.glGetString(GL.GL_VERSION) + "'")
    }

    val extensions = (X.glGetString(GL.GL_EXTENSIONS) ?: "").split(" ").toSet()

    init {
        println("GL_EXTENSIONS: " + extensions.size)
        if (Environment["GL_DUMP_EXTENSIONS"] == "true") {
            println("GL_EXTENSIONS: '$extensions'")
        }
    }

    override fun makeCurrent() {
        val result = X.glXMakeCurrent(d, w, glc)
        //println("makeCurrent: $result")
    }

    override fun swapBuffers() {
        val result = X.glXSwapBuffers(d, w)
        //println("swapBuffers: $result")
    }
}
