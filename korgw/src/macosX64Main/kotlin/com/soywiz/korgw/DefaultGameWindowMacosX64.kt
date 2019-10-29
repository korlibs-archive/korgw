package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.darwin.*

actual fun CreateDefaultGameWindow(): GameWindow = object : GameWindow() {
    val app = NSApplication.sharedApplication()
    val controller = WinController()

    val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

    val attrs: UIntArray by lazy {
        val antialias = (this.quality != GameWindow.Quality.PERFORMANCE)
        val antialiasArray = if (antialias) intArrayOf(
            NSOpenGLPFAMultisample.convert(),
            NSOpenGLPFASampleBuffers.convert(), 1.convert(),
            NSOpenGLPFASamples.convert(), 4.convert()
        ) else intArrayOf()
        intArrayOf(
            *antialiasArray,
            //NSOpenGLPFAOpenGLProfile,
            //NSOpenGLProfileVersion4_1Core,
            NSOpenGLPFAColorSize.convert(), 24.convert(),
            NSOpenGLPFAAlphaSize.convert(), 8.convert(),
            NSOpenGLPFADoubleBuffer.convert(),
            NSOpenGLPFADepthSize.convert(), 32.convert(),
            0.convert()
        ).toUIntArray()
    }

    val pixelFormat by lazy {
        attrs.usePinned {
            NSOpenGLPixelFormat(it.addressOf(0).reinterpret<NSOpenGLPixelFormatAttributeVar>())
            //NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).reinterpret())!!
        }
    }

    val windowConfigWidth = 640
    val windowConfigHeight = 480
    val windowConfigTitle = ""

    val windowRect: CValue<NSRect> = run {
        val frame = NSScreen.mainScreen()!!.frame
        NSMakeRect(
            (frame.width * 0.5 - windowConfigWidth * 0.5),
            (frame.height * 0.5 - windowConfigHeight * 0.5),
            windowConfigWidth.toDouble(),
            windowConfigHeight.toDouble()
        )
    }

    private val openglView: NSOpenGLView = NSOpenGLView(NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    var timer: NSTimer? = null

    private var responder: NSResponder

    private val window: NSWindow = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
        setIsVisible(false)
        title = windowConfigTitle
        opaque = true
        hasShadow = true
        preferredBackingLocation = NSWindowBackingLocationVideoMemory
        hidesOnDeactivate = false
        releasedWhenClosed = false

        openglView.setFrame(contentRectForFrameRect(frame))
        delegate = object : NSObject(), NSWindowDelegateProtocol {
            override fun windowShouldClose(sender: NSWindow): Boolean {
                println("windowShouldClose")
                return true
            }

            override fun windowWillClose(notification: NSNotification) {
                println("windowWillClose")
            }

            override fun windowDidResize(notification: NSNotification) {
                println("windowDidResize")
                val width = openglView.bounds.width.toInt()
                val height = openglView.bounds.height.toInt()
                //macTrace("windowDidResize")
                dispatchReshapeEvent(0, 0, width, height)
                doRender()
            }
        }

        setAcceptsMouseMovedEvents(true)
        setContentView(openglView)
        setContentMinSize(NSMakeSize(150.0, 100.0))
        responder = object : NSResponder() {
            override fun acceptsFirstResponder(): Boolean = true

            fun getHeight() = openglView.bounds.height

            override fun mouseUp(event: NSEvent) {
                //super.mouseUp(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseUp($rx,$ry)")
                val x = rx
                val y = ry
                val button = event.buttonNumber.toInt()

                mouseEvent(MouseEvent.Type.UP, x, y, button)
                mouseEvent(
                    MouseEvent.Type.CLICK,
                    x,
                    y,
                    button
                ) // @TODO: Conditionally depending on the down x,y & time
            }

            override fun mouseDown(event: NSEvent) {
                //super.mouseDown(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseDown($rx,$ry)")
                mouseDown(rx, ry, event.buttonNumber.toInt())
            }

            override fun mouseMoved(event: NSEvent) {
                //super.mouseMoved(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseMoved($rx,$ry)")
                mouseMoved(rx, ry)
            }


            private fun mouseEvent(etype: MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
                dispatch(mouseEvent.apply {
                    this.type = etype
                    this.x = ex
                    this.y = ey
                    this.buttons = 1 shl ebutton
                    this.isAltDown = false
                    this.isCtrlDown = false
                    this.isShiftDown = false
                    this.isMetaDown = false
                    //this.scaleCoords = false
                })
            }

            fun mouseDown(x: Int, y: Int, button: Int) =
                mouseEvent(MouseEvent.Type.DOWN, x, y, button)

            fun mouseMoved(x: Int, y: Int) = mouseEvent(MouseEvent.Type.MOVE, x, y, 0)
            fun mouseDragged(x: Int, y: Int) = mouseEvent(MouseEvent.Type.DRAG, x, y, 0)

            override fun mouseDragged(event: NSEvent) {
                super.mouseDragged(event)
                val rx = event.locationInWindow.x.toInt()
                val ry = (getHeight() - event.locationInWindow.y).toInt()
                //println("mouseDragged($rx,$ry)")
                mouseDragged(rx, ry)
            }

            fun keyDownUp(event: NSEvent, pressed: Boolean) {
                val str = event.charactersIgnoringModifiers ?: "\u0000"
                val c = str.getOrNull(0) ?: '\u0000'
                val cc = c.toInt().toChar()
                //println("keyDownUp")
                val char = cc
                val modifiers: Int = event.modifierFlags.convert()
                val keyCode = event.keyCode.toInt()

                val key = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN
                //println("keyDownUp: char=$char, modifiers=$modifiers, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed")
                dispatch(keyEvent.apply {
                    this.type =
                        if (pressed) KeyEvent.Type.DOWN else KeyEvent.Type.UP
                    this.id = 0
                    this.key = key
                    this.keyCode = keyCode
                    this.character = char
                })
            }

            override fun keyDown(event: NSEvent) {
                //super.keyDown(event)
                keyDownUp(event, true)
            }

            override fun keyUp(event: NSEvent) {
                //super.keyUp(event)
                keyDownUp(event, false)
            }

            //external override fun performKeyEquivalent(event: NSEvent): Boolean {
            //    return true
            //}
        }
        openglView.setNextResponder(responder)
        setNextResponder(responder)
        setIsVisible(false)
    }

    private fun doRender() {
        //macTrace("render")
        val context = openglView.openGLContext

        coroutineDispatcher.executePending()

        //context?.flushBuffer()
        context?.makeCurrentContext()
        ag.clear(Colors.BLACK)
        ag.onRender(ag)
        dispatch(renderEvent)
        context?.flushBuffer()
    }

    override val ag: AG = AGNative()

    override var fps: Int = 60
    override val width: Int get() = window.frame.width.toInt()
    override val height: Int get() = window.frame.height.toInt()
    override var title: String = ""
        set(value) {
            field = value
            window.title = value
        }

    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = (window.styleMask and NSFullScreenWindowMask) == NSFullScreenWindowMask
        set(value) {
            if (fullscreen != value) {
                window.toggleFullScreen(window)
            }
        }
    override var visible: Boolean
        get() = window.visible
        set(value) {
            window.setIsVisible(value)
            if (value) {
                window.makeKeyAndOrderFront(this)
            }
            //if (value) {
            //    window.makeKeyAndOrderFront(this)
            //    app.activateIgnoringOtherApps(true)
            //} else {
            //    window.orderOut(this)
            //}
        }

    override fun setSize(width: Int, height: Int) {
        val frame = NSScreen.mainScreen()!!.frame
        val rect = NSMakeRect(
            ((frame.width - width) * 0.5), ((frame.height - height) * 0.5),
            width.toDouble(), height.toDouble()
        )

        window.setFrame(rect, true, false)
    }

    override suspend fun browse(url: URL) {
        super.browse(url)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        val openDlg: NSOpenPanel = NSOpenPanel().apply {
            setCanChooseFiles(true)
            setAllowsMultipleSelection(false)
            setCanChooseDirectories(false)
        }
        if (openDlg.runModalForDirectory(null, null).toInt() == NSOKButton.toInt()) {
            return openDlg.filenames().filterIsInstance<String>().map { localVfs(it) }
        } else {
            throw CancelException()
        }
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) = autoreleasepool {
        app.mainMenu = NSMenu().apply {
            //this.autoenablesItems = true
            addItem(NSMenuItem("Application", null, "").apply {
                this.submenu = NSMenu().apply {
                    //this.autoenablesItems = true
                    addItem(NSMenuItem("Quit", NSSelectorFromString(WinController::doTerminate.name), "q").apply {
                        target = controller
                        //enabled = true
                    })
                }
                //enabled = true
            })
        }


        val agNativeComponent = Any()
        val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

        app.delegate = object : NSObject(), NSApplicationDelegateProtocol {

            //private val openglView: AppNSOpenGLView

            override fun applicationShouldTerminateAfterLastWindowClosed(app: NSApplication): Boolean {
                println("applicationShouldTerminateAfterLastWindowClosed")
                return true
            }

            override fun applicationWillFinishLaunching(notification: NSNotification) {
                println("applicationWillFinishLaunching")
                //window.makeKeyAndOrderFront(this)
            }

            override fun applicationDidFinishLaunching(notification: NSNotification) {
                //val data = decodeImageData(readBytes("icon.jpg"))
                //println("${data.width}, ${data.height}")

                openglView.openGLContext?.makeCurrentContext()
                try {
                    macTrace("init[a] -- bb")
                    macTrace("init[b]")
                    println("KoruiWrap.pentry[0]")
                    ag.__ready()
                    //launch(KoruiDispatcher) { // Doesn't work!
                    println("KoruiWrap.pentry[1]")
                    println("KoruiWrap.entry[0]")
                    kotlinx.coroutines.GlobalScope.launch(coroutineDispatcher) {
                        entry()
                    }
                    println("KoruiWrap.entry[1]")
                    //}
                    println("KoruiWrap.pentry[2]")

                    doRender()
                    timer = NSTimer.scheduledTimerWithTimeInterval(1.0 / 60.0, true, ::timer)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    window.close()
                }
            }

            private fun timer(timer: NSTimer?) {
                //println("TIMER")
                doRender()
            }

            override fun applicationWillTerminate(notification: NSNotification) {
                println("applicationWillTerminate")
                // Insert code here to tear down your application

            }
        }

        app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
        app.activateIgnoringOtherApps(true)
        coroutineDispatcher.executePending()
        app.run()
    }
}


class WinController : NSObject() {
    @ObjCAction
    fun doTerminate() {
        NSApplication.sharedApplication.terminate(null)
    }
}


fun macTrace(str: String) {
    println(str)
}

val CValue<NSPoint>.x get() = this.useContents { x }
val CValue<NSPoint>.y get() = this.useContents { y }

val CValue<NSRect>.left get() = this.useContents { origin.x }
val CValue<NSRect>.top get() = this.useContents { origin.y }
val CValue<NSRect>.width get() = this.useContents { size.width }
val CValue<NSRect>.height get() = this.useContents { size.height }
