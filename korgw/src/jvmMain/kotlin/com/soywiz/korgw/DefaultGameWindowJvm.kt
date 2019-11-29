package com.soywiz.korgw

import Win32GameWindow
import com.soywiz.korio.util.OS

actual fun CreateDefaultGameWindow(): GameWindow = when {
    OS.isMac -> {
        when {
            //MacGameWindow.isMainThread -> MacGameWindow()
            else -> {
                println("WARNING. Slower startup: NOT in main thread! Using AWT!")
                CreateJoglGameWindow()
            }
        }
    }
    OS.isWindows -> {
        Win32GameWindow()
    }
    else -> CreateJoglGameWindow()
}
