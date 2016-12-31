package com.soywiz.korui

import com.soywiz.korui.log.LogLightComponents
import org.junit.Assert
import org.junit.Test

class BasicTest {
    val lc = LogLightComponents()

    @Test
    fun name() {
        val frame = Application(lc).frame {

        }
        Assert.assertEquals(
                listOf(
                        "create(frame)=0",
                        "setVisible(0,true)"
                ),
                lc.log
        )
    }
}