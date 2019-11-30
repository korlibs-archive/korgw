package com.soywiz.korgw.awt

import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlDummy
import com.soywiz.korag.AGOpengl
import com.soywiz.korag.versionString
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.platform.DummyOpenglContext
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11KmlGl
import com.soywiz.korgw.x11.X11OpenglContext
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.soywiz.korio.util.OS
import com.sun.jna.Native
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.win32.WinDef
import java.awt.*
import java.awt.Toolkit.getDefaultToolkit
import java.awt.event.*
import javax.swing.JFrame


class AwtAg(val window: AwtGameWindow) : AGOpengl() {
    override val nativeComponent: Any = window
    override val gl: KmlGl by lazy {
        when {
            OS.isMac -> KmlGlDummy
            OS.isWindows -> Win32KmlGl
            else -> X11KmlGl()
        }
    }
}

private fun <T> Class<T>.getMethodOrNull(name: String, vararg args: Class<*>) = runCatching { getDeclaredMethod(name, *args) }.getOrNull()
private fun <T> Class<T>.getFieldOrNull(name: String) = runCatching { getDeclaredField(name) }.getOrNull()

fun Frame.awtGetPeer(): Any {
    val method = this.javaClass.getMethodOrNull("getPeer")
    if (method != null) {
        method.isAccessible = true
        return method.invoke(this)
    }
    val field = this.javaClass.getFieldOrNull("peer")
    if (field != null) {
        field.isAccessible = true
        return field.get(this)
    }
    error("Can't get peer from Frame")
}

fun Component.awtNativeHandle(): Long {
    //val peer = this.awtGetPeer()
    //val hwnd = peer.javaClass.getFieldOrNull("hwnd")?.get(peer)
    //if (hwnd != null) return hwnd as Long
    //error("Can't get native handle from peer")
    return Native.getComponentID(this)
}

class AwtGameWindow : GameWindow() {
    override val ag: AwtAg = AwtAg(this)

    var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val frame = object : JFrame("Korgw") {
        init {
            ignoreRepaint = true
            setBounds(0, 0, 640, 480)
        }
        override fun paintComponents(g: Graphics?) {

        }

        override fun paint(g: Graphics) {
            val frame = this

            //println("PAINT")
            if (ctx == null) {
                ctx = when {
                    OS.isMac -> DummyOpenglContext
                    OS.isWindows -> Win32OpenglContext(
                        WinDef.HWND(Native.getComponentPointer(frame)), doubleBuffered = true
                    )
                    else -> {
                        val d = X.XOpenDisplay(null)
                        val d2 = X.XOpenDisplay(null)
                        println("DISPLAY: $d, $d2")
                        X11.Display()
                        X11OpenglContext(d, X11.Window(Native.getWindowID(frame)))
                    }
                }
            }

            ctx?.makeCurrent()
            try {
                val gl = ag.gl
                gl.viewport(0, 0, width, height)
                gl.clearColor(.2f, .4f, .9f, 1f)
                gl.clear(gl.COLOR_BUFFER_BIT)
                //println(gl.getString(gl.VERSION))
                //println(gl.versionString)
                frame()
                ctx?.swapBuffers()
            } finally {
                ctx?.releaseCurrent()
            }
            //println("FRAME!")
        }
    }


    override var title: String
        get() = frame.title
        set(value) = run { frame.title = value }
    override val width: Int get() = frame.width
    override val height: Int get() = frame.height
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = super.fullscreen
        set(value) {}
    override var visible: Boolean
        get() = frame.isVisible
        set(value) {
            frame.isVisible = value
        }
    override var quality: Quality = Quality.AUTOMATIC

    override fun setSize(width: Int, height: Int) {
        frame.setSize(width, height)
    }

    override suspend fun browse(url: URL) {
        //Shell32.ShellExecute()
        //ShellExecute(0, 0, L"http://www.google.com", 0, 0 , SW_SHOW );

        super.browse(url)
    }

    override suspend fun alert(message: String) {
        return super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        return super.openFileDialog(filter, write, multi)
    }

    var exiting = false

    override fun close() {
        super.close()
        exiting = true
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }
        //frame.setBounds(0, 0, width, height)
        val dim = getDefaultToolkit().screenSize
        frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)
        frame.isVisible = true

        //val timer= Timer(40, ActionListener {
        //})

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exiting = true
            }
        })

        fun dispatchReshapeEvent() {
            queue {
                dispatchReshapeEvent(frame.x, frame.y, frame.width, frame.height)
            }
        }

        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                frame.repaint()
                dispatchReshapeEvent()
            }
        })

        fun handleMouseEvent(e: MouseEvent) {
            queue {
                //dispatchMouseEvent(com.soywiz.korev.MouseEvent.Type.MOVE)
            }
        }

        frame.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
        })

        frame.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseEntered(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseClicked(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseExited(e: MouseEvent) = handleMouseEvent(e)
            override fun mousePressed(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseWheelMoved(e: MouseWheelEvent) = handleMouseEvent(e)
        })

        dispatchInitEvent()
        dispatchReshapeEvent()

        while (!exiting) {
            //frame.invalidate()
            EventQueue.invokeLater {
                frame.repaint()
            }
            Thread.sleep((1000 / fps).toLong())
        }

        dispatchDestroyEvent()

        System.exit(0)
    }
}
