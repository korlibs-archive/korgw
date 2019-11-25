package com.soywiz.korgw

import com.soywiz.korio.util.OS

actual fun CreateDefaultGameWindow(): GameWindow = when {
    OS.isMac -> {
        when {
            //MacGameWindow.isMainThread -> CreateDefaultGameWindowMac()
            else -> {
                println("WARNING. Slower startup: NOT in main thread! Using AWT!")
                CreateJoglGameWindow()
            }
        }
    }
    else -> CreateJoglGameWindow()
}
