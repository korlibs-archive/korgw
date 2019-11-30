package com.soywiz.korgw

import Win32GameWindow
import com.soywiz.korgw.awt.AwtGameWindow
import com.soywiz.korgw.x11.X11GameWindow
import com.soywiz.korio.util.OS
import kotlinx.coroutines.runBlocking

actual fun CreateDefaultGameWindow(): GameWindow = when {
    OS.isMac -> {
        when {
            //MacGameWindow.isMainThread -> MacGameWindow()
            else -> {
                println("WARNING. Slower startup: NOT in main thread! Using AWT!")
                //CreateJoglGameWindow()
                AwtGameWindow()
            }
        }
    }
    OS.isWindows -> {
        Win32GameWindow()
        //AwtGameWindow()
    }
    else -> {
        X11GameWindow()
        //AwtGameWindow()
        //CreateJoglGameWindow()
    }
}

object TestGameWindow {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val gameWindow = CreateDefaultGameWindow()
            //val gameWindow = Win32GameWindow()
            //val gameWindow = AwtGameWindow()
            gameWindow.setSize(320, 240)
            gameWindow.title = "HELLO WORLD"
            gameWindow.loop {
                println("HELLO")
            }
        }
    }
}
