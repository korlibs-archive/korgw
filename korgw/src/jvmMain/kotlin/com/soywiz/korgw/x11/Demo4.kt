package com.soywiz.korgw.x11

/*
object Demo4 {
    const val GLX_RGBA = 4
    const val GLX_DEPTH_SIZE = 12
    const val GLX_DOUBLEBUFFER = 5

    val aatt = intArrayOf(GLX_RGBA, GLX_DEPTH_SIZE, 24, GLX_DOUBLEBUFFER, None)

    @JvmStatic
    fun main(args: Array<String>) {
        val title = "HELLO"

        X.apply {
            val d = XOpenDisplay(null)
            val s = XDefaultScreen(d)

            val root = XDefaultRootWindow(d)

            val vi = glXChooseVisual(d, 0, aatt);

            println("VI: $vi")

            //val cmap = XCreateColormap(d, root, vi->visual, AllocNone);
            val screenWidth = XDisplayWidth(d, s)
            val screenHeight = XDisplayHeight(d, s)

            var width: Int = 640
            var height: Int = 480
            val winX = screenWidth / 2 - width / 2
            val winY = screenHeight / 2 - height / 2

            println("screenWidth: $screenWidth, screenHeight: $screenHeight, winX=$winX, winY=$winY")

            val w = XCreateSimpleWindow(
                d, XRootWindow(d, s),
                winX * 2, winY * 2,
                width, height,
                1,
                XBlackPixel(d, s), XWhitePixel(d, s)
            )

            val eventMask = NativeLong(
                (ExposureMask or StructureNotifyMask or KeyPressMask or PointerMotionMask or ButtonPressMask)
                    .toLong())

            XSelectInput(d, w, eventMask)
            XStoreName(d, w, title)
            XSetIconName(d, w, title)
            XMapWindow(d, w)

            val glc = glXCreateContext(d, vi, null, true);
            glXMakeCurrent(d, w, glc);
            println(glGetString(GL.GL_VENDOR))
            println(glGetString(GL.GL_VERSION))

            val keysim = XStringToKeysym("A")
            println("keysim: $keysim")

            var frame = 0


            fun render() {
                glXMakeCurrent(d, w, glc);
                glViewport(0, 0, width, height)
                glClearColor(.3f, .6f, (frame % 60).toFloat() / 60, 1f)
                glClear(GL.GL_COLOR_BUFFER_BIT)
                glXSwapBuffers(d, w);
            }

            var running = true

            loop@ while (running) {
                val e = XEvent()
                //XNextEvent(d, e)
                while (XCheckWindowEvent(d, w, eventMask, e)) {
                    when (e.type) {
                        Expose -> {
                            println("EXPOSE")
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
                            //println("RESIZED! ${conf.width} ${conf.height}")
                        }
                        KeyPress, KeyRelease -> {
                            val key = XKeyEvent(e.pointer)
                            println(key.keycode)
                            //break@loop
                        }
                        MotionNotify -> {
                            //val mot = e.xmotion
                            val mot = MyXMotionEvent(e.pointer)
                            //println(XMotionEvent().size())
                            //println(mot.size)
                            //println("MOTION ${e.type} ${mot.type} ${mot.type} ${mot.x}, ${mot.y}")
                        }
                        ButtonPress, ButtonRelease -> {
                            println("BUTTON")
                        }
                        else -> {
                            println("OTHER EVENT ${e.type}")
                        }
                    }
                }
                Thread.sleep(16L)
                render()
                frame++
            }

            XDestroyWindow(d, w)
            XCloseDisplay(d)
        }
    }
}
*/
