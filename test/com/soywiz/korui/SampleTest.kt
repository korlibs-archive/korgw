package com.soywiz.korui

import com.soywiz.korui.log.LogLightComponents
import org.junit.Test

class SampleTest {
    val lc = LogLightComponents()

    @Test
    fun name() {
        val frame = lc.createFrame()
    }
}