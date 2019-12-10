package com.soywiz.korgw

import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGFactory
import com.soywiz.korag.AGWindow
import com.soywiz.korgw.awt.AwtGameWindow
import com.soywiz.korgw.jogl.JoglGameWindow
import com.soywiz.korgw.osx.MacGameWindow
import com.soywiz.korgw.osx.initializeMacOnce
import com.soywiz.korgw.osx.isOSXMainThread
import com.soywiz.korgw.win32.Win32GameWindow
import com.soywiz.korgw.x11.X11GameWindow
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.OS
import kotlinx.coroutines.runBlocking

actual fun CreateDefaultGameWindow(): GameWindow {
    if (OS.isMac) {
        initializeMacOnce()
    }

    val engine = korgwJvmEngine
        ?: System.getenv("KORGW_JVM_ENGINE")
        ?: System.getProperty("korgw.jvm.engine")
        //?: "jogl"
        ?: "jna"

    return when (engine) {
        "jna" -> when {
            OS.isMac -> {
                when {
                    isOSXMainThread -> MacGameWindow()
                    else -> {
                        println("WARNING. Slower startup: NOT in main thread! Using AWT! (on mac use -XstartOnFirstThread when possible)")
                        AwtGameWindow()
                    }
                }
            }
            OS.isWindows -> Win32GameWindow()
            else -> X11GameWindow()
        }
        "jogl" -> {
            if (isOSXMainThread) {
                println("-XstartOnFirstThread not supported via Jogl, switching to an experimental native jna-based implementation")
                MacGameWindow()
            } else {
                // @TODO: Remove JoGL after a month once we ensure JNA/native versions work for everyone
                JoglGameWindow()
            }
        }
        else -> {
            error("Unsupported KORGW_JVM_ENGINE,korgw.jvm.engine='$engine'")
        }
    }
}

object JvmAGFactory : AGFactory {
    override val supportsNativeFrame: Boolean = true

    override fun create(nativeControl: Any?, config: AGConfig): AG {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        return CreateDefaultGameWindow().apply {
            this.title = title
            this.setSize(width, height)
        }
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
            var step = 0
            gameWindow.loop {
                val ag = gameWindow.ag
                ag.onRender {
                    ag.clear(RGBA(64, 96, step % 256, 255))
                    step++
                }
                //println("HELLO")
            }
        }
    }
}
