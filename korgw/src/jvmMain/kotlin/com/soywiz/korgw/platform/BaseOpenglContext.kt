package com.soywiz.korgw.platform

interface BaseOpenglContext {
    val scaleFactor: Double get() = 1.0
    fun useContext(obj: Any?, action: Runnable) {
        makeCurrent()
        try {
            action.run()
        } finally {
            swapBuffers()
            releaseCurrent()
        }
    }

    fun makeCurrent()
    fun releaseCurrent() {
    }
    fun swapBuffers()
}

object DummyOpenglContext : BaseOpenglContext {
    override fun makeCurrent() {
    }

    override fun swapBuffers() {
    }
}
