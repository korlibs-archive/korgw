package com.soywiz.korgw.x11

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11.*

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, override val gl: KmlGl = X11KmlGl) : AGOpengl() {
    override val gles: Boolean = true
    override val nativeComponent: Any = window
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
            } else {
                println("NO WINDOW!")
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
                (ExposureMask
                    or StructureNotifyMask
                    or EnterWindowMask
                    or LeaveWindowMask
                    or KeyPressMask
                    or KeyReleaseMask
                    or PointerMotionMask
                    or ButtonPressMask
                    or ButtonReleaseMask
                    or ButtonMotionMask
                )
                    .toLong()
            )

            XSelectInput(d, w, eventMask)
            XStoreName(d, w, title)
            XSetIconName(d, w, title)
            XMapWindow(d, w)

            val doubleBuffered = false
            //val doubleBuffered = true
            val ctx = X11OpenglContext(d, w, doubleBuffered = doubleBuffered)
            ctx.makeCurrent()

            var running = true

            val wmDeleteMessage = XInternAtom(d, "WM_DELETE_WINDOW", false)
            if (wmDeleteMessage != null) {
                XSetWMProtocols(d, w, arrayOf(wmDeleteMessage), 1)
            }

            dispatchInitEvent()

            var lastRenderTime = System.nanoTime()
            fun elapsedSinceLastRenderTime(): Long = System.nanoTime() - lastRenderTime
            fun render(doUpdate: Boolean) {
                lastRenderTime = System.nanoTime()
                ctx.makeCurrent()
                glViewport(0, 0, width, height)
                glClearColor(.3f, .6f, .3f, 1f)
                glClear(GL.GL_COLOR_BUFFER_BIT)
                frame(doUpdate)
                ctx.swapBuffers()
            }

            loop@ while (running) {
                val e = XEvent()
                if (XPending(d) == 0) {
                    if (elapsedSinceLastRenderTime() >= timePerFrame.nanoseconds.toLong()) {
                        render(doUpdate = true)
                    }
                    //println("No events!")
                    //Thread.sleep(0L, 100_000)
                    Thread.sleep(1L)
                    continue
                }
                XNextEvent(d, e)
                when (e.type) {
                    Expose -> if (e.xexpose.count == 0) render(doUpdate = false)
                    ClientMessage, DestroyNotify -> running = false
                    ConfigureNotify -> {
                        val conf = XConfigureEvent(e.pointer)
                        width = conf.width
                        height = conf.height
                        dispatchReshapeEvent(conf.x, conf.y, conf.width, conf.height)
                        if (!doubleBuffered) {
                            render(doUpdate = false)
                        }
                        //println("RESIZED! ${conf.width} ${conf.height}")
                    }
                    KeyPress, KeyRelease -> {
                        val pressing = e.type == KeyPress
                        val ev =
                            if (pressing) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
                        val keyCode = XKeyEvent(e.pointer).keycode
                        val kkey = XK_KeyMap[XLookupKeysym(e, 0)] ?: Key.UNKNOWN
                        //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$keySym")
                        dispatchKeyEvent(ev, 0, keyCode.toChar(), kkey, keyCode)
                        //break@loop
                    }
                    MotionNotify, ButtonPress, ButtonRelease -> {
                        val mot = MyXMotionEvent(e.pointer)
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

                        dispatchSimpleMouseEvent(ev, 0, mot.x, mot.y, button, simulateClickOnUp = true)
                    }
                    else -> {
                        //println("OTHER EVENT ${e.type}")
                    }
                }
            }
            dispatchStopEvent()
            dispatchDestroyEvent()

            XDestroyWindow(d, w)
            XCloseDisplay(d)
        }
    }
}
