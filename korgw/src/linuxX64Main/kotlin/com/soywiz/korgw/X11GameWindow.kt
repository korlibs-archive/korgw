package com.soywiz.korgw

import GL.*
import com.soywiz.kgl.*
import kotlinx.cinterop.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korag.*
import com.soywiz.korim.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.net.URL
import com.soywiz.korio.file.*
import com.soywiz.korio.async.*
import com.soywiz.korio.*
import platform.posix.*

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, override val gl: KmlGl = com.soywiz.kgl.KmlGlNative()) : AGOpengl() {
    override val gles: Boolean = true
    override val nativeComponent: Any = window
}

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: CPointer<Display>?, val w: Window, val doubleBuffered: Boolean = true) {
    val vi = memScoped {
        val values = intArrayOf(
            GLX_RGBA,
            GLX_DEPTH_SIZE, 24,
            *(if (doubleBuffered) intArrayOf(GLX_DOUBLEBUFFER) else intArrayOf()),
            None.toInt()
        )
        values.usePinned {
            glXChooseVisual(d, 0, it.addressOf(0))
        }
    }
    val glc = glXCreateContext(d, vi, null, 1)

    init {
        println("VI: $vi, d: $d, w: $w, glc: $glc")
        makeCurrent()
        println("GL_VENDOR: " + glGetString(GL.GL_VENDOR))
        println("GL_VERSION: " + glGetString(GL.GL_VERSION))
    }

    fun makeCurrent() {
        glXMakeCurrent(d, w, glc)
    }

    fun swapBuffers() {
        glXSwapBuffers(d, w)
    }
}

class X11GameWindow : GameWindow() {
    override val ag: X11Ag by lazy { X11Ag(this) }
    override var fps: Int
        get() = super.fps
        set(value) {}
    override var title: String = "Korgw"
        set(value) {
            field = value
            if (w != null) {
                XStoreName(d, w, title)
                XSetIconName(d, w, title)
            }
        }
    override var width: Int = 200; private set
    override var height: Int = 200; private set
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = super.fullscreen
        set(value) {}
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override suspend fun browse(url: URL) {
        platform.posix.system("xdg-open ${url.fullUrl}")
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

    var d: CPointer<Display>? = null
    var root: Window = 0UL
    var w: Window = 0UL
    var s: Int = 0

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }

        d = XOpenDisplay(null) ?: error("Can't open main display")
        s = XDefaultScreen(d)
        root = XDefaultRootWindow(d)

        //val cmap = XCreateColormap(d, root, vi->visual, AllocNone);
        val screenWidth = XDisplayWidth(d, s)
        val screenHeight = XDisplayHeight(d, s)

        val gameWindow = this@X11GameWindow

        val winX = screenWidth / 2 - width / 2
        val winY = screenHeight / 2 - height / 2

        println("screenWidth: $screenWidth, screenHeight: $screenHeight, winX=$winX, winY=$winY")

        w = XCreateSimpleWindow(
            d, XRootWindow(d, s),
            winX, winY,
            width.convert(), height.convert(),
            1,
            XBlackPixel(d, s), XWhitePixel(d, s)
        )

        val eventMask = (ExposureMask or StructureNotifyMask or KeyPressMask or PointerMotionMask or ButtonPressMask or ButtonReleaseMask)

        XSelectInput(d, w, eventMask)
        XStoreName(d, w, title)
        XSetIconName(d, w, title)
        XMapWindow(d, w)

        val ctx = X11OpenglContext(d!!, w)
        ctx.makeCurrent()

        val keysim = XStringToKeysym("A")
        println("keysim: $keysim")

        var frame = 0

        fun render() {
            ctx.makeCurrent()
            glViewport(0, 0, width, height)
            glClearColor(.3f, .6f, (frame % 60).toFloat() / 60, 1f)
            glClear(GL.GL_COLOR_BUFFER_BIT)
            frame()
            ctx.swapBuffers()
        }

        var running = true

        dispatchInitEvent()

        memScoped {
            val e = alloc<XEvent>()
            loop@ while (running) {
                //XNextEvent(d, e)
                while (XCheckWindowEvent(d, w, eventMask, e.ptr) != 0) {
                    when (e.type) {
                        Expose -> {
                            //println("EXPOSE")
                            render()
                            //XFillRectangle(d, w, XDefaultGC(d, s), 20, 20, 10, 10)
                            //XDrawString(d, w, DefaultGC(d, s), 10, 50, msg, strlen(msg));

                        }
                        DestroyNotify -> {
                            running = false
                        }
                        ConfigureNotify -> {
                            val conf = e.xconfigure
                            width = conf.width
                            height = conf.height
                            dispatchReshapeEvent(conf.x, conf.y, conf.width, conf.height)
                            //println("RESIZED! ${conf.width} ${conf.height}")
                        }
                        KeyPress, KeyRelease -> {
                            val pressing = e.type == KeyPress
                            val ev =
                                if (pressing) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
                            val key = e.xkey
                            val keySym = XLookupKeysym(e.xkey.ptr, 0)
                            val keyCode = key.keycode
                            val kkey = XK_KeyMap[keySym.toInt()] ?: Key.UNKNOWN
                            //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$keySym")
                            dispatchKeyEvent(ev, 0, keyCode.toInt().toChar(), kkey, keyCode.toInt())
                            //break@loop
                        }
                        MotionNotify, ButtonPress, ButtonRelease -> {
                            val mot = e.xmotion
                            val but = e.xbutton
                            val scrollDeltaX = 0.0
                            val scrollDeltaY = 0.0
                            val scrollDeltaZ = 0.0
                            val isShiftDown = false
                            val isCtrlDown = false
                            val isAltDown = false
                            val isMetaDown = false
                            val ev = when (e.type) {
                                MotionNotify -> MouseEvent.Type.MOVE
                                ButtonPress -> MouseEvent.Type.DOWN
                                ButtonRelease -> MouseEvent.Type.UP
                                else -> MouseEvent.Type.MOVE
                            }
                            val button = when (but.button.toInt()) {
                                1 -> MouseButton.LEFT
                                2 -> MouseButton.MIDDLE
                                3 -> MouseButton.RIGHT
                                4 -> MouseButton.BUTTON4 // WHEEL_UP!
                                5 -> MouseButton.BUTTON5 // WHEEL_DOWN!
                                else -> MouseButton.BUTTON_UNKNOWN
                            }
                            //println(XMotionEvent().size())
                            //println(mot.size)
                            //println("MOUSE ${ev} ${mot.x} ${mot.y} ${mot.button}")

                            dispatchMouseEvent(
                                ev,
                                0,
                                mot.x,
                                mot.y,
                                button,
                                0,
                                scrollDeltaX,
                                scrollDeltaY,
                                scrollDeltaZ,
                                isShiftDown,
                                isCtrlDown,
                                isAltDown,
                                isMetaDown,
                                false
                            )
                            if (ev == MouseEvent.Type.UP) {
                                // @TODO: Check the down event to see if we should produce a click event!
                                dispatchMouseEvent(
                                    MouseEvent.Type.CLICK,
                                    0,
                                    mot.x,
                                    mot.y,
                                    button,
                                    0,
                                    scrollDeltaX,
                                    scrollDeltaY,
                                    scrollDeltaZ,
                                    isShiftDown,
                                    isCtrlDown,
                                    isAltDown,
                                    isMetaDown,
                                    false
                                )
                            }
                        }
                        else -> {
                            //println("OTHER EVENT ${e.type}")
                        }
                    }
                }
                usleep(16_000.convert())
                render()
                frame++
            }
        }
        //dispatchStopEvent()
        //frame()

        XDestroyWindow(d, w)
        XCloseDisplay(d)
    }
}

private val XK_KeyMap: Map<Int, Key> by lazy {
    mapOf(
        XK_space to Key.SPACE,
        XK_exclam to Key.UNKNOWN,
        XK_quotedbl to Key.UNKNOWN,
        XK_numbersign to Key.UNKNOWN,
        XK_dollar to Key.UNKNOWN,
        XK_percent to Key.UNKNOWN,
        XK_ampersand to Key.UNKNOWN,
        XK_apostrophe to Key.APOSTROPHE,
        XK_quoteright to Key.UNKNOWN,
        XK_parenleft to Key.UNKNOWN,
        XK_parenright to Key.UNKNOWN,
        XK_asterisk to Key.UNKNOWN,
        XK_plus to Key.KP_ADD,
        XK_comma to Key.COMMA,
        XK_minus to Key.MINUS,
        XK_period to Key.PERIOD,
        XK_slash to Key.SLASH,
        XK_0 to Key.N0,
        XK_1 to Key.N1,
        XK_2 to Key.N2,
        XK_3 to Key.N3,
        XK_4 to Key.N4,
        XK_5 to Key.N5,
        XK_6 to Key.N6,
        XK_7 to Key.N7,
        XK_8 to Key.N8,
        XK_9 to Key.N9,
        XK_colon to Key.UNKNOWN,
        XK_semicolon to Key.SEMICOLON,
        XK_less to Key.UNKNOWN,
        XK_equal to Key.EQUAL,
        XK_greater to Key.UNKNOWN,
        XK_question to Key.UNKNOWN,
        XK_at to Key.UNKNOWN,
        XK_A to Key.A,
        XK_B to Key.B,
        XK_C to Key.C,
        XK_D to Key.D,
        XK_E to Key.E,
        XK_F to Key.F,
        XK_G to Key.G,
        XK_H to Key.H,
        XK_I to Key.I,
        XK_J to Key.J,
        XK_K to Key.K,
        XK_L to Key.L,
        XK_M to Key.M,
        XK_N to Key.N,
        XK_O to Key.O,
        XK_P to Key.P,
        XK_Q to Key.Q,
        XK_R to Key.R,
        XK_S to Key.S,
        XK_T to Key.T,
        XK_U to Key.U,
        XK_V to Key.V,
        XK_W to Key.W,
        XK_X to Key.X,
        XK_Y to Key.Y,
        XK_Z to Key.Z,
        XK_bracketleft to Key.UNKNOWN,
        XK_backslash to Key.BACKSLASH,
        XK_bracketright to Key.UNKNOWN,
        XK_asciicircum to Key.UNKNOWN,
        XK_underscore to Key.UNKNOWN,
        XK_grave to Key.UNKNOWN,
        XK_quoteleft to Key.UNKNOWN,
        XK_a to Key.A,
        XK_b to Key.B,
        XK_c to Key.C,
        XK_d to Key.D,
        XK_e to Key.E,
        XK_f to Key.F,
        XK_g to Key.G,
        XK_h to Key.H,
        XK_i to Key.I,
        XK_j to Key.J,
        XK_k to Key.K,
        XK_l to Key.L,
        XK_m to Key.M,
        XK_n to Key.N,
        XK_o to Key.O,
        XK_p to Key.P,
        XK_q to Key.Q,
        XK_r to Key.R,
        XK_s to Key.S,
        XK_t to Key.T,
        XK_u to Key.U,
        XK_v to Key.V,
        XK_w to Key.W,
        XK_x to Key.X,
        XK_y to Key.Y,
        XK_z to Key.Z,
        XK_BackSpace to Key.BACKSPACE,
        XK_Tab to Key.TAB,
        XK_Linefeed to Key.UNKNOWN,
        XK_Clear to Key.CLEAR,
        XK_Return to Key.RETURN,
        XK_Pause to Key.PAUSE,
        XK_Scroll_Lock to Key.SCROLL_LOCK,
        XK_Sys_Req to Key.UNKNOWN,
        XK_Escape to Key.ESCAPE,
        XK_Delete to Key.DELETE,
        XK_Home to Key.HOME,
        XK_Left to Key.LEFT,
        XK_Up to Key.UP,
        XK_Right to Key.RIGHT,
        XK_Down to Key.DOWN,
        XK_Prior to Key.UNKNOWN,
        XK_Page_Up to Key.PAGE_UP,
        XK_Next to Key.UNKNOWN,
        XK_Page_Down to Key.PAGE_DOWN,
        XK_End to Key.END,
        XK_Begin to Key.UNKNOWN,
        XK_Select to Key.UNKNOWN,
        XK_Print to Key.PRINT_SCREEN,
        XK_Execute to Key.UNKNOWN,
        XK_Insert to Key.INSERT,
        XK_Undo to Key.UNKNOWN,
        XK_Redo to Key.UNKNOWN,
        XK_Menu to Key.MENU,
        XK_Find to Key.UNKNOWN,
        XK_Cancel to Key.CANCEL,
        XK_Help to Key.HELP,
        XK_Break to Key.UNKNOWN,
        XK_Mode_switch to Key.UNKNOWN,
        XK_script_switch to Key.UNKNOWN,
        XK_Num_Lock to Key.NUM_LOCK,
        XK_KP_Space to Key.UNKNOWN,
        XK_KP_Tab to Key.UNKNOWN,
        XK_KP_Enter to Key.KP_ENTER,
        XK_KP_F1 to Key.F1,
        XK_KP_F2 to Key.F2,
        XK_KP_F3 to Key.F3,
        XK_KP_F4 to Key.F4,
        XK_KP_Home to Key.HOME,
        XK_KP_Left to Key.KP_LEFT,
        XK_KP_Up to Key.KP_UP,
        XK_KP_Right to Key.KP_RIGHT,
        XK_KP_Down to Key.KP_DOWN,
        XK_KP_Prior to Key.UNKNOWN,
        XK_KP_Page_Up to Key.UNKNOWN,
        XK_KP_Next to Key.UNKNOWN,
        XK_KP_Page_Down to Key.UNKNOWN,
        XK_KP_End to Key.END,
        XK_KP_Begin to Key.HOME,
        XK_KP_Insert to Key.INSERT,
        XK_KP_Delete to Key.DELETE,
        XK_KP_Equal to Key.KP_EQUAL,
        XK_KP_Multiply to Key.KP_MULTIPLY,
        XK_KP_Add to Key.KP_ADD,
        XK_KP_Separator to Key.KP_SEPARATOR,
        XK_KP_Subtract to Key.KP_SUBTRACT,
        XK_KP_Decimal to Key.KP_DECIMAL,
        XK_KP_Divide to Key.KP_DIVIDE,
        XK_KP_0 to Key.KP_0,
        XK_KP_1 to Key.KP_1,
        XK_KP_2 to Key.KP_2,
        XK_KP_3 to Key.KP_3,
        XK_KP_4 to Key.KP_4,
        XK_KP_5 to Key.KP_5,
        XK_KP_6 to Key.KP_6,
        XK_KP_7 to Key.KP_7,
        XK_KP_8 to Key.KP_8,
        XK_KP_9 to Key.KP_9,
        XK_F1 to Key.F1,
        XK_F2 to Key.F2,
        XK_F3 to Key.F3,
        XK_F4 to Key.F4,
        XK_F5 to Key.F5,
        XK_F6 to Key.F6,
        XK_F7 to Key.F7,
        XK_F8 to Key.F8,
        XK_F9 to Key.F9,
        XK_F10 to Key.F10,
        XK_F11 to Key.F11,
        XK_F12 to Key.F12,
        XK_F13 to Key.F13,
        XK_F14 to Key.F14,
        XK_F15 to Key.F15,
        XK_F16 to Key.F16,
        XK_F17 to Key.F17,
        XK_F18 to Key.F18,
        XK_F19 to Key.F19,
        XK_F20 to Key.F20,
        XK_F21 to Key.F21,
        XK_F22 to Key.F22,
        XK_F23 to Key.F23,
        XK_F24 to Key.F24,
        XK_F25 to Key.F25,
        XK_F26 to Key.UNKNOWN,
        XK_F27 to Key.UNKNOWN,
        XK_F28 to Key.UNKNOWN,
        XK_F29 to Key.UNKNOWN,
        XK_F30 to Key.UNKNOWN,
        XK_F31 to Key.UNKNOWN,
        XK_F32 to Key.UNKNOWN,
        XK_F33 to Key.UNKNOWN,
        XK_F34 to Key.UNKNOWN,
        XK_F35 to Key.UNKNOWN,

        XK_R1 to Key.UNKNOWN,
        XK_R2 to Key.UNKNOWN,
        XK_R3 to Key.UNKNOWN,
        XK_R4 to Key.UNKNOWN,
        XK_R5 to Key.UNKNOWN,
        XK_R6 to Key.UNKNOWN,
        XK_R7 to Key.UNKNOWN,
        XK_R8 to Key.UNKNOWN,
        XK_R9 to Key.UNKNOWN,
        XK_R10 to Key.UNKNOWN,
        XK_R11 to Key.UNKNOWN,
        XK_R12 to Key.UNKNOWN,
        XK_R13 to Key.UNKNOWN,
        XK_R14 to Key.UNKNOWN,
        XK_R15 to Key.UNKNOWN,

        XK_L1 to Key.UNKNOWN,
        XK_L2 to Key.UNKNOWN,
        XK_L3 to Key.UNKNOWN,
        XK_L4 to Key.UNKNOWN,
        XK_L5 to Key.UNKNOWN,
        XK_L6 to Key.UNKNOWN,
        XK_L7 to Key.UNKNOWN,
        XK_L8 to Key.UNKNOWN,
        XK_L9 to Key.UNKNOWN,
        XK_L10 to Key.UNKNOWN,

        XK_Shift_L to Key.LEFT_SHIFT,
        XK_Shift_R to Key.RIGHT_SHIFT,
        XK_Control_L to Key.LEFT_CONTROL,
        XK_Control_R to Key.RIGHT_CONTROL,
        XK_Caps_Lock to Key.CAPS_LOCK,
        XK_Shift_Lock to Key.CAPS_LOCK,
        XK_Meta_L to Key.LEFT_SUPER,
        XK_Meta_R to Key.RIGHT_SUPER,
        XK_Alt_L to Key.LEFT_ALT,
        XK_Alt_R to Key.RIGHT_ALT,
        XK_Super_L to Key.LEFT_SUPER,
        XK_Super_R to Key.RIGHT_SUPER,
        XK_Hyper_L to Key.LEFT_SUPER,
        XK_Hyper_R to Key.RIGHT_SUPER
    )
}
