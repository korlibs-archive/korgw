package com.soywiz.korgw.x11

import com.soywiz.korgw.platform.*
import com.soywiz.korio.lang.*
import com.sun.jna.platform.unix.*

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: X11.Display?, val w: X11.Window?, val doubleBuffered: Boolean = true) : BaseOpenglContext {
    val vi = X.glXChooseVisual(d, 0, intArrayOf(
        GLX_RGBA,
        GLX_DEPTH_SIZE, 24,
        //*(if (doubleBuffered) intArrayOf(GLX_DOUBLEBUFFER) else intArrayOf()),
        X11.None
    ))
    val glc = X.glXCreateContext(d, vi, null, true)

    init {
        println("VI: $vi, d: $d, w: $w, glc: $glc")
        makeCurrent()
        println("GL_VENDOR: " + X.glGetString(GL.GL_VENDOR))
        println("GL_VERSION: " + X.glGetString(GL.GL_VERSION))
    }

    val extensions = (X.glGetString(GL.GL_EXTENSIONS) ?: "").split(" ").toSet()

    init {
        println("GL_EXTENSIONS: " + extensions.size)
        if (Environment["GL_DUMP_EXTENSIONS"] == "true") {
            println("GL_EXTENSIONS: $extensions")
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
