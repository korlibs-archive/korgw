package com.soywiz.korgw.awt

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.osx.MacKmlGL
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.win32.Win32OpenglContext
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
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.Toolkit.getDefaultToolkit
import java.awt.event.*
import javax.swing.JFrame


class AwtAg(val window: AwtGameWindow) : AGOpengl() {
    override val nativeComponent: Any = window
    override val gles: Boolean = true
    override val gl: KmlGl by lazy {
        when {
            OS.isMac -> MacKmlGL
            OS.isWindows -> Win32KmlGl
            else -> X11KmlGl
        }
    }
}

class AwtGameWindow : GameWindow() {
    override val ag: AwtAg = AwtAg(this)

    var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val frame = object : JFrame("Korgw") {
        //val frame = object : Frame("Korgw") {
        init {
            isVisible = false
            ignoreRepaint = true
            setBounds(0, 0, 640, 480)
            val frame = this
            val dim = getDefaultToolkit().screenSize
            frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)
        }

        override fun paintComponents(g: Graphics?) {

        }

        override fun paint(g: Graphics) {
            val frame = this

            if (ctx == null) {
                ctx = when {
                    OS.isMac -> {
                        val utils = Class.forName("sun.java2d.opengl.OGLUtilities")
                        val invokeWithOGLContextCurrentMethod = utils.getDeclaredMethod(
                            "invokeWithOGLContextCurrent",
                            Graphics::class.java, Runnable::class.java
                        )
                        invokeWithOGLContextCurrentMethod.isAccessible = true

                        object : BaseOpenglContext {
                            override fun useContext(obj: Any?, action: Runnable) {
                                invokeWithOGLContextCurrentMethod.invoke(null, obj as Graphics, action)
                            }

                            override fun makeCurrent() {
                            }

                            override fun releaseCurrent() {
                            }

                            override fun swapBuffers() {
                            }
                        }
                    }
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

            ctx?.useContext(g, Runnable {
                val gl = ag.gl
                gl.viewport(0, 0, this@AwtGameWindow.width, this@AwtGameWindow.height)
                //gl.clearColor(.2f, .4f, .9f, 1f)
                gl.clearColor(.3f, .3f, .3f, 1f)
                gl.clear(gl.COLOR_BUFFER_BIT)
                //println(gl.getString(gl.VERSION))
                //println(gl.versionString)
                frame()
            })
            //println("FRAME!")
        }
    }


    override var title: String
        get() = frame.title
        set(value) = run { frame.title = value }
    override val width: Int get() = frame.contentPane.width
    override val height: Int get() = frame.contentPane.height
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
        frame.contentPane.setSize(width, height)
        frame.contentPane.preferredSize = Dimension(width, height)
        frame.pack()
        val dim = getDefaultToolkit().screenSize
        frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)
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

        //val timer= Timer(40, ActionListener {
        //})

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exiting = true
            }
        })

        fun dispatchReshapeEvent() {
            dispatchReshapeEvent(frame.x, frame.y, frame.contentPane.width, frame.contentPane.height)
        }

        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                queue {
                    dispatchReshapeEvent()
                    frame.repaint()
                }
            }
        })

        fun handleMouseEvent(e: MouseEvent) {
            queue {
                val ev = when (e.id) {
                    MouseEvent.MOUSE_MOVED -> com.soywiz.korev.MouseEvent.Type.MOVE
                    MouseEvent.MOUSE_CLICKED -> com.soywiz.korev.MouseEvent.Type.CLICK
                    MouseEvent.MOUSE_PRESSED -> com.soywiz.korev.MouseEvent.Type.DOWN
                    MouseEvent.MOUSE_RELEASED -> com.soywiz.korev.MouseEvent.Type.UP
                    else -> com.soywiz.korev.MouseEvent.Type.MOVE
                }
                val id = 0
                val x = e.x
                val y = e.y
                val button = MouseButton[e.button - 1]
                dispatchSimpleMouseEvent(ev, id, x, y, button, simulateClickOnUp = false)
            }
        }

        fun handleKeyEvent(e: KeyEvent) {
            queue {
                val ev = when (e.id) {
                    KeyEvent.KEY_TYPED -> com.soywiz.korev.KeyEvent.Type.TYPE
                    KeyEvent.KEY_PRESSED -> com.soywiz.korev.KeyEvent.Type.DOWN
                    KeyEvent.KEY_RELEASED -> com.soywiz.korev.KeyEvent.Type.UP
                    else -> com.soywiz.korev.KeyEvent.Type.TYPE
                }
                val id = 0
                val char = e.keyChar
                val keyCode = e.keyCode
                val key = AwtKeyMap[e.keyCode] ?: Key.UNKNOWN
                dispatchKeyEvent(ev, id, char, key, keyCode)
            }

        }

        frame.contentPane.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
        })

        frame.contentPane.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseEntered(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseClicked(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseExited(e: MouseEvent) = handleMouseEvent(e)
            override fun mousePressed(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseWheelMoved(e: MouseWheelEvent) = handleMouseEvent(e)
        })

        frame.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) = handleKeyEvent(e)
            override fun keyPressed(e: KeyEvent) = handleKeyEvent(e)
            override fun keyReleased(e: KeyEvent) = handleKeyEvent(e)
        })

        queue {
            dispatchInitEvent()
            dispatchReshapeEvent()
        }
        EventQueue.invokeLater {
            frame.isVisible = true
        }

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

internal val AwtKeyMap = mapOf(
    KeyEvent.VK_ENTER to Key.ENTER,
    KeyEvent.VK_BACK_SPACE to Key.BACKSPACE,
    KeyEvent.VK_TAB to Key.TAB,
    KeyEvent.VK_CANCEL to Key.CANCEL,
    KeyEvent.VK_CLEAR to Key.CLEAR,
    KeyEvent.VK_SHIFT to Key.LEFT_SHIFT,
    KeyEvent.VK_CONTROL to Key.LEFT_CONTROL,
    KeyEvent.VK_ALT to Key.LEFT_ALT,
    KeyEvent.VK_PAUSE to Key.PAUSE,
    KeyEvent.VK_CAPS_LOCK to Key.CAPS_LOCK,
    KeyEvent.VK_ESCAPE to Key.ESCAPE,
    KeyEvent.VK_SPACE to Key.SPACE,
    KeyEvent.VK_PAGE_UP to Key.PAGE_UP,
    KeyEvent.VK_PAGE_DOWN to Key.PAGE_DOWN,
    KeyEvent.VK_END to Key.END,
    KeyEvent.VK_HOME to Key.HOME,
    KeyEvent.VK_LEFT to Key.LEFT,
    KeyEvent.VK_UP to Key.UP,
    KeyEvent.VK_RIGHT to Key.RIGHT,
    KeyEvent.VK_DOWN to Key.DOWN,
    KeyEvent.VK_COMMA to Key.COMMA,
    KeyEvent.VK_MINUS to Key.MINUS,
    KeyEvent.VK_PERIOD to Key.PERIOD,
    KeyEvent.VK_SLASH to Key.SLASH,
    KeyEvent.VK_0 to Key.N0,
    KeyEvent.VK_1 to Key.N1,
    KeyEvent.VK_2 to Key.N2,
    KeyEvent.VK_3 to Key.N3,
    KeyEvent.VK_4 to Key.N4,
    KeyEvent.VK_5 to Key.N5,
    KeyEvent.VK_6 to Key.N6,
    KeyEvent.VK_7 to Key.N7,
    KeyEvent.VK_8 to Key.N8,
    KeyEvent.VK_9 to Key.N9,
    KeyEvent.VK_SEMICOLON to Key.SEMICOLON,
    KeyEvent.VK_EQUALS to Key.EQUAL,
    KeyEvent.VK_A to Key.A,
    KeyEvent.VK_B to Key.B,
    KeyEvent.VK_C to Key.C,
    KeyEvent.VK_D to Key.D,
    KeyEvent.VK_E to Key.E,
    KeyEvent.VK_F to Key.F,
    KeyEvent.VK_G to Key.G,
    KeyEvent.VK_H to Key.H,
    KeyEvent.VK_I to Key.I,
    KeyEvent.VK_J to Key.J,
    KeyEvent.VK_K to Key.K,
    KeyEvent.VK_L to Key.L,
    KeyEvent.VK_M to Key.M,
    KeyEvent.VK_N to Key.N,
    KeyEvent.VK_O to Key.O,
    KeyEvent.VK_P to Key.P,
    KeyEvent.VK_Q to Key.Q,
    KeyEvent.VK_R to Key.R,
    KeyEvent.VK_S to Key.S,
    KeyEvent.VK_T to Key.T,
    KeyEvent.VK_U to Key.U,
    KeyEvent.VK_V to Key.V,
    KeyEvent.VK_W to Key.W,
    KeyEvent.VK_X to Key.X,
    KeyEvent.VK_Y to Key.Y,
    KeyEvent.VK_Z to Key.Z,
    KeyEvent.VK_OPEN_BRACKET to Key.OPEN_BRACKET,
    KeyEvent.VK_BACK_SLASH to Key.BACKSLASH,
    KeyEvent.VK_CLOSE_BRACKET to Key.CLOSE_BRACKET,
    KeyEvent.VK_NUMPAD0 to Key.NUMPAD0,
    KeyEvent.VK_NUMPAD1 to Key.NUMPAD1,
    KeyEvent.VK_NUMPAD2 to Key.NUMPAD2,
    KeyEvent.VK_NUMPAD3 to Key.NUMPAD3,
    KeyEvent.VK_NUMPAD4 to Key.NUMPAD4,
    KeyEvent.VK_NUMPAD5 to Key.NUMPAD5,
    KeyEvent.VK_NUMPAD6 to Key.NUMPAD6,
    KeyEvent.VK_NUMPAD7 to Key.NUMPAD7,
    KeyEvent.VK_NUMPAD8 to Key.NUMPAD8,
    KeyEvent.VK_NUMPAD9 to Key.NUMPAD9,
    KeyEvent.VK_MULTIPLY to Key.KP_MULTIPLY,
    KeyEvent.VK_ADD to Key.KP_ADD,
    KeyEvent.VK_SEPARATER to Key.KP_SEPARATOR,
    KeyEvent.VK_SUBTRACT to Key.KP_SUBTRACT,
    KeyEvent.VK_DECIMAL to Key.KP_DECIMAL,
    KeyEvent.VK_DIVIDE to Key.KP_DIVIDE,
    KeyEvent.VK_DELETE to Key.DELETE,
    KeyEvent.VK_NUM_LOCK to Key.NUM_LOCK,
    KeyEvent.VK_SCROLL_LOCK to Key.SCROLL_LOCK,
    KeyEvent.VK_F1 to Key.F1,
    KeyEvent.VK_F2 to Key.F2,
    KeyEvent.VK_F3 to Key.F3,
    KeyEvent.VK_F4 to Key.F4,
    KeyEvent.VK_F5 to Key.F5,
    KeyEvent.VK_F6 to Key.F6,
    KeyEvent.VK_F7 to Key.F7,
    KeyEvent.VK_F8 to Key.F8,
    KeyEvent.VK_F9 to Key.F9,
    KeyEvent.VK_F10 to Key.F10,
    KeyEvent.VK_F11 to Key.F11,
    KeyEvent.VK_F12 to Key.F12,
    KeyEvent.VK_F13 to Key.F13,
    KeyEvent.VK_F14 to Key.F14,
    KeyEvent.VK_F15 to Key.F15,
    KeyEvent.VK_F16 to Key.F16,
    KeyEvent.VK_F17 to Key.F17,
    KeyEvent.VK_F18 to Key.F18,
    KeyEvent.VK_F19 to Key.F19,
    KeyEvent.VK_F20 to Key.F20,
    KeyEvent.VK_F21 to Key.F21,
    KeyEvent.VK_F22 to Key.F22,
    KeyEvent.VK_F23 to Key.F23,
    KeyEvent.VK_F24 to Key.F24,
    KeyEvent.VK_PRINTSCREEN to Key.PRINT_SCREEN,
    KeyEvent.VK_INSERT to Key.INSERT,
    KeyEvent.VK_HELP to Key.HELP,
    KeyEvent.VK_META to Key.META,
    KeyEvent.VK_BACK_QUOTE to Key.BACKQUOTE,
    KeyEvent.VK_QUOTE to Key.QUOTE,
    KeyEvent.VK_KP_UP to Key.KP_UP,
    KeyEvent.VK_KP_DOWN to Key.KP_DOWN,
    KeyEvent.VK_KP_LEFT to Key.KP_LEFT,
    KeyEvent.VK_KP_RIGHT to Key.KP_RIGHT,
    KeyEvent.VK_UNDEFINED to Key.UNDEFINED
)
