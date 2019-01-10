package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
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

actual val DefaultGameWindow: GameWindow = object : GameWindow() {
    val app = NSApplication.sharedApplication()
    val controller = WinController()

    val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

    val attrs = uintArrayOf(
        //NSOpenGLPFAOpenGLProfile,
        //NSOpenGLProfileVersion4_1Core,
        NSOpenGLPFAColorSize.convert(), 24.convert(),
        NSOpenGLPFAAlphaSize.convert(), 8.convert(),
        NSOpenGLPFADoubleBuffer.convert(),
        NSOpenGLPFADepthSize.convert(), 32.convert(),
        0.convert()
    )

    val pixelFormat = attrs.usePinned {
        NSOpenGLPixelFormat(it.addressOf(0).reinterpret<NSOpenGLPixelFormatAttributeVar>())
        //NSOpenGLPixelFormat.alloc()!!.initWithAttributes(it.addressOf(0).reinterpret())!!
    }

    val windowConfigWidth = 640
    val windowConfigHeight = 480
    val windowConfigTitle = "Title"

    val windowRect = NSScreen.mainScreen()!!.frame.useContents<CGRect, CValue<CGRect>> {
        NSMakeRect(
            (size.width * 0.5 - windowConfigWidth * 0.5),
            (size.height * 0.5 - windowConfigHeight * 0.5),
            windowConfigWidth.toDouble(),
            windowConfigHeight.toDouble()
        )
    }

    private val openglView: NSOpenGLView = NSOpenGLView(NSMakeRect(0.0, 0.0, 16.0, 16.0), pixelFormat)
    var timer: NSTimer? = null

    private val window: NSWindow = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
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
                openglView.bounds.useContents<CGRect, Unit> {
                    val bounds = this
                    val width = bounds.size.width.toInt()
                    val height = bounds.size.height.toInt()
                    //macTrace("windowDidResize")
                    ag.resized(width, height)
                    dispatch(reshapeEvent.apply {
                        this.width = width
                        this.height = height
                    })
                    doRender()

                    Unit
                }
            }
        }

        setAcceptsMouseMovedEvents(true)
        setContentView(openglView)
        setContentMinSize(NSMakeSize(150.0, 100.0))
        val responder = object : NSResponder() {
            override fun acceptsFirstResponder(): Boolean = true

            fun getHeight(): Int = openglView.bounds.useContents<CGRect, Int> { size.height.toInt() }

            override fun mouseUp(event: NSEvent) {
                super.mouseUp(event)
                event.locationInWindow.useContents<CGPoint, Unit> {
                    val rx = x.toInt()
                    val ry = getHeight() - y.toInt()
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
            }

            override fun mouseDown(event: NSEvent) {
                super.mouseDown(event)
                event.locationInWindow.useContents<CGPoint, Unit> {
                    val rx = x.toInt()
                    val ry = getHeight() - y.toInt()
                    //println("mouseDown($rx,$ry)")
                    mouseDown(rx, ry, event.buttonNumber.toInt())
                }
            }

            override fun mouseMoved(event: NSEvent) {
                super.mouseMoved(event)
                event.locationInWindow.useContents<CGPoint, Unit> {
                    val rx = x.toInt()
                    val ry = getHeight() - y.toInt()
                    //println("mouseMoved($rx,$ry)")
                    mouseMoved(rx, ry)
                }
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



            override fun mouseDragged(event: NSEvent) {
                super.mouseDragged(event)
                event.locationInWindow.useContents<CGPoint, Unit> {
                    val rx = x.toInt()
                    val ry = getHeight() - y.toInt()
                    //println("mouseDragged($rx,$ry)")
                    mouseMoved(rx, ry)
                }
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
                    this.char = char
                })
            }

            override fun keyDown(event: NSEvent) {
                super.keyDown(event)
                keyDownUp(event, true)
            }

            override fun keyUp(event: NSEvent) {
                super.keyUp(event)
                keyDownUp(event, false)
            }
        }
        openglView.setNextResponder(responder)
        setNextResponder(responder)
    }

    private fun doRender() {
        //macTrace("render")
        val context = openglView.openGLContext

        coroutineDispatcher.executePending()

        //context?.flushBuffer()
        context?.makeCurrentContext()
        ag.onRender(ag)
        dispatch(renderEvent)
        context?.flushBuffer()
    }

    override val ag: AG = AGNative()

    override var fps: Int = 60
    override val width: Int get() = window.frame.useContents { this.size.width }.toInt()
    override val height: Int get() = window.frame.useContents { this.size.height }.toInt()
    override var title: String = "Title"
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
        set(value) {}
    override var quality: Quality
        get() = Quality.AUTO
        set(value) {}

    override fun setSize(width: Int, height: Int) {
        val rect = NSScreen.mainScreen()!!.frame.useContents {
            NSMakeRect(
                ((size.width - width) * 0.5), ((size.height - height) * 0.5),
                width.toDouble(), height.toDouble()
            )
        }
        window.setFrame(rect, true, true)
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
                window.makeKeyAndOrderFront(this)
            }

            override fun applicationDidFinishLaunching(notification: NSNotification) {
                //val data = decodeImageData(readBytes("icon.jpg"))
                //println("${data.width}, ${data.height}")

                openglView.openGLContext?.makeCurrentContext()
                try {
                    macTrace("init[a]")
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
