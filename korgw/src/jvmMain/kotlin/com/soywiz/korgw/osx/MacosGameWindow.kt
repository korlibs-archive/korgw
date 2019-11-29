package com.soywiz.korgw.osx

import com.soywiz.kgl.KmlGl
import com.soywiz.korag.AG
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.GameWindowCoroutineDispatcher
import com.soywiz.korgw.osx.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.URL
import kotlin.coroutines.CoroutineContext

class MacAG(val window: Long) : AGOpengl() {
    override val gles: Boolean = true
    override val gl: KmlGl = MacKmlGl()
    override val nativeComponent: Any = window
}

class MacGameWindow : GameWindow() {
    val autoreleasePool = NSClass("NSAutoreleasePool").alloc().msgSend("init")

    companion object {
        val isMainThread get() = NSClass("NSThread").msgSend("isMainThread") != 0L
    }
    val app = NSClass("NSApplication").msgSend("sharedApplication")
    val sharedApp = app
    val MyNsWindow = AllocateClass("MyNSWindow", "NSWindow")
    val rect = NSRect(0, 0, 128, 128)
    val window = MyNsWindow.alloc().msgSend(
        "initWithContentRect:styleMask:backing:defer:",
        rect,
        NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or NSWindowStyleMaskMiniaturizable or NSWindowStyleMaskResizable or NSWindowStyleMaskResizable,
        NSBackingStoreBuffered,
        false
    ).also {
        it.msgSend("setTitle:", NSString(""))
    }

    override val key: CoroutineContext.Key<*>
        get() = super.key
    override val ag: MacAG = MacAG(window)
    override val coroutineDispatcher: GameWindowCoroutineDispatcher
        get() = super.coroutineDispatcher
    override var fps: Int
        get() = super.fps
        set(value) {}
    override var title: String
        get() = NSString(window.msgSend("title")).toString()
        set(value) {
            window.msgSend("title", NSString(value).id)
        }
    override val width: Int
        get() = super.width
    override val height: Int
        get() = super.height
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
        window.msgSend("setContentSize:", MyNativeNSPoint.ByValue(width, height))

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
        return super.openFileDialog(filter, write, multi)
    }

    override fun close() {
        super.close()
        autoreleasePool.msgSend("drain")
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }

        app.msgSend("setActivationPolicy:", 0)
        val AppDelegateClass = ObjectiveC.objc_allocateClassPair(NSObject.OBJ_CLASS, "AppDelegate", 0)
        val NSApplicationDelegate = ObjectiveC.objc_getProtocol("NSApplicationDelegate")
        ObjectiveC.class_addProtocol(AppDelegateClass, NSApplicationDelegate)
        ObjectiveC.class_addMethod(AppDelegateClass, sel("applicationShouldTerminate:"), applicationShouldTerminateCallback, "@:@");
        val appDelegate = AppDelegateClass.alloc().msgSend("init")
        app.msgSend("setDelegate:", appDelegate)
        app.msgSend("finishLaunching")

        val menubar = NSClass("NSMenu").alloc().msgSend("init")
        val appMenuItem = NSClass("NSMenuItem").alloc().msgSend("init")
        menubar.msgSend("addItem:", appMenuItem)
        app.msgSend("setMainMenu:", menubar)

        ///////////////////

        val processName = NSString(NSClass("NSProcessInfo").msgSend("processInfo").msgSend("processName"))

        var a: NSRect

        val appMenu = NSClass("NSMenu").alloc().msgSend("init")
        val quitMenuItem = NSClass("NSMenuItem").alloc()
            .msgSend("initWithTitle:action:keyEquivalent:", NSString("Quit $processName").id, sel("terminate:"), NSString("q").id)
        quitMenuItem.msgSend("autorelease")
        appMenu.msgSend("addItem:", quitMenuItem)
        appMenuItem.msgSend("setSubmenu:", appMenu)

        //window.msgSend("styleMask", window.msgSend("styleMask").toInt() or NSWindowStyleMaskFullScreen)

        window.msgSend("setReleasedWhenClosed:", 0L)

        window.msgSend("cascadeTopLeftFromPoint:", NSPoint(20, 20))

        val pixelFormat = NSClass("NSOpenGLPixelFormat").alloc().msgSend(
            "initWithAttributes:", intArrayOf(
                8, 24,
                11, 8,
                5,
                73,
                72,
                55, 1,
                56, 4,
                //99, 0x1000, // or 0x3200
                //99, 0x3200,
                99, 0x4100,
                0
            )
        )
        val openGLContext = NSClass("NSOpenGLContext").alloc().msgSend("initWithFormat:shareContext:", pixelFormat, null)
        val contentView = window.msgSend("contentView")
        openGLContext.msgSend("setView:", contentView)
        println("contentView: $contentView")
        contentView.msgSend("setWantsBestResolutionOpenGLSurface:", true)

        //val openglView = NSClass("NSOpenGLView").alloc().msgSend("initWithFrame:pixelFormat:", MyNativeNSRect.ByValue(0, 0, 512, 512), pixelFormat)
        //val openGLContext = openglView.msgSend("openGLContext")
        //window.msgSend("contentView", openglView)
        //val contentView = window.msgSend("contentView")

        println("pixelFormat: $pixelFormat")
        println("openGLContext: $openGLContext")

        window.msgSend("setAcceptsMouseMovedEvents:", true)
        window.msgSend("setBackgroundColor:", NSClass("NSColor").msgSend("blackColor"))
        window.msgSend("makeKeyAndOrderFront:", app)
        window.msgSend("center")

        app.msgSend("activateIgnoringOtherApps:", true)

        window.msgSend("makeKeyWindow")
        window.msgSend("setIsVisible:", true)

        //contentView.msgSend("fullScreenEnable")

        //val NSApp = Foundation.NATIVE.getGlobalVariableAddress("NSApp")
        val NSDefaultRunLoopMode = Foundation.NATIVE.getGlobalVariableAddress("NSDefaultRunLoopMode")
        //val NSDefaultRunLoopMode = Foundation.NATIVE.getGlobalVariableAddress("NSDefaultRunLoopMode")
        println("NSDefaultRunLoopMode: $NSDefaultRunLoopMode")

        fun renderOpengl() {
            val rect = MyNativeNSRect()
            window.msgSend_stret(rect, "frame")

            openGLContext.msgSend("makeCurrentContext")
            GL.glViewport(0, 0, rect.width.toInt(), rect.height.toInt())
            GL.glClearColor(.3f, .7f, 1f, 1f)
            GL.glClear(GL.GL_COLOR_BUFFER_BIT)

            frame()

            openGLContext.msgSend("flushBuffer")
        }

        val mouseEvent = ObjcCallbackVoid { self, _sel, sender ->
            val point = sender.msgSendNSPoint("locationInWindow")
            val buttonNumber = sender.msgSend("buttonNumber")
            val clickCount = sender.msgSend("clickCount")

            val rect = MyNativeNSRect()
            contentView.msgSend_stret(rect, "frame")

            val rect2 = MyNativeNSRect()
            window.msgSend_stret(rect2, "frame")

            val rect3 = MyNativeNSRect()
            window.msgSend_stret(rect3, "contentRectForFrameRect:", rect2)

            val dims = intArrayOf(720, 480)
            GL.CGLSetParameter(openGLContext, 304, dims)
            GL.CGLEnable(openGLContext, 304)

            val point2 = NSPoint(point.x, rect.height - point.y)

            //val res = NSClass("NSEvent").id.msgSend_stret(data, "mouseLocation")

            val selName = ObjectiveC.sel_getName(_sel)

            println("MOUSE EVENT ($selName) from NSWindow! $point2 : $buttonNumber : $clickCount")
        }

        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseEntered:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseExited:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseDragged:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseMoved:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseDown:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("mouseUp:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("rightMouseDragged:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("rightMouseMoved:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("rightMouseDown:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("rightMouseUp:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("otherMouseDragged:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("otherMouseMoved:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("otherMouseDown:"), mouseEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("otherMouseUp:"), mouseEvent, "v@:@")

        val keyEvent = ObjcCallbackVoid { self, _sel, sender ->
            val selName = ObjectiveC.sel_getName(_sel)
            val characters = NSString(sender.msgSend("characters")).toString()
            val charactersIgnoringModifiers = NSString(sender.msgSend("charactersIgnoringModifiers")).toString()
            val char = charactersIgnoringModifiers.getOrNull(0) ?: '\u0000'
            val keyCode = sender.msgSend("keyCode").toInt()

            val key = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN

            println("keyDown: $selName : $characters : ${char.toInt()} : $charactersIgnoringModifiers : $keyCode : $key")
        }

        ObjectiveC.class_addMethod(MyNsWindow, sel("keyDown:"), keyEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("keyUp:"), keyEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("keyPress:"), keyEvent, "v@:@")
        ObjectiveC.class_addMethod(MyNsWindow, sel("flagsChanged:"), ObjcCallbackVoid { self, _sel, sender ->
            val modifierFlags = sender.msgSend("modifierFlags")
            println("flags changed! : $modifierFlags")
        }, "v@:@")
        window.msgSend("setAcceptsMouseMovedEvents:", true)

        /*
        val eventHandler = object : ObjcCallback {
            override fun invoke(self: Long, _sel: Long, sender: Long): Long {
                val point = NSPoint()

                val senderName = ObjectiveC.class_getName(ObjectiveC.object_getClass(sender))
                //val res = sender.msgSend_stret(data, "mouseLocation:")
                //println("Mouse moved! $self, $_sel, $sender : $senderName : ${ObjectiveC.sel_getName(_sel)} : $res : $point : $data : ${sender.msgSend("buttonNumber")} : ${sender.msgSend("clickCount")}")

                renderOpengl()
                return 0L
            }
        }
    val MyResponderClass = AllocateClass("MyResponder", "NSObject", "NSResponder")
    //ObjectiveC.class_addMethod(MyResponderClass, sel("mouseDragged:"), eventHandler, "v@:@")
    //ObjectiveC.class_addMethod(MyResponderClass, sel("mouseUp:"), eventHandler, "v@:@")
    //ObjectiveC.class_addMethod(MyResponderClass, sel("mouseDown:"), eventHandler , "v@:@")
    ObjectiveC.class_addMethod(MyResponderClass, sel("mouseMoved:"), eventHandler, "v@:@")
    val Responder = MyResponderClass.alloc().msgSend("init")
    window.msgSend("setNextResponder:", Responder)
    */

        val WindowDelegate = AllocateClass("WindowDelegate", "NSObject", "NSWindowDelegate")
        ObjectiveC.class_addMethod(WindowDelegate, sel("windowWillClose:"), windowWillClose, "v@:@")
        ObjectiveC.class_addMethod(WindowDelegate, sel("windowDidExpose:"), ObjcCallbackVoid { self, _sel, notification ->
            //println("windowDidExpose")
            renderOpengl()
        }, "v@:@")
        ObjectiveC.class_addMethod(WindowDelegate, sel("windowDidUpdate:"), ObjcCallbackVoid { self, _sel, notification ->
            //println("windowDidUpdate")
            renderOpengl()
        }, "v@:@")
        ObjectiveC.class_addMethod(WindowDelegate, sel("windowDidResize:"), ObjcCallbackVoid { self, _sel, notification ->
            val rect = MyNativeNSRect()
            window.msgSend_stret(rect, "frame")
            openGLContext.msgSend("clearDrawable")
            contentView.msgSend("setBoundsSize:", MyNativeNSPoint.ByValue(rect.width, rect.height))
            openGLContext.msgSend("setView:", contentView)
            renderOpengl()
        }, "v@:@")

        val Delegate = WindowDelegate.alloc().msgSend("init")
        window.msgSend("setDelegate:", Delegate)

        ag.__ready()
        dispatchInitEvent()
        app.msgSend("run")

        autoreleasePool.msgSend("drain")
    }
}

val NSWindowStyleMaskTitled = 1 shl 0
val NSWindowStyleMaskClosable = 1 shl 1
val NSWindowStyleMaskMiniaturizable = 1 shl 2
val NSWindowStyleMaskResizable = 1 shl 3
val NSWindowStyleMaskFullScreen = 1 shl 14
val NSWindowStyleMaskFullSizeContentView = 1 shl 15

val NSBackingStoreBuffered = 2


internal val KeyCodesToKeys = mapOf(
    0x24 to Key.ENTER,
    0x4C to Key.ENTER,
    0x30 to Key.TAB,
    0x31 to Key.SPACE,
    0x33 to Key.DELETE,
    0x35 to Key.ESCAPE,
    0x37 to Key.META,
    0x38 to Key.LEFT_SHIFT,
    0x39 to Key.CAPS_LOCK,
    0x3A to Key.LEFT_ALT,
    0x3B to Key.LEFT_CONTROL,
    0x3C to Key.RIGHT_SHIFT,
    0x3D to Key.RIGHT_ALT,
    0x3E to Key.RIGHT_CONTROL,
    0x7B to Key.LEFT,
    0x7C to Key.RIGHT,
    0x7D to Key.DOWN,
    0x7E to Key.UP,
    0x48 to Key.VOLUME_UP,
    0x49 to Key.VOLUME_DOWN,
    0x4A to Key.MUTE,
    0x72 to Key.HELP,
    0x73 to Key.HOME,
    0x74 to Key.PAGE_UP,
    0x75 to Key.DELETE,
    0x77 to Key.END,
    0x79 to Key.PAGE_DOWN,
    0x3F to Key.FUNCTION,
    0x7A to Key.F1,
    0x78 to Key.F2,
    0x76 to Key.F4,
    0x60 to Key.F5,
    0x61 to Key.F6,
    0x62 to Key.F7,
    0x63 to Key.F3,
    0x64 to Key.F8,
    0x65 to Key.F9,
    0x6D to Key.F10,
    0x67 to Key.F11,
    0x6F to Key.F12,
    0x69 to Key.F13,
    0x6B to Key.F14,
    0x71 to Key.F15,
    0x6A to Key.F16,
    0x40 to Key.F17,
    0x4F to Key.F18,
    0x50 to Key.F19,
    0x5A to Key.F20
)

internal val CharToKeys = mapOf(
    'a' to Key.A, 'A' to Key.A,
    'b' to Key.B, 'B' to Key.B,
    'c' to Key.C, 'C' to Key.C,
    'd' to Key.D, 'D' to Key.D,
    'e' to Key.E, 'E' to Key.E,
    'f' to Key.F, 'F' to Key.F,
    'g' to Key.G, 'G' to Key.G,
    'h' to Key.H, 'H' to Key.H,
    'i' to Key.I, 'I' to Key.I,
    'j' to Key.J, 'J' to Key.J,
    'k' to Key.K, 'K' to Key.K,
    'l' to Key.L, 'L' to Key.L,
    'm' to Key.M, 'M' to Key.M,
    'n' to Key.N, 'N' to Key.N,
    'o' to Key.O, 'O' to Key.O,
    'p' to Key.P, 'P' to Key.P,
    'q' to Key.Q, 'Q' to Key.Q,
    'r' to Key.R, 'R' to Key.R,
    's' to Key.S, 'S' to Key.S,
    't' to Key.T, 'T' to Key.T,
    'u' to Key.U, 'U' to Key.U,
    'v' to Key.V, 'V' to Key.V,
    'w' to Key.W, 'W' to Key.W,
    'x' to Key.X, 'X' to Key.X,
    'y' to Key.Y, 'Y' to Key.Y,
    'z' to Key.Z, 'Z' to Key.Z,
    '0' to Key.N0, '1' to Key.N1, '2' to Key.N2, '3' to Key.N3, '4' to Key.N4,
    '5' to Key.N5, '6' to Key.N6, '7' to Key.N7, '8' to Key.N8, '9' to Key.N9
)
