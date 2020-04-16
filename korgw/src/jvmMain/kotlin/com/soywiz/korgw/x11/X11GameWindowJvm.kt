package com.soywiz.korgw.x11

import com.soywiz.kgl.KmlGl
import com.soywiz.kmem.arrayfill
import com.soywiz.kmem.write32LE
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.DialogInterface
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.ZenityDialogs
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.rootLocalVfs
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.URL
import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11.*

//class X11Ag(val window: X11GameWindow, override val gl: KmlGl = LogKmlGlProxy(X11KmlGl())) : AGOpengl() {
class X11Ag(val window: X11GameWindow, override val gl: KmlGl = X11KmlGl) : AGOpengl() {
    override val gles: Boolean = true
    override val nativeComponent: Any = window
}

class X11GameWindow : GameWindow(), DialogInterface by ZenityDialogs() {
    override val ag: X11Ag by lazy { X11Ag(this) }
    override var fps: Int = 60
    override var width: Int = 200; private set
    override var height: Int = 200; private set
    override var title: String = "Korgw"
        set(value) {
            field = value
            realSetTitle(value)
        }
    override var icon: Bitmap? = null
        set(value) {
            field = value
            realSetIcon(value)
        }
    override var fullscreen: Boolean = false
        set(value) {
            field = value
            realSetFullscreen(value)
        }
    override var visible: Boolean = true
        set(value) {
            field = value
            realSetVisible(value)
        }
    override var quality: Quality = Quality.AUTOMATIC

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    var d: Display? = null
    var root: Window? = null
    val NilWin: Window? = null
    var w: Window? = null
    var s: Int = 0

    fun realSetTitle(title: String): Unit = X.run {
        if (d == null || w == NilWin) return@run
        //X.XSetWMIconName(d, w, )
        X.XStoreName(d, w, title)
        X.XSetIconName(d, w, title)
    }

    fun realSetIcon(value: Bitmap?): Unit = X.run {
        if (d == null || w == NilWin || value == null) return@run
        val property = XInternAtom(d, "_NET_WM_ICON", false)
        val bmp = value.toBMP32()
        val VSIZE = NativeLong.SIZE
        val bytes = ByteArray((bmp.area + 2) * VSIZE)
        bytes.write32LE(0, bmp.width)
        bytes.write32LE(VSIZE, bmp.height)
        for (n in 0 until bmp.area) {
            val pos = VSIZE * (2 + n)
            val c = bmp.data[n]
            bytes[pos + 0] = c.r.toByte()
            bytes[pos + 1] = c.g.toByte()
            bytes[pos + 2] = c.b.toByte()
            bytes[pos + 3] = c.a.toByte()
        }
        val mem = Memory((bytes.size * 8).toLong())
        mem.write(0L, bytes, 0, bytes.size)
        XChangeProperty(
            d, w, property, XA_CARDINAL, 32, PropModeReplace,
            mem, bytes.size / NativeLong.SIZE
        )
    }

    // https://stackoverflow.com/questions/9065669/x11-glx-fullscreen-mode
    fun realSetFullscreen(value: Boolean): Unit = X.run {
        if (d == null || w == NilWin) return@run
    }
    fun realSetVisible(value: Boolean): Unit = X.run {
        if (d == null || w == NilWin) return@run
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        X.apply {
            // Required here so setSize is called
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

            println("screenWidth: $screenWidth, screenHeight: $screenHeight, winX=$winX, winY=$winY, width=$width, height=$height")

            w = XCreateSimpleWindow(
                d, XRootWindow(d, s),
                winX, winY,
                width, height,
                1,
                XBlackPixel(d, s), XWhitePixel(d, s)
            )
            //val attr = XSetWindowAttributes().apply { autoWrite() }.apply { autoRead() }
            //XChangeWindowAttributes(d, w, NativeLong(0L), attr)

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
            XMapWindow(d, w)
            realSetIcon(icon)
            realSetVisible(fullscreen)
            realSetVisible(visible)
            realSetTitle(title)

            val doubleBuffered = false
            //val doubleBuffered = true
            val ctx = X11OpenglContext(d, w, doubleBuffered = doubleBuffered)
            ctx.makeCurrent()

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

            val e = XEvent()
            loop@ while (running) {
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
                    ClientMessage, DestroyNotify -> close()
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
                        val keyCode = XKeyEvent(e.pointer).keycode.toInt()
                        val kkey = XK_KeyMap[XLookupKeysym(e, 0)] ?: Key.UNKNOWN
                        //println("KEY: $ev, ${keyCode.toChar()}, $kkey, $keyCode, keySym=$keySym")
                        dispatchKeyEvent(ev, 0, keyCode.toChar(), kkey, keyCode)
                        //break@loop
                    }
                    MotionNotify, ButtonPress, ButtonRelease -> {
                        val mot = MyXMotionEvent(e.pointer)
                        //val mot = e.xmotion
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
