package com.soywiz.korgw

import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import org.w3c.dom.events.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.*

private external val navigator: dynamic

class BrowserGameWindow : GameWindow() {
    override val ag: AGWebgl = AGWebgl(AGConfig())
    val canvas get() = ag.canvas

    fun is_touch_device(): Boolean = try {
        document.createEvent("TouchEvent")
        true
    } catch (e: dynamic) {
        false
    }

    init {
        window.asDynamic().canvas = canvas
        window.asDynamic().ag = ag
        window.asDynamic().gl = ag.gl
        document.body?.appendChild(canvas)
        document.body?.style?.margin = "0px"
        document.body?.style?.padding = "0px"
        document.body?.style?.overflowX = "hidden"
        document.body?.style?.overflowY = "hidden"
        canvas.addEventListener("mouseenter", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.ENTER) })
        canvas.addEventListener("mouseleave", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseover", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.MOVE) })
        canvas.addEventListener("mousemove", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.MOVE) })
        canvas.addEventListener("mouseout", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseup", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.UP) })
        canvas.addEventListener("mousedown", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.DOWN) })
        canvas.addEventListener("click", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.CLICK) })

        canvas.addEventListener("touchstart", { touchEvent(it, com.soywiz.korev.TouchEvent.Type.START) })
        canvas.addEventListener("touchmove", { touchEvent(it, com.soywiz.korev.TouchEvent.Type.MOVE) })
        canvas.addEventListener("touchend", { touchEvent(it, com.soywiz.korev.TouchEvent.Type.END) })
        //canvas.addEventListener("touchcancel", { touchEvent(it, com.soywiz.korev.TouchEvent.Type.CANCEL) })

        window.addEventListener("keypress", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keydown", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keyup", { keyEvent(it.unsafeCast<KeyboardEvent>()) })

        //	val info = GamePadButtonEvent()

        @Suppress("UNUSED_PARAMETER")
        fun frame(e: Double) {
            try {
                window.requestAnimationFrame(::frame)
                if (navigator.getGamepads != null) {
                    val gamepads = navigator.getGamepads().unsafeCast<JsArray<JsGamePad?>>()
                    for (gp in gamePadUpdateEvent.gamepads) gp.connected = false
                    gamePadUpdateEvent.gamepadsLength = gamepads.length
                    for (gamepadId in 0 until gamepads.length) {
                        val controller = gamepads[gamepadId] ?: continue
                        val gamepad = gamePadUpdateEvent.gamepads.getOrNull(gamepadId) ?: continue
                        val mapping = knownControllers[controller.id] ?: knownControllers[controller.mapping] ?: StandardGamepadMapping
                        gamepad.apply {
                            this.connected = controller.connected
                            this.index = controller.index
                            this.name = controller.id
                            this.mapping = mapping
                            this.axesLength = controller.axes.length
                            this.buttonsLength = controller.buttons.length
                            this.rawButtonsPressed = 0
                            for (n in 0 until controller.buttons.length) {
                                val button = controller.buttons[n]
                                if (button.pressed) this.rawButtonsPressed = this.rawButtonsPressed or (1 shl n)
                                this.rawButtonsPressure[n] = button.value
                            }
                            for (n in 0 until controller.axes.length) {
                                this.rawAxes[n] = controller.axes[n]
                            }
                        }
                    }
                    dispatch(gamePadUpdateEvent)
                }
            } catch (e: dynamic) {
                console.error(e)
            }
        }
        //frame(0.0)
        window.requestAnimationFrame(::frame)

        window.addEventListener("gamepadconnected", { e ->
            //console.log("gamepadconnected")
            val e = e.unsafeCast<JsGamepadEvent>()
            dispatch(gamePadConnectionEvent.apply {
                this.type = GamePadConnectionEvent.Type.CONNECTED
                this.gamepad = e.gamepad.index
            })
        })
        window.addEventListener("gamepaddisconnected", { e ->
            //console.log("gamepaddisconnected")
            val e = e.unsafeCast<JsGamepadEvent>()
            dispatch(gamePadConnectionEvent.apply {
                this.type = GamePadConnectionEvent.Type.DISCONNECTED
                this.gamepad = e.gamepad.index
            })
        })


        window.addEventListener("resize", { onResized() })
        onResized()
    }

    override var quality: Quality = Quality.AUTOMATIC
        set(value) {
            if (field != value) {
                field = value
                onResized()
            }
        }

    private fun onResized() {
        val doQuality = quality == GameWindow.Quality.QUALITY
        val scale = if (doQuality) ag.devicePixelRatio.toInt() else 1
        canvas.width = window.innerWidth * scale
        canvas.height = window.innerHeight * scale
        canvas.style.position = "absolute"
        canvas.style.left = "0"
        canvas.style.right = "0"
        canvas.style.width = "${window.innerWidth}px"
        canvas.style.height = "${window.innerHeight}px"
        //ag.resized(canvas.width, canvas.height)
        //dispatchReshapeEvent(0, 0, window.innerWidth, window.innerHeight)
        dispatchReshapeEvent(0, 0, canvas.width, canvas.height)
    }

    private fun doRender() {
        ag.onRender(ag)
        dispatch(renderEvent)
    }

    private fun keyEvent(me: KeyboardEvent) {
        dispatch(keyEvent {
            this.type = when (me.type) {
                "keydown" -> KeyEvent.Type.DOWN
                "keyup" -> KeyEvent.Type.UP
                "keypress" -> KeyEvent.Type.TYPE
                else -> error("Unsupported event type ${me.type}")
            }
            this.id = 0
            this.keyCode = me.keyCode
            this.key = when (me.key) {
                "0" -> Key.N0; "1" -> Key.N1; "2" -> Key.N2; "3" -> Key.N3
                "4" -> Key.N4; "5" -> Key.N5; "6" -> Key.N6; "7" -> Key.N7
                "8" -> Key.N8; "9" -> Key.N9
                "a" -> Key.A; "b" -> Key.B; "c" -> Key.C; "d" -> Key.D
                "e" -> Key.E; "f" -> Key.F; "g" -> Key.G; "h" -> Key.H
                "i" -> Key.I; "j" -> Key.J; "k" -> Key.K; "l" -> Key.L
                "m" -> Key.M; "n" -> Key.N; "o" -> Key.O; "p" -> Key.P
                "q" -> Key.Q; "r" -> Key.R; "s" -> Key.S; "t" -> Key.T
                "u" -> Key.U; "v" -> Key.V; "w" -> Key.W; "x" -> Key.X
                "y" -> Key.Y; "z" -> Key.Z
                "F1" -> Key.F1; "F2" -> Key.F2; "F3" -> Key.F3; "F4" -> Key.F4
                "F5" -> Key.F5; "F6" -> Key.F6; "F7" -> Key.F7; "F8" -> Key.F8
                "F9" -> Key.F9; "F10" -> Key.F10; "F11" -> Key.F11; "F12" -> Key.F12
                "F13" -> Key.F13; "F14" -> Key.F14; "F15" -> Key.F15; "F16" -> Key.F16
                "F17" -> Key.F17; "F18" -> Key.F18; "F19" -> Key.F19; "F20" -> Key.F20
                "F21" -> Key.F21; "F22" -> Key.F22; "F23" -> Key.F23; "F24" -> Key.F24
                "F25" -> Key.F25
                else -> when (me.code) {
                    "MetaLeft" -> Key.LEFT_SUPER
                    "MetaRight" -> Key.RIGHT_SUPER
                    "ShiftLeft" -> Key.LEFT_SHIFT
                    "ShiftRight" -> Key.RIGHT_SHIFT
                    "ControlLeft" -> Key.LEFT_CONTROL
                    "ControlRight" -> Key.RIGHT_CONTROL
                    "AltLeft" -> Key.LEFT_ALT
                    "AltRight" -> Key.RIGHT_ALT
                    "Space" -> Key.SPACE
                    "ArrowUp" -> Key.UP
                    "ArrowDown" -> Key.DOWN
                    "ArrowLeft" -> Key.LEFT
                    "ArrowRight" -> Key.RIGHT
                    "Enter" -> Key.ENTER
                    "Escape" -> Key.ESCAPE
                    "Backspace" -> Key.BACKSPACE
                    "Period" -> Key.PERIOD
                    "Comma" -> Key.COMMA
                    "Semicolon" -> Key.SEMICOLON
                    "Slash" -> Key.SLASH
                    "Tab" -> Key.TAB
                    else -> Key.UNKNOWN
                }
            }
            this.character = me.charCode.toChar()
        })
    }

    private fun touchEvent(e: dynamic, type: com.soywiz.korev.TouchEvent.Type) {
        touchEvent.startFrame(type)
        for (n in 0 until e.touches.length) {
            val touch = e.touches.item(n)
            touchEvent.touch(touch.identifier, touch.clientX, touch.clientY)
        }
        dispatch(touchEvent)
    }

    private fun mouseEvent(e: MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
        if (!is_touch_device()) {
            dispatch(mouseEvent {
                this.type = type
                this.id = 0
                this.x = e.clientX
                this.y = e.clientY
                this.button = MouseButton[e.button.toInt()]
                this.buttons = e.buttons.toInt()
            })
        }
    }

    override var title: String
        get() = document.title
        set(value) { document.title = value }
    override val width: Int get() = canvas.clientWidth
    override val height: Int get() = canvas.clientHeight
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean
        get() = document.fullscreenElement != null
        set(value) {
            if (fullscreen != value) {
                if (value) {
                    canvas.requestFullscreen()
                } else {
                    document.exitFullscreen()
                }
            }
        }
    override var visible: Boolean
        get() = canvas.style.visibility == "visible"
        set(value) = run { canvas.style.visibility = if (value) "visible" else "hidden" }

    override fun setSize(width: Int, height: Int) {
        // Do nothing!
    }

    override suspend fun browse(url: URL) {
        document.open(url.fullUrl)
    }

    override suspend fun alert(message: String) {
        window.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return window.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return window.prompt(message, default) ?: throw CancellationException("cancelled")
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        TODO()
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(coroutineDispatcher) {
            entry()
        }
        frame(0.0)
    }

    private fun frame(step: Double) {
        coroutineDispatcher.executePending()
        doRender()
        window.requestAnimationFrame(::frame)
    }
}

private external class JsArray<T> {
    val length: Int
}

private inline operator fun <T> JsArray<T>.get(index: Int): T = this.asDynamic()[index]

private external class JsGamepadButton {
    val value: Double
    val pressed: Boolean
}

private external class JsGamePad {
    val axes: JsArray<Double>
    val buttons: JsArray<JsGamepadButton>
    val connected: Boolean
    val id: String
    val index: Int
    val mapping: String
    val timestamp: Double
}

private external class JsGamepadEvent : Event {
    val gamepad: JsGamePad
}

class NodeJsGameWindow : GameWindow() {
}

actual val DefaultGameWindow: GameWindow = if (OS.isJsNodeJs) NodeJsGameWindow() else BrowserGameWindow()

/*
public external open class TouchEvent(type: String, eventInitDict: MouseEventInit = definedExternally) : UIEvent {
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val ctrlKey: Boolean
    open val metaKey: Boolean

    open val changedTouches: TouchList
    open val touches: TouchList
    open val targetTouches: TouchList
}

external class TouchList {
    val length: Int
    fun item(index: Int): Touch
}

external class Touch {
    val identifier: Int
    val screenX: Int
    val screenY: Int
    val clientX: Int
    val clientY: Int
    val pageX: Int
    val pageY: Int
    val target: dynamic
}
*/
object Nimbus_111_1420_Safari_GamepadMapping : GamepadMapping() {
    override val id = "111-1420-Nimbus"

    override fun get(button: GameButton, info: GamepadInfo): Double {
        return when (button) {
            GameButton.BUTTON0 -> info.getRawButton(0)
            GameButton.BUTTON1 -> info.getRawButton(1)
            GameButton.BUTTON2 -> info.getRawButton(2)
            GameButton.BUTTON3 -> info.getRawButton(3)
            GameButton.L1 -> info.getRawButton(4)
            GameButton.R1 -> info.getRawButton(5)
            GameButton.L2 -> info.getRawButton(6)
            GameButton.R2 -> info.getRawButton(7)
            GameButton.LEFT -> info.getRawButton(8)
            GameButton.DOWN -> info.getRawButton(9)
            GameButton.RIGHT -> info.getRawButton(10)
            GameButton.UP -> info.getRawButton(11)
            GameButton.SELECT -> 0.0
            GameButton.START -> 0.0
            GameButton.SYSTEM -> 0.0
            GameButton.LX -> info.getRawAxe(0)
            GameButton.LY -> info.getRawAxe(1)
            GameButton.RX -> info.getRawAxe(2)
            GameButton.RY -> info.getRawAxe(3)
            else -> 0.0
        }
    }
}

val knownControllers = listOf(
    StandardGamepadMapping,
    Nimbus_111_1420_Safari_GamepadMapping
).associateBy { it.id }
