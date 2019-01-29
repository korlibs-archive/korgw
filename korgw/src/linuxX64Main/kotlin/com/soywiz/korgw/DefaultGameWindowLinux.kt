package com.soywiz.korgw

import GL.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class GlutGameWindow : GameWindow() {
    val agNativeComponent = Any()
    override val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

    override var fps: Int
        get() = super.fps
        set(value) {}

    override var title: String = ""
        set(value) {
            field = value
            glutSetWindowTitle(field)
        }

    private val screenWidth get() = glutGet(GLUT_SCREEN_WIDTH)
    private val screenHeight get() = glutGet(GLUT_SCREEN_HEIGHT)

    override val width: Int get() = glutGet(GLUT_WINDOW_WIDTH)
    override val height: Int get() = glutGet(GLUT_WINDOW_HEIGHT)

    private var widthInternal = 640
    private var heightInternal = 480

    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    widthInternal = width
                    heightInternal = height
                    glutFullScreen()
                } else {
                    setSizeInternal(widthInternal, heightInternal)
                }
            }
        }
    override var visible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    glutShowWindow()
                    setSize(widthInternal, heightInternal)
                } else {
                    widthInternal = width
                    heightInternal = height
                    glutHideWindow()
                }
            }
        }
    override var quality: Quality = Quality.AUTOMATIC

    override fun setSize(width: Int, height: Int) {
        widthInternal = width
        heightInternal = height
        setSizeInternal(width, height)
    }

    private fun setSizeInternal(width: Int, height: Int) {
        glutReshapeWindow(width, height)
        glutPositionWindow((screenWidth - width) / 2, (screenHeight - height) / 2)
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

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        memScoped {
            val argc = alloc<IntVar>().apply { value = 0 }
            glutInit(argc.ptr, null) // TODO: pass real args
        }

        glutInitDisplayMode((GLUT_RGB or GLUT_DOUBLE or GLUT_DEPTH).convert())
        glutInitWindowSize(640, 480)
        glutCreateWindow("")
        glutHideWindow()

        glutReshapeFunc(staticCFunction(::glutReshape))
        glutDisplayFunc(staticCFunction(::glutDisplay))
        glutIdleFunc(staticCFunction(::glutDisplay))
        glutMotionFunc(staticCFunction(::glutMouseMove))
        glutPassiveMotionFunc(staticCFunction(::glutMouseMove))
        glutMouseFunc(staticCFunction(::glutMouse))
        glutKeyboardFunc(staticCFunction(::glutKeyDown))
        glutKeyboardFunc(staticCFunction(::glutKeyUp))

        ag.__ready()
        var running = true
        CoroutineScope(coroutineContext).launch(coroutineDispatcher) {
            try {
                entry()
            } catch (e: Throwable) {
                println(e)
                running = false
            }
        }

        glutMainLoop()
    }

    fun display() {
        coroutineDispatcher.executePending()
        ag.onRender(ag)
        dispatch(renderEvent)
        glutSwapBuffers()
    }

    fun reshape(width: Int, height: Int) {
        ag.resized(width, height)
        dispatch(reshapeEvent {
            this.width = width
            this.height = height
        })
        glutDisplay()
    }

    fun mouseEvent(etype: com.soywiz.korev.MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
        dispatch(mouseEvent {
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

    fun glutKeyUpDown(key: UByte, pressed: Boolean) {
        val key = KeyCodesToKeys[key.toInt()] ?: CharToKeys[key.toInt().toChar()] ?: Key.UNKNOWN
        //println("keyDownUp: char=$char, modifiers=$modifiers, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed")
        dispatch(keyEvent.apply {
            this.type =
                if (pressed) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
            this.id = 0
            this.key = key
            this.keyCode = keyCode
            this.character = char
        })
    }
}

val glutGameWindow = com.soywiz.korgw.GlutGameWindow()
actual val DefaultGameWindow: GameWindow = glutGameWindow

fun glutDisplay() = glutGameWindow.display()
fun glutReshape(width: Int, height: Int) = glutGameWindow.reshape(width, height)
fun glutMouseMove(x: Int, y: Int) {
    glutGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.MOVE, x, y, 0)
}

fun glutMouse(button: Int, state: Int, x: Int, y: Int) {
    val up = state == GLUT_UP
    val event = if (up) {
        com.soywiz.korev.MouseEvent.Type.UP
    } else {
        com.soywiz.korev.MouseEvent.Type.DOWN
    }
    glutGameWindow.mouseEvent(event, x, y, button)
    if (up) {
        glutGameWindow.mouseEvent(com.soywiz.korev.MouseEvent.Type.CLICK, x, y, button)
    }
}

fun glutKeyDown(key: UByte, x: Int, y: Int) {
    glutGameWindow.glutKeyUpDown(key, true)
}

fun glutKeyUp(key: UByte, x: Int, y: Int) {
    glutGameWindow.glutKeyUpDown(key, false)
}
