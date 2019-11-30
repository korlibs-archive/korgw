package com.soywiz.korgw.x11

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.platform.INativeGL
import com.soywiz.korgw.platform.KStructure
import com.soywiz.korgw.platform.NativeKgl
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.unix.X11.*

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, override val gl: KmlGl = X11KmlGl) : AGOpengl() {
    override val gles: Boolean = true
    override val nativeComponent: Any = window
}

object X11KmlGl : NativeKgl(X11GL)

interface X11GL : INativeGL, Library {
    fun glXChooseVisual(display: X11.Display, screen: Int, attribList: IntArray): XVisualInfo
    fun glXCreateContext(display: X11.Display, vis: XVisualInfo, shareList: GLXContext?, direct: Boolean): GLXContext
    fun glXMakeCurrent(display: X11.Display, drawable: X11.Window, ctx: GLXContext?): Boolean
    fun glXSwapBuffers(display: X11.Display, drawable: X11.Window)

    companion object : X11GL by Native.load("GL", X11GL::class.java)
}

// https://www.khronos.org/opengl/wiki/Tutorial:_OpenGL_3.0_Context_Creation_(GLX)
class X11OpenglContext(val d: Display?, val w: Window?, val doubleBuffered: Boolean = true) : BaseOpenglContext {
    val vi = X.glXChooseVisual(d, 0, intArrayOf(
        GLX_RGBA,
        GLX_DEPTH_SIZE, 24,
        *(if (doubleBuffered) intArrayOf(GLX_DOUBLEBUFFER) else intArrayOf()),
        None
    ))
    val glc = X.glXCreateContext(d, vi, null, true)

    init {
        println("VI: $vi, d: $d, w: $w, glc: $glc")
        makeCurrent()
        println("GL_VENDOR: " + X.glGetString(GL.GL_VENDOR))
        println("GL_VERSION: " + X.glGetString(GL.GL_VERSION))
    }

    override fun makeCurrent() {
        X.glXMakeCurrent(d, w, glc)
    }

    override fun swapBuffers() {
        X.glXSwapBuffers(d, w)
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
                X.XStoreName(d, w, title)
                X.XSetIconName(d, w, title)
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
        // system("open https://your.domain/uri");
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

    var d: Display? = null
    var root: Window? = null
    var w: Window? = null
    var s: Int = 0

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }

        X.apply {
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
                width, height,
                1,
                XBlackPixel(d, s), XWhitePixel(d, s)
            )

            val eventMask = NativeLong(
                (ExposureMask or StructureNotifyMask or KeyPressMask or PointerMotionMask or ButtonPressMask or ButtonReleaseMask)
                    .toLong()
            )

            XSelectInput(d, w, eventMask)
            XStoreName(d, w, title)
            XSetIconName(d, w, title)
            XMapWindow(d, w)

            val ctx = X11OpenglContext(d, w)
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

            loop@ while (running) {
                val e = XEvent()
                //XNextEvent(d, e)
                while (XCheckWindowEvent(d, w, eventMask, e)) {
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
                            val conf = XConfigureEvent(e.pointer)
                            width = conf.width
                            height = conf.height
                            dispatchReshapeEvent(conf.x, conf.y, conf.width, conf.height)
                            //println("RESIZED! ${conf.width} ${conf.height}")
                        }
                        KeyPress, KeyRelease -> {
                            val pressing = e.type == KeyPress
                            val ev =
                                if (pressing) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
                            val key = XKeyEvent(e.pointer)
                            val keySym = XLookupKeysym(e, 0)
                            val keyCode = key.keycode
                            val kkey = XK_KeyMap[keySym] ?: Key.UNKNOWN
                            //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$keySym")
                            dispatchKeyEvent(ev, 0, keyCode.toChar(), kkey, keyCode)
                            //break@loop
                        }
                        MotionNotify, ButtonPress, ButtonRelease -> {
                            val mot = MyXMotionEvent(e.pointer)
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
                            val button = when (mot.button) {
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

                            dispatchMouseEvent(ev, 0, mot.x, mot.y, button, 0, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, false)
                            if (ev == MouseEvent.Type.UP) {
                                // @TODO: Check the down event to see if we should produce a click event!
                                dispatchMouseEvent(MouseEvent.Type.CLICK, 0, mot.x, mot.y, button, 0, scrollDeltaX, scrollDeltaY, scrollDeltaZ, isShiftDown, isCtrlDown, isAltDown, isMetaDown, false)
                            }
                        }
                        else -> {
                            //println("OTHER EVENT ${e.type}")
                        }
                    }
                }
                Thread.sleep(16L)
                render()
                frame++
            }
            //dispatchStopEvent()
            //frame()

            XDestroyWindow(d, w)
            XCloseDisplay(d)
        }
    }
}

fun KStructure.display() = pointer<Display?>()
fun KStructure.window() = pointer<Window?>()

class XConfigureEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by display()
    var event by window()
    var window by window()
    var x by int()
    var y by int()
    var width by int()
    var height by int()
    var border_width by int()
    var above by window()
    var override_redirect by int()
}

class XKeyEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by pointer<Display>()
    var window by pointer<Window>()
    var root by pointer<Window>()
    var subwindow by pointer<Window>()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var keycode by int()
    var same_screen by int()
}

class MyXMotionEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by pointer<Display?>()
    var window by pointer<Window?>()
    var root by pointer<Window?>()
    var subwindow by pointer<Window?>()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var button by int()
    var same_screen by int()
}

object X : X11Impl by Native.load("X11", X11Impl::class.java),
    GL by Native.load("GL", GL::class.java)

interface X11Impl : X11 {
    fun XDefaultGC(display: Display?, scn: Int): GC?
    fun XBlackPixel(display: Display?, scn: Int): Int
    fun XWhitePixel(display: Display?, scn: Int): Int
    fun XStoreName(display: Display?, w: Window?, window_name: String)
    fun XSetIconName(display: Display?, w: Window?, window_name: String)
    fun XLookupKeysym(e: XEvent?, i: Int): Int
}

interface GL : Library {
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glClear(flags: Int)
    fun glGetString(id: Int): String
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glXChooseVisual(display: Display?, screen: Int, attribList: IntArray): XVisualInfo?
    fun glXCreateContext(display: Display?, vis: XVisualInfo?, shareList: GLXContext?, direct: Boolean): GLXContext?
    fun glXMakeCurrent(display: Display?, drawable: Window?, ctx: GLXContext?): Boolean
    fun glXSwapBuffers(display: Display?, drawable: Window?)

    companion object {
        const val GL_DEPTH_BUFFER_BIT = 0x00000100
        const val GL_STENCIL_BUFFER_BIT = 0x00000400
        const val GL_COLOR_BUFFER_BIT = 0x00004000

        const val WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091
        const val WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092

        const val GL_VENDOR = 0x1F00
        const val GL_RENDERER = 0x1F01
        const val GL_VERSION = 0x1F02
        const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
        const val GL_EXTENSIONS = 0x1F03
    }
}

typealias XVisualInfo = Pointer
typealias GLXContext = Pointer

private const val GLX_RGBA = 4
private const val GLX_DEPTH_SIZE = 12
private const val GLX_DOUBLEBUFFER = 5


private const val XK_space = 0x0020  /* U+0020 SPACE */
private const val XK_exclam = 0x0021  /* U+0021 EXCLAMATION MARK */
private const val XK_quotedbl = 0x0022  /* U+0022 QUOTATION MARK */
private const val XK_numbersign = 0x0023  /* U+0023 NUMBER SIGN */
private const val XK_dollar = 0x0024  /* U+0024 DOLLAR SIGN */
private const val XK_percent = 0x0025  /* U+0025 PERCENT SIGN */
private const val XK_ampersand = 0x0026  /* U+0026 AMPERSAND */
private const val XK_apostrophe = 0x0027  /* U+0027 APOSTROPHE */
private const val XK_quoteright = 0x0027  /* deprecated */
private const val XK_parenleft = 0x0028  /* U+0028 LEFT PARENTHESIS */
private const val XK_parenright = 0x0029  /* U+0029 RIGHT PARENTHESIS */
private const val XK_asterisk = 0x002a  /* U+002A ASTERISK */
private const val XK_plus = 0x002b  /* U+002B PLUS SIGN */
private const val XK_comma = 0x002c  /* U+002C COMMA */
private const val XK_minus = 0x002d  /* U+002D HYPHEN-MINUS */
private const val XK_period = 0x002e  /* U+002E FULL STOP */
private const val XK_slash = 0x002f  /* U+002F SOLIDUS */
private const val XK_0 = 0x0030  /* U+0030 DIGIT ZERO */
private const val XK_1 = 0x0031  /* U+0031 DIGIT ONE */
private const val XK_2 = 0x0032  /* U+0032 DIGIT TWO */
private const val XK_3 = 0x0033  /* U+0033 DIGIT THREE */
private const val XK_4 = 0x0034  /* U+0034 DIGIT FOUR */
private const val XK_5 = 0x0035  /* U+0035 DIGIT FIVE */
private const val XK_6 = 0x0036  /* U+0036 DIGIT SIX */
private const val XK_7 = 0x0037  /* U+0037 DIGIT SEVEN */
private const val XK_8 = 0x0038  /* U+0038 DIGIT EIGHT */
private const val XK_9 = 0x0039  /* U+0039 DIGIT NINE */
private const val XK_colon = 0x003a  /* U+003A COLON */
private const val XK_semicolon = 0x003b  /* U+003B SEMICOLON */
private const val XK_less = 0x003c  /* U+003C LESS-THAN SIGN */
private const val XK_equal = 0x003d  /* U+003D EQUALS SIGN */
private const val XK_greater = 0x003e  /* U+003E GREATER-THAN SIGN */
private const val XK_question = 0x003f  /* U+003F QUESTION MARK */
private const val XK_at = 0x0040  /* U+0040 COMMERCIAL AT */
private const val XK_A = 0x0041  /* U+0041 LATIN CAPITAL LETTER A */
private const val XK_B = 0x0042  /* U+0042 LATIN CAPITAL LETTER B */
private const val XK_C = 0x0043  /* U+0043 LATIN CAPITAL LETTER C */
private const val XK_D = 0x0044  /* U+0044 LATIN CAPITAL LETTER D */
private const val XK_E = 0x0045  /* U+0045 LATIN CAPITAL LETTER E */
private const val XK_F = 0x0046  /* U+0046 LATIN CAPITAL LETTER F */
private const val XK_G = 0x0047  /* U+0047 LATIN CAPITAL LETTER G */
private const val XK_H = 0x0048  /* U+0048 LATIN CAPITAL LETTER H */
private const val XK_I = 0x0049  /* U+0049 LATIN CAPITAL LETTER I */
private const val XK_J = 0x004a  /* U+004A LATIN CAPITAL LETTER J */
private const val XK_K = 0x004b  /* U+004B LATIN CAPITAL LETTER K */
private const val XK_L = 0x004c  /* U+004C LATIN CAPITAL LETTER L */
private const val XK_M = 0x004d  /* U+004D LATIN CAPITAL LETTER M */
private const val XK_N = 0x004e  /* U+004E LATIN CAPITAL LETTER N */
private const val XK_O = 0x004f  /* U+004F LATIN CAPITAL LETTER O */
private const val XK_P = 0x0050  /* U+0050 LATIN CAPITAL LETTER P */
private const val XK_Q = 0x0051  /* U+0051 LATIN CAPITAL LETTER Q */
private const val XK_R = 0x0052  /* U+0052 LATIN CAPITAL LETTER R */
private const val XK_S = 0x0053  /* U+0053 LATIN CAPITAL LETTER S */
private const val XK_T = 0x0054  /* U+0054 LATIN CAPITAL LETTER T */
private const val XK_U = 0x0055  /* U+0055 LATIN CAPITAL LETTER U */
private const val XK_V = 0x0056  /* U+0056 LATIN CAPITAL LETTER V */
private const val XK_W = 0x0057  /* U+0057 LATIN CAPITAL LETTER W */
private const val XK_X = 0x0058  /* U+0058 LATIN CAPITAL LETTER X */
private const val XK_Y = 0x0059  /* U+0059 LATIN CAPITAL LETTER Y */
private const val XK_Z = 0x005a  /* U+005A LATIN CAPITAL LETTER Z */
private const val XK_bracketleft = 0x005b  /* U+005B LEFT SQUARE BRACKET */
private const val XK_backslash = 0x005c  /* U+005C REVERSE SOLIDUS */
private const val XK_bracketright = 0x005d  /* U+005D RIGHT SQUARE BRACKET */
private const val XK_asciicircum = 0x005e  /* U+005E CIRCUMFLEX ACCENT */
private const val XK_underscore = 0x005f  /* U+005F LOW LINE */
private const val XK_grave = 0x0060  /* U+0060 GRAVE ACCENT */
private const val XK_quoteleft = 0x0060  /* deprecated */
private const val XK_a = 0x0061  /* U+0061 LATIN SMALL LETTER A */
private const val XK_b = 0x0062  /* U+0062 LATIN SMALL LETTER B */
private const val XK_c = 0x0063  /* U+0063 LATIN SMALL LETTER C */
private const val XK_d = 0x0064  /* U+0064 LATIN SMALL LETTER D */
private const val XK_e = 0x0065  /* U+0065 LATIN SMALL LETTER E */
private const val XK_f = 0x0066  /* U+0066 LATIN SMALL LETTER F */
private const val XK_g = 0x0067  /* U+0067 LATIN SMALL LETTER G */
private const val XK_h = 0x0068  /* U+0068 LATIN SMALL LETTER H */
private const val XK_i = 0x0069  /* U+0069 LATIN SMALL LETTER I */
private const val XK_j = 0x006a  /* U+006A LATIN SMALL LETTER J */
private const val XK_k = 0x006b  /* U+006B LATIN SMALL LETTER K */
private const val XK_l = 0x006c  /* U+006C LATIN SMALL LETTER L */
private const val XK_m = 0x006d  /* U+006D LATIN SMALL LETTER M */
private const val XK_n = 0x006e  /* U+006E LATIN SMALL LETTER N */
private const val XK_o = 0x006f  /* U+006F LATIN SMALL LETTER O */
private const val XK_p = 0x0070  /* U+0070 LATIN SMALL LETTER P */
private const val XK_q = 0x0071  /* U+0071 LATIN SMALL LETTER Q */
private const val XK_r = 0x0072  /* U+0072 LATIN SMALL LETTER R */
private const val XK_s = 0x0073  /* U+0073 LATIN SMALL LETTER S */
private const val XK_t = 0x0074  /* U+0074 LATIN SMALL LETTER T */
private const val XK_u = 0x0075  /* U+0075 LATIN SMALL LETTER U */
private const val XK_v = 0x0076  /* U+0076 LATIN SMALL LETTER V */
private const val XK_w = 0x0077  /* U+0077 LATIN SMALL LETTER W */
private const val XK_x = 0x0078  /* U+0078 LATIN SMALL LETTER X */
private const val XK_y = 0x0079  /* U+0079 LATIN SMALL LETTER Y */
private const val XK_z = 0x007a  /* U+007A LATIN SMALL LETTER Z */
private const val XK_braceleft = 0x007b  /* U+007B LEFT CURLY BRACKET */
private const val XK_bar = 0x007c  /* U+007C VERTICAL LINE */
private const val XK_braceright = 0x007d  /* U+007D RIGHT CURLY BRACKET */
private const val XK_asciitilde = 0x007e  /* U+007E TILDE */

private const val XK_leftarrow = 0x08fb  /* U+2190 LEFTWARDS ARROW */
private const val XK_uparrow = 0x08fc  /* U+2191 UPWARDS ARROW */
private const val XK_rightarrow = 0x08fd  /* U+2192 RIGHTWARDS ARROW */
private const val XK_downarrow = 0x08fe  /* U+2193 DOWNWARDS ARROW */
private const val XK_BackSpace = 0xff08  /* Back space, back char */
private const val XK_Tab = 0xff09
private const val XK_Linefeed = 0xff0a  /* Linefeed, LF */
private const val XK_Clear = 0xff0b
private const val XK_Return = 0xff0d  /* Return, enter */
private const val XK_Pause = 0xff13  /* Pause, hold */
private const val XK_Scroll_Lock = 0xff14
private const val XK_Sys_Req = 0xff15
private const val XK_Escape = 0xff1b
private const val XK_Delete = 0xffff  /* Delete, rubout */

private const val XK_Home                          = 0xff50
private const val XK_Left                          = 0xff51  /* Move left, left arrow */
private const val XK_Up                            = 0xff52  /* Move up, up arrow */
private const val XK_Right                         = 0xff53  /* Move right, right arrow */
private const val XK_Down                          = 0xff54  /* Move down, down arrow */
private const val XK_Prior                         = 0xff55  /* Prior, previous */
private const val XK_Page_Up                       = 0xff55
private const val XK_Next                          = 0xff56  /* Next */
private const val XK_Page_Down                     = 0xff56
private const val XK_End                           = 0xff57  /* EOL */
private const val XK_Begin                         = 0xff58  /* BOL */
private const val XK_Select                        = 0xff60  /* Select, mark */
private const val XK_Print                         = 0xff61
private const val XK_Execute                       = 0xff62  /* Execute, run, do */
private const val XK_Insert                        = 0xff63  /* Insert, insert here */
private const val XK_Undo                          = 0xff65
private const val XK_Redo                          = 0xff66  /* Redo, again */
private const val XK_Menu                          = 0xff67
private const val XK_Find                          = 0xff68  /* Find, search */
private const val XK_Cancel                        = 0xff69  /* Cancel, stop, abort, exit */
private const val XK_Help                          = 0xff6a  /* Help */
private const val XK_Break                         = 0xff6b
private const val XK_Mode_switch                   = 0xff7e  /* Character set switch */
private const val XK_script_switch                 = 0xff7e  /* Alias for mode_switch */
private const val XK_Num_Lock                      = 0xff7f
private const val XK_KP_Space                      = 0xff80  /* Space */
private const val XK_KP_Tab                        = 0xff89
private const val XK_KP_Enter                      = 0xff8d  /* Enter */
private const val XK_KP_F1                         = 0xff91  /* PF1, KP_A, ... */
private const val XK_KP_F2                         = 0xff92
private const val XK_KP_F3                         = 0xff93
private const val XK_KP_F4                         = 0xff94
private const val XK_KP_Home                       = 0xff95
private const val XK_KP_Left                       = 0xff96
private const val XK_KP_Up                         = 0xff97
private const val XK_KP_Right                      = 0xff98
private const val XK_KP_Down                       = 0xff99
private const val XK_KP_Prior                      = 0xff9a
private const val XK_KP_Page_Up                    = 0xff9a
private const val XK_KP_Next                       = 0xff9b
private const val XK_KP_Page_Down                  = 0xff9b
private const val XK_KP_End                        = 0xff9c
private const val XK_KP_Begin                      = 0xff9d
private const val XK_KP_Insert                     = 0xff9e
private const val XK_KP_Delete                     = 0xff9f
private const val XK_KP_Equal                      = 0xffbd  /* Equals */
private const val XK_KP_Multiply                   = 0xffaa
private const val XK_KP_Add                        = 0xffab
private const val XK_KP_Separator                  = 0xffac  /* Separator, often comma */
private const val XK_KP_Subtract                   = 0xffad
private const val XK_KP_Decimal                    = 0xffae
private const val XK_KP_Divide                     = 0xffaf
private const val XK_KP_0                          = 0xffb0
private const val XK_KP_1                          = 0xffb1
private const val XK_KP_2                          = 0xffb2
private const val XK_KP_3                          = 0xffb3
private const val XK_KP_4                          = 0xffb4
private const val XK_KP_5                          = 0xffb5
private const val XK_KP_6                          = 0xffb6
private const val XK_KP_7                          = 0xffb7
private const val XK_KP_8                          = 0xffb8
private const val XK_KP_9                          = 0xffb9
private const val XK_F1                            = 0xffbe
private const val XK_F2                            = 0xffbf
private const val XK_F3                            = 0xffc0
private const val XK_F4                            = 0xffc1
private const val XK_F5                            = 0xffc2
private const val XK_F6                            = 0xffc3
private const val XK_F7                            = 0xffc4
private const val XK_F8                            = 0xffc5
private const val XK_F9                            = 0xffc6
private const val XK_F10                           = 0xffc7
private const val XK_F11                           = 0xffc8
private const val XK_L1                            = 0xffc8
private const val XK_F12                           = 0xffc9
private const val XK_L2                            = 0xffc9
private const val XK_F13                           = 0xffca
private const val XK_L3                            = 0xffca
private const val XK_F14                           = 0xffcb
private const val XK_L4                            = 0xffcb
private const val XK_F15                           = 0xffcc
private const val XK_L5                            = 0xffcc
private const val XK_F16                           = 0xffcd
private const val XK_L6                            = 0xffcd
private const val XK_F17                           = 0xffce
private const val XK_L7                            = 0xffce
private const val XK_F18                           = 0xffcf
private const val XK_L8                            = 0xffcf
private const val XK_F19                           = 0xffd0
private const val XK_L9                            = 0xffd0
private const val XK_F20                           = 0xffd1
private const val XK_L10                           = 0xffd1
private const val XK_F21                           = 0xffd2
private const val XK_R1                            = 0xffd2
private const val XK_F22                           = 0xffd3
private const val XK_R2                            = 0xffd3
private const val XK_F23                           = 0xffd4
private const val XK_R3                            = 0xffd4
private const val XK_F24                           = 0xffd5
private const val XK_R4                            = 0xffd5
private const val XK_F25                           = 0xffd6
private const val XK_R5                            = 0xffd6
private const val XK_F26                           = 0xffd7
private const val XK_R6                            = 0xffd7
private const val XK_F27                           = 0xffd8
private const val XK_R7                            = 0xffd8
private const val XK_F28                           = 0xffd9
private const val XK_R8                            = 0xffd9
private const val XK_F29                           = 0xffda
private const val XK_R9                            = 0xffda
private const val XK_F30                           = 0xffdb
private const val XK_R10                           = 0xffdb
private const val XK_F31                           = 0xffdc
private const val XK_R11                           = 0xffdc
private const val XK_F32                           = 0xffdd
private const val XK_R12                           = 0xffdd
private const val XK_F33                           = 0xffde
private const val XK_R13                           = 0xffde
private const val XK_F34                           = 0xffdf
private const val XK_R14                           = 0xffdf
private const val XK_F35                           = 0xffe0
private const val XK_R15                           = 0xffe0
private const val XK_Shift_L                       = 0xffe1  /* Left shift */
private const val XK_Shift_R                       = 0xffe2  /* Right shift */
private const val XK_Control_L                     = 0xffe3  /* Left control */
private const val XK_Control_R                     = 0xffe4  /* Right control */
private const val XK_Caps_Lock                     = 0xffe5  /* Caps lock */
private const val XK_Shift_Lock                    = 0xffe6  /* Shift lock */
private const val XK_Meta_L                        = 0xffe7  /* Left meta */
private const val XK_Meta_R                        = 0xffe8  /* Right meta */
private const val XK_Alt_L                         = 0xffe9  /* Left alt */
private const val XK_Alt_R                         = 0xffea  /* Right alt */
private const val XK_Super_L                       = 0xffeb  /* Left super */
private const val XK_Super_R                       = 0xffec  /* Right super */
private const val XK_Hyper_L                       = 0xffed  /* Left hyper */
private const val XK_Hyper_R                       = 0xffee  /* Right hyper */


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
        XK_leftarrow to Key.LEFT,
        XK_uparrow to Key.UP,
        XK_rightarrow to Key.RIGHT,
        XK_downarrow to Key.DOWN,
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
