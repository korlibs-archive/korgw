package com.soywiz.korgw

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.cinterop.nativeHeap.alloc
import kotlinx.coroutines.*
import platform.windows.*
import kotlin.math.*

//override val ag: AG = AGNative()

val windowsGameWindow: WindowsGameWindow = WindowsGameWindow()
actual fun CreateDefaultGameWindow(): GameWindow = windowsGameWindow

fun processString(maxLen: Int, callback: (ptr: CPointer<WCHARVar>, maxLen: Int) -> Unit): String {
    return memScoped {
        val ptr = allocArray<WCHARVar>(maxLen)
        callback(ptr, maxLen)
        ptr.toKString()
    }
}

class WindowsGameWindow : GameWindow() {
    val agNativeComponent = Any()
    var hwnd: HWND? = null
    var glRenderContext: HGLRC? = null
    override val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

    override var fps: Int = 60
    override var title: String
        get() = processString(4096) { ptr, len ->
            GetWindowTextW(hwnd, ptr, len)
        }
        set(value) {
            SetWindowTextW(hwnd, value)
        }
    override val width: Int
        get() = memScoped {
            alloc<RECT>().let {
                GetWindowRect(hwnd, it.ptr)
                it.right - it.left
            }
        }
    override val height: Int
        get() = memScoped {
            alloc<RECT>().let {
                GetWindowRect(hwnd, it.ptr)
                it.bottom - it.top
            }
        }
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = memScoped {
            val placement = alloc<WINDOWPLACEMENT>()
            GetWindowPlacement(hwnd, placement.ptr)
            placement.showCmd.toInt() == SW_MAXIMIZE.toInt()
        }
        set(value) {
            ShowWindow(hwnd, if (value) SW_MAXIMIZE else SW_RESTORE)
        }

    override var visible: Boolean
        get() = IsWindowVisible(hwnd) != 0
        set(value) {
            ShowWindow(hwnd, if (value) SW_SHOW else SW_HIDE)
        }
    override var quality: Quality
        get() = super.quality
        set(value) {}

    val RECT.width get() = right - left
    val RECT.height get() = bottom - top

    override fun setSize(width: Int, height: Int): Unit = memScoped {
        alloc<RECT> {
            val rect: RECT = this

            val screenWidth = GetSystemMetrics(SM_CXSCREEN)
            val screenHeight = GetSystemMetrics(SM_CYSCREEN)
            val x = (screenWidth - width) / 2
            val y = (screenHeight - height) / 2
            rect.left = x
            rect.top = y
            rect.right = x + width
            rect.bottom = y + height
            val style = GetWindowLongA(hwnd, GWL_STYLE)
            val exstyle = GetWindowLongA(hwnd, GWL_EXSTYLE)
            val menu = false
            AdjustWindowRectEx(rect.ptr, (style and WS_OVERLAPPED.inv()).convert(), if (menu) 1 else 0, exstyle.convert())
            println("SetWindowPos: ($width, $height) -> (${rect.width}, ${rect.height})")
            SetWindowPos(hwnd, HWND_TOP, rect.left, rect.top, rect.width, rect.height, 0.convert())
        }
        Unit
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
        val selectedFile = openSelectFile(hwnd = hwnd)
        if (selectedFile != null) {
            return listOf(com.soywiz.korio.file.std.localVfs(selectedFile))
        } else {
            throw com.soywiz.korio.lang.CancelException()
        }
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        memScoped {
            // https://www.khronos.org/opengl/wiki/Creating_an_OpenGL_Context_(WGL)

            val windowTitle = ""
            val windowWidth = 0
            val windowHeight = 0

            val wc = alloc<WNDCLASSW>()

            val clazzName = "oglkotlinnative"
            val clazzNamePtr = clazzName.wcstr.getPointer(this@memScoped)
            wc.lpfnWndProc = staticCFunction(::WndProc)
            wc.hInstance = null
            wc.hbrBackground = COLOR_BACKGROUND.convert()

            val hInstance = GetModuleHandleA(null)
            //FindResourceA(null, null, 124)
            wc.hIcon = LoadIconAFunc(hInstance, 1000)
            //wc.hIcon = LoadIconAFunc(hInstance, 1)
            //wc.hIcon = LoadIconAFunc(hInstance, 32512)
            //wc.hIcon = LoadIconAFunc(null, 32512) // IDI_APPLICATION - MAKEINTRESOURCE(32512)

            wc.lpszClassName = clazzNamePtr.reinterpret()
            wc.style = CS_OWNDC.convert()
            if (RegisterClassW(wc.ptr).toInt() == 0) {
                return
            }

            val screenWidth = GetSystemMetrics(SM_CXSCREEN)
            val screenHeight = GetSystemMetrics(SM_CYSCREEN)
            hwnd = CreateWindowExW(
                WS_EX_CLIENTEDGE.convert(),
                clazzName,
                windowTitle,
                //(WS_OVERLAPPEDWINDOW or WS_VISIBLE).convert(),
                (WS_OVERLAPPEDWINDOW).convert(),
                min(max(0, (screenWidth - windowWidth) / 2), screenWidth - 16).convert(),
                min(max(0, (screenHeight - windowHeight) / 2), screenHeight - 16).convert(),
                windowWidth.convert(),
                windowHeight.convert(),
                null, null, null, null
            )
            println("ERROR: " + GetLastError())

            ShowWindow(hwnd, SW_SHOWNORMAL.convert())

            //SetTimer(hwnd, 1, 1000 / 60, staticCFunction(::WndTimer))
        }

        runBlocking {
            var running = true
            launch(coroutineDispatcher) {
                try {
                    entry()
                } catch (e: Throwable) {
                    println(e)
                    running = false
                }
            }

            memScoped {
                val fps = 60
                val msPerFrame = 1000 / fps
                val msg = alloc<MSG>()
                //var start = milliStamp()
                var prev = DateTime.nowUnixLong()
                while (running) {
                    while (
                        PeekMessageW(
                            msg.ptr,
                            null,
                            0.convert(),
                            0.convert(),
                            PM_REMOVE.convert()
                        ).toInt() != 0
                    ) {
                        TranslateMessage(msg.ptr)
                        DispatchMessageW(msg.ptr)
                    }
                    //val now = milliStamp()
                    //val elapsed = now - start
                    //val sleepTime = kotlin.math.max(0L, (16 - elapsed)).toInt()
                    //println("SLEEP: sleepTime=$sleepTime, start=$start, now=$now, elapsed=$elapsed")
                    //start = now
                    //Sleep(sleepTime)
                    val now = DateTime.nowUnixLong()
                    val elapsed = now - prev
                    //println("$prev, $now, $elapsed")
                    val sleepTime = max(0L, (msPerFrame - elapsed)).toInt()
                    prev = now

                    Sleep(sleepTime.convert())
                    coroutineDispatcher.executePending()
                    //println("RENDER")
                    tryRender()
                }
            }
        }
    }

    fun resized(width: Int, height: Int) {
        dispatchReshapeEvent(0, 0, width, height)
        tryRender()
    }

    fun tryRender() {
        if (hwnd != null && glRenderContext != null) {
            val hdc = GetDC(hwnd)
            //println("render")
            wglMakeCurrent(hdc, glRenderContext)
            //renderFunction()
            ag.onRender(ag)
            dispatch(renderEvent)
            SwapBuffers(hdc)
        }
    }

    fun keyUpdate(keyCode: Int, down: Boolean) {
        // @TODO: KeyEvent.Tpe.TYPE
        dispatch(keyEvent.apply {
            this.type = if (down) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
            this.id = 0
            this.key = KEYS[keyCode] ?: com.soywiz.korev.Key.UNKNOWN
            this.keyCode = keyCode
            this.character = keyCode.toChar()
        })
    }

    fun mouseEvent(etype: com.soywiz.korev.MouseEvent.Type, ex: Int, ey: Int, ebutton: Int, wParam: Int) {
        val lbutton = (wParam and MK_LBUTTON) != 0
        val rbutton = (wParam and MK_RBUTTON) != 0
        val shift = (wParam and MK_SHIFT) != 0
        val control = (wParam and MK_CONTROL) != 0
        val mbutton = (wParam and MK_MBUTTON) != 0
        val xbutton1 = (wParam and MK_XBUTTON1) != 0
        val xbutton2 = (wParam and MK_XBUTTON2) != 0
        val anyButton = lbutton || rbutton || mbutton || xbutton1 || xbutton2
        var buttons = 0
        if (lbutton) buttons = buttons or 1
        if (rbutton) buttons = buttons or 2
        if (mbutton) buttons = buttons or 4
        if (xbutton1) buttons = buttons or 8
        if (xbutton2) buttons = buttons or 16

        dispatch(mouseEvent.apply {
            this.type = etype
            this.x = ex
            this.y = ey
            this.button = MouseButton[ebutton]
            this.buttons = buttons
            this.isAltDown = false
            this.isCtrlDown = control
            this.isShiftDown = shift
            this.isMetaDown = false
            //this.scaleCoords = false
        })
    }
}

// @TODO: when + cases with .toUInt() or .convert() didn't work
val _WM_CREATE: UINT = WM_CREATE.convert()
val _WM_SIZE: UINT = WM_SIZE.convert()
val _WM_QUIT: UINT = WM_QUIT.convert()
val _WM_MOUSEMOVE: UINT = WM_MOUSEMOVE.convert()
val _WM_MOUSELEAVE: UINT = WM_MOUSELEAVE.convert()
val _WM_LBUTTONDOWN: UINT = WM_LBUTTONDOWN.convert()
val _WM_MBUTTONDOWN: UINT = WM_MBUTTONDOWN.convert()
val _WM_RBUTTONDOWN: UINT = WM_RBUTTONDOWN.convert()
val _WM_LBUTTONUP: UINT = WM_LBUTTONUP.convert()
val _WM_MBUTTONUP: UINT = WM_MBUTTONUP.convert()
val _WM_RBUTTONUP: UINT = WM_RBUTTONUP.convert()
val _WM_KEYDOWN: UINT = WM_KEYDOWN.convert()
val _WM_KEYUP: UINT = WM_KEYUP.convert()
val _WM_CLOSE: UINT = WM_CLOSE.convert()

@Suppress("UNUSED_PARAMETER")
fun WndProc(hWnd: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    //println("WndProc: $hWnd, $message, $wParam, $lParam")
    when (message) {
        _WM_CREATE -> {
            memScoped {
                val pfd = alloc<PIXELFORMATDESCRIPTOR>()
                pfd.nSize = PIXELFORMATDESCRIPTOR.size.convert()
                pfd.nVersion = 1.convert()
                pfd.dwFlags = (PFD_DRAW_TO_WINDOW or PFD_SUPPORT_OPENGL or PFD_DOUBLEBUFFER).convert()
                pfd.iPixelType = PFD_TYPE_RGBA.convert()
                pfd.cColorBits = 32.convert()
                pfd.cDepthBits = 24.convert()
                pfd.cStencilBits = 8.convert()
                pfd.iLayerType = PFD_MAIN_PLANE.convert()
                val hDC = GetDC(hWnd)
                val letWindowsChooseThisPixelFormat = ChoosePixelFormat(hDC, pfd.ptr)

                SetPixelFormat(hDC, letWindowsChooseThisPixelFormat, pfd.ptr)
                windowsGameWindow.glRenderContext = wglCreateContext(hDC)
                wglMakeCurrent(hDC, windowsGameWindow.glRenderContext)

                val wglSwapIntervalEXT = wglGetProcAddressAny("wglSwapIntervalEXT")!!
                    .reinterpret<CFunction<Function1<Int, Int>>>()

                println("wglSwapIntervalEXT: $wglSwapIntervalEXT")
                wglSwapIntervalEXT?.invoke(0)

                println("GL_CONTEXT: ${windowsGameWindow.glRenderContext}")

                windowsGameWindow.ag.__ready()
            }
        }
        _WM_SIZE -> {
            var width = 0
            var height = 0
            memScoped {
                val rect = alloc<RECT>()
                GetClientRect(hWnd, rect.ptr)
                width = (rect.right - rect.left).convert()
                height = (rect.bottom - rect.top).convert()
            }
            //val width = (lParam.toInt() ushr 0) and 0xFFFF
            //val height = (lParam.toInt() ushr 16) and 0xFFFF
            windowsGameWindow.resized(width, height)
        }
        _WM_QUIT -> {
            kotlin.system.exitProcess(0.convert())
        }
        _WM_MOUSEMOVE -> {
            val x = (lParam.toInt() ushr 0) and 0xFFFF
            val y = (lParam.toInt() ushr 16) and 0xFFFF
            mouseMove(x, y, wParam.toInt())
        }
        _WM_LBUTTONDOWN -> mouseButton(0, true, wParam.toInt())
        _WM_MBUTTONDOWN -> mouseButton(1, true, wParam.toInt())
        _WM_RBUTTONDOWN -> mouseButton(2, true, wParam.toInt())
        _WM_LBUTTONUP -> mouseButton(0, false, wParam.toInt())
        _WM_MBUTTONUP -> mouseButton(1, false, wParam.toInt())
        _WM_RBUTTONUP -> mouseButton(2, false, wParam.toInt())
        _WM_KEYDOWN -> windowsGameWindow.keyUpdate(wParam.toInt(), true)
        _WM_KEYUP -> windowsGameWindow.keyUpdate(wParam.toInt(), false)
        _WM_CLOSE -> {
            kotlin.system.exitProcess(0)
        }
    }
    return DefWindowProcW(hWnd, message, wParam, lParam)
}


val COMDLG32_DLL: HMODULE? by lazy { LoadLibraryA("comdlg32.dll") }

val GetOpenFileNameWFunc by lazy {
    GetProcAddress(COMDLG32_DLL, "GetOpenFileNameW") as CPointer<CFunction<Function1<CPointer<OPENFILENAMEW>, BOOL>>>
}

data class FileFilter(val name: String, val pattern: String)

fun openSelectFile(
    initialDir: String? = null,
    filters: List<FileFilter> = listOf(FileFilter("All (*.*)", "*.*")),
    hwnd: HWND? = null
): String? = memScoped {
    val szFileSize = 1024
    val szFile = allocArray<WCHARVar>(szFileSize + 1)
    val ofn = alloc<OPENFILENAMEW>().apply {
        lStructSize = OPENFILENAMEW.size.convert()
        hwndOwner = hwnd
        lpstrFile = szFile.reinterpret()
        nMaxFile = szFileSize.convert()
        lpstrFilter =
            (filters.flatMap { listOf(it.name, it.pattern) }.joinToString("\u0000") + "\u0000").wcstr.ptr.reinterpret()
        nFilterIndex = 1.convert()
        lpstrFileTitle = null
        nMaxFileTitle = 0.convert()
        lpstrInitialDir = if (initialDir != null) initialDir.wcstr.ptr.reinterpret() else null
        Flags = (OFN_PATHMUSTEXIST or OFN_FILEMUSTEXIST).convert()
    }
    val res = GetOpenFileNameWFunc(ofn.ptr.reinterpret())
    if (res.toInt() != 0) szFile.reinterpret<ShortVar>().toKString() else null
}

@ThreadLocal
private var mouseX: Int = 0

@ThreadLocal
private var mouseY: Int = 0

//@ThreadLocal
//private val buttons = BooleanArray(16)

val MK_LBUTTON = 0x0001
val MK_RBUTTON = 0x0002
val MK_SHIFT = 0x0004
val MK_CONTROL = 0x0008
val MK_MBUTTON = 0x0010
val MK_XBUTTON1 = 0x0020
val MK_XBUTTON2 = 0x0040

fun mouseMove(x: Int, y: Int, wParam: Int) {
    mouseX = x
    mouseY = y
    SetCursor(ARROW_CURSOR)

    val anyButton = (wParam and (MK_LBUTTON or MK_RBUTTON or MK_MBUTTON or MK_XBUTTON1 or MK_XBUTTON2)) != 0

    windowsGameWindow.mouseEvent(
        if (anyButton) com.soywiz.korev.MouseEvent.Type.DRAG else com.soywiz.korev.MouseEvent.Type.MOVE,
        mouseX, mouseY, 0, wParam
    )
}

fun mouseButton(button: Int, down: Boolean, wParam: Int) {
    //buttons[button] = down
    if (down) {
        windowsGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.DOWN, mouseX, mouseY, button, wParam)
    } else {
        windowsGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.UP, mouseX, mouseY, button, wParam)
        windowsGameWindow.mouseEvent(
            com.soywiz.korev.MouseEvent.Type.CLICK,
            mouseX,
            mouseY,
            button,
            wParam
        ) // @TODO: Conditionally depending on the down x,y & time
    }
}

//val String.glstr: CPointer<GLcharVar> get() = this.cstr.reinterpret()
val OPENGL32_DLL_MODULE: HMODULE? by lazy { LoadLibraryA("opengl32.dll") }

fun wglGetProcAddressAny(name: String): PROC? {
    return wglGetProcAddress(name)
        ?: GetProcAddress(OPENGL32_DLL_MODULE, name)
        ?: throw RuntimeException("Can't find GL function: '$name'")
}

val USER32_DLL by lazy { LoadLibraryA("User32.dll") }
val LoadCursorAFunc by lazy {
    GetProcAddress(
        USER32_DLL,
        "LoadCursorA"
    )!!.reinterpret<CFunction<Function2<Int, Int, HCURSOR?>>>()
}

val LoadIconAFunc by lazy {
    GetProcAddress(
        USER32_DLL,
        "LoadIconA"
    )!!.reinterpret<CFunction<Function2<HMODULE?, Int, HICON?>>>()
}

val FindResourceAFunc by lazy {
    GetProcAddress(
        USER32_DLL,
        "FindResourceA"
    )!!.reinterpret<CFunction<Function2<HMODULE?, Int, HICON?>>>()
}

//val ARROW_CURSOR by lazy { LoadCursorA(null, 32512.reinterpret<CPointer<ByteVar>>().reinterpret()) }
val ARROW_CURSOR: HICON? by lazy { LoadCursorAFunc(0, 32512)!! }
