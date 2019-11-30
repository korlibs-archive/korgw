package com.soywiz.korgw.platform

interface BaseOpenglContext {
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
