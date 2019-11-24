package com.soywiz.korgw

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.util.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*


actual fun CreateDefaultGameWindow(): GameWindow = object : GameWindow() {
    override val ag: AGAwt by lazy {
        AGAwt(AGConfig(antialiasHint = (quality != GameWindow.Quality.PERFORMANCE)))
    }

    val frame = object : JFrame() {
        init {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            contentPane.add(ag.glcanvas)
            ag.glcanvas.requestFocusInWindow()
        }

        override fun createRootPane(): JRootPane = super.createRootPane().apply {
            putClientProperty("apple.awt.fullscreenable", true)
        }
    }

    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override var width: Int = 0; private set
    override var height: Int = 0; private set

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        frame.contentPane.preferredSize = Dimension(width, height)
        //frame.setSize(width, height)
        frame.pack()
        frame.setLocationRelativeTo(null)

        //println(ag.glcanvas.size)

        //val screenSize = Toolkit.getDefaultToolkit().screenSize
        //frame.setPosition(
        //    (screenSize.width - value.width) / 2,
        //    (screenSize.height - value.height) / 2
        //)
        //window.setLocationRelativeTo(null)
    }

    override var icon: Bitmap?
        get() = super.icon
        set(value) {
        }
    override var fullscreen: Boolean
        //get() = (frame.extendedState and JFrame.MAXIMIZED_BOTH) != 0
        get() = if (OS.isMac) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val frameSize = frame.size
            //println("screenSize=$screenSize, frameSize=$frameSize")
            (screenSize == frameSize)
        } else {
            frame.graphicsConfiguration.device.fullScreenWindow == frame
        }
        set(value) {
            if (OS.isMac) {
                if (fullscreen != value) {
                    KDynamic {
                        global["com.apple.eawt.Application"].dynamicInvoke("getApplication")
                            .dynamicInvoke("requestToggleFullScreen", frame)
                    }
                }
            } else {
                frame.graphicsConfiguration.device.fullScreenWindow = if (value) frame else null
            }
            /*
            if (value) {
                frame.extendedState = frame.extendedState or JFrame.MAXIMIZED_BOTH
            } else {
                frame.extendedState = frame.extendedState and JFrame.MAXIMIZED_BOTH.inv()
            }
            */
        }
    override var visible: Boolean
        get() = frame.isVisible
        set(value) {
            frame.isVisible = value
        }

    private fun dispatchME(e: java.awt.event.MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
        dispatch(mouseEvent {
            this.type = type
            this.id = 0
            this.x = e.x
            this.y = e.y
            this.button = MouseButton[e.button]
        })
    }

    private fun dispatchKE(e: java.awt.event.KeyEvent, type: com.soywiz.korev.KeyEvent.Type) {
        dispatch(keyEvent {
            this.type = type
            this.id = 0
            this.keyCode = e.keyCode
            this.character = e.keyChar
            this.key = when (e.keyCode) {
                KeyEvent.VK_ENTER          -> Key.ENTER
                KeyEvent.VK_BACK_SPACE     -> Key.BACKSPACE
                KeyEvent.VK_TAB            -> Key.TAB
                KeyEvent.VK_CANCEL         -> Key.CANCEL
                KeyEvent.VK_CLEAR          -> Key.CLEAR
                KeyEvent.VK_SHIFT          -> Key.LEFT_SHIFT
                KeyEvent.VK_CONTROL        -> Key.LEFT_CONTROL
                KeyEvent.VK_ALT            -> Key.LEFT_ALT
                KeyEvent.VK_PAUSE          -> Key.PAUSE
                KeyEvent.VK_CAPS_LOCK      -> Key.CAPS_LOCK
                KeyEvent.VK_ESCAPE         -> Key.ESCAPE
                KeyEvent.VK_SPACE          -> Key.SPACE
                KeyEvent.VK_PAGE_UP        -> Key.PAGE_UP
                KeyEvent.VK_PAGE_DOWN      -> Key.PAGE_DOWN
                KeyEvent.VK_END            -> Key.END
                KeyEvent.VK_HOME           -> Key.HOME
                KeyEvent.VK_LEFT           -> Key.LEFT
                KeyEvent.VK_UP             -> Key.UP
                KeyEvent.VK_RIGHT          -> Key.RIGHT
                KeyEvent.VK_DOWN           -> Key.DOWN
                KeyEvent.VK_COMMA          -> Key.COMMA
                KeyEvent.VK_MINUS          -> Key.MINUS
                KeyEvent.VK_PERIOD         -> Key.PERIOD
                KeyEvent.VK_SLASH          -> Key.SLASH
                KeyEvent.VK_0              -> Key.N0
                KeyEvent.VK_1              -> Key.N1
                KeyEvent.VK_2              -> Key.N2
                KeyEvent.VK_3              -> Key.N3
                KeyEvent.VK_4              -> Key.N4
                KeyEvent.VK_5              -> Key.N5
                KeyEvent.VK_6              -> Key.N6
                KeyEvent.VK_7              -> Key.N7
                KeyEvent.VK_8              -> Key.N8
                KeyEvent.VK_9              -> Key.N9
                KeyEvent.VK_SEMICOLON      -> Key.SEMICOLON
                KeyEvent.VK_EQUALS         -> Key.EQUAL
                KeyEvent.VK_A              -> Key.A
                KeyEvent.VK_B              -> Key.B
                KeyEvent.VK_C              -> Key.C
                KeyEvent.VK_D              -> Key.D
                KeyEvent.VK_E              -> Key.E
                KeyEvent.VK_F              -> Key.F
                KeyEvent.VK_G              -> Key.G
                KeyEvent.VK_H              -> Key.H
                KeyEvent.VK_I              -> Key.I
                KeyEvent.VK_J              -> Key.J
                KeyEvent.VK_K              -> Key.K
                KeyEvent.VK_L              -> Key.L
                KeyEvent.VK_M              -> Key.M
                KeyEvent.VK_N              -> Key.N
                KeyEvent.VK_O              -> Key.O
                KeyEvent.VK_P              -> Key.P
                KeyEvent.VK_Q              -> Key.Q
                KeyEvent.VK_R              -> Key.R
                KeyEvent.VK_S              -> Key.S
                KeyEvent.VK_T              -> Key.T
                KeyEvent.VK_U              -> Key.U
                KeyEvent.VK_V              -> Key.V
                KeyEvent.VK_W              -> Key.W
                KeyEvent.VK_X              -> Key.X
                KeyEvent.VK_Y              -> Key.Y
                KeyEvent.VK_Z              -> Key.Z
                KeyEvent.VK_OPEN_BRACKET   -> Key.OPEN_BRACKET
                KeyEvent.VK_BACK_SLASH     -> Key.BACKSLASH
                KeyEvent.VK_CLOSE_BRACKET  -> Key.CLOSE_BRACKET
                KeyEvent.VK_NUMPAD0        -> Key.NUMPAD0
                KeyEvent.VK_NUMPAD1        -> Key.NUMPAD1
                KeyEvent.VK_NUMPAD2        -> Key.NUMPAD2
                KeyEvent.VK_NUMPAD3        -> Key.NUMPAD3
                KeyEvent.VK_NUMPAD4        -> Key.NUMPAD4
                KeyEvent.VK_NUMPAD5        -> Key.NUMPAD5
                KeyEvent.VK_NUMPAD6        -> Key.NUMPAD6
                KeyEvent.VK_NUMPAD7        -> Key.NUMPAD7
                KeyEvent.VK_NUMPAD8        -> Key.NUMPAD8
                KeyEvent.VK_NUMPAD9        -> Key.NUMPAD9
                KeyEvent.VK_MULTIPLY       -> Key.KP_MULTIPLY
                KeyEvent.VK_ADD            -> Key.KP_ADD
                KeyEvent.VK_SEPARATER      -> Key.KP_SEPARATOR
                //KeyEvent.VK_SEPARATOR      -> Key.KP_SEPARATOR
                KeyEvent.VK_SUBTRACT       -> Key.KP_SUBTRACT
                KeyEvent.VK_DECIMAL        -> Key.KP_DECIMAL
                KeyEvent.VK_DIVIDE         -> Key.KP_DIVIDE
                KeyEvent.VK_DELETE         -> Key.DELETE
                KeyEvent.VK_NUM_LOCK       -> Key.NUM_LOCK
                KeyEvent.VK_SCROLL_LOCK    -> Key.SCROLL_LOCK
                KeyEvent.VK_F1             -> Key.F1
                KeyEvent.VK_F2             -> Key.F2
                KeyEvent.VK_F3             -> Key.F3
                KeyEvent.VK_F4             -> Key.F4
                KeyEvent.VK_F5             -> Key.F5
                KeyEvent.VK_F6             -> Key.F6
                KeyEvent.VK_F7             -> Key.F7
                KeyEvent.VK_F8             -> Key.F8
                KeyEvent.VK_F9             -> Key.F9
                KeyEvent.VK_F10            -> Key.F10
                KeyEvent.VK_F11            -> Key.F11
                KeyEvent.VK_F12            -> Key.F12
                KeyEvent.VK_F13            -> Key.F13
                KeyEvent.VK_F14            -> Key.F14
                KeyEvent.VK_F15            -> Key.F15
                KeyEvent.VK_F16            -> Key.F16
                KeyEvent.VK_F17            -> Key.F17
                KeyEvent.VK_F18            -> Key.F18
                KeyEvent.VK_F19            -> Key.F19
                KeyEvent.VK_F20            -> Key.F20
                KeyEvent.VK_F21            -> Key.F21
                KeyEvent.VK_F22            -> Key.F22
                KeyEvent.VK_F23            -> Key.F23
                KeyEvent.VK_F24            -> Key.F24
                KeyEvent.VK_PRINTSCREEN    -> Key.PRINT_SCREEN
                KeyEvent.VK_INSERT         -> Key.INSERT
                KeyEvent.VK_HELP           -> Key.HELP
                KeyEvent.VK_META           -> Key.META
                KeyEvent.VK_BACK_QUOTE     -> Key.BACKQUOTE
                KeyEvent.VK_QUOTE          -> Key.QUOTE
                KeyEvent.VK_KP_UP          -> Key.KP_UP
                KeyEvent.VK_KP_DOWN        -> Key.KP_DOWN
                KeyEvent.VK_KP_LEFT        -> Key.KP_LEFT
                KeyEvent.VK_KP_RIGHT       -> Key.KP_RIGHT
                //KeyEvent.VK_DEAD_GRAVE               -> Key.DEAD_GRAVE
                //KeyEvent.VK_DEAD_ACUTE               -> Key.DEAD_ACUTE
                //KeyEvent.VK_DEAD_CIRCUMFLEX          -> Key.DEAD_CIRCUMFLEX
                //KeyEvent.VK_DEAD_TILDE               -> Key.DEAD_TILDE
                //KeyEvent.VK_DEAD_MACRON              -> Key.DEAD_MACRON
                //KeyEvent.VK_DEAD_BREVE               -> Key.DEAD_BREVE
                //KeyEvent.VK_DEAD_ABOVEDOT            -> Key.DEAD_ABOVEDOT
                //KeyEvent.VK_DEAD_DIAERESIS           -> Key.DEAD_DIAERESIS
                //KeyEvent.VK_DEAD_ABOVERING           -> Key.DEAD_ABOVERING
                //KeyEvent.VK_DEAD_DOUBLEACUTE         -> Key.DEAD_DOUBLEACUTE
                //KeyEvent.VK_DEAD_CARON               -> Key.DEAD_CARON
                //KeyEvent.VK_DEAD_CEDILLA             -> Key.DEAD_CEDILLA
                //KeyEvent.VK_DEAD_OGONEK              -> Key.DEAD_OGONEK
                //KeyEvent.VK_DEAD_IOTA                -> Key.DEAD_IOTA
                //KeyEvent.VK_DEAD_VOICED_SOUND        -> Key.DEAD_VOICED_SOUND
                //KeyEvent.VK_DEAD_SEMIVOICED_SOUND    -> Key.DEAD_SEMIVOICED_SOUND
                //KeyEvent.VK_AMPERSAND                -> Key.AMPERSAND
                //KeyEvent.VK_ASTERISK                 -> Key.ASTERISK
                //KeyEvent.VK_QUOTEDBL                 -> Key.QUOTEDBL
                //KeyEvent.VK_LESS                     -> Key.LESS
                //KeyEvent.VK_GREATER                  -> Key.GREATER
                //KeyEvent.VK_BRACELEFT                -> Key.BRACELEFT
                //KeyEvent.VK_BRACERIGHT               -> Key.BRACERIGHT
                //KeyEvent.VK_AT                       -> Key.AT
                //KeyEvent.VK_COLON                    -> Key.COLON
                //KeyEvent.VK_CIRCUMFLEX               -> Key.CIRCUMFLEX
                //KeyEvent.VK_DOLLAR                   -> Key.DOLLAR
                //KeyEvent.VK_EURO_SIGN                -> Key.EURO_SIGN
                //KeyEvent.VK_EXCLAMATION_MARK         -> Key.EXCLAMATION_MARK
                //KeyEvent.VK_INVERTED_EXCLAMATION_MARK -> Key.INVERTED_EXCLAMATION_MARK
                //KeyEvent.VK_LEFT_PARENTHESIS         -> Key.LEFT_PARENTHESIS
                //KeyEvent.VK_NUMBER_SIGN              -> Key.NUMBER_SIGN
                //KeyEvent.VK_PLUS                     -> Key.PLUS
                //KeyEvent.VK_RIGHT_PARENTHESIS        -> Key.RIGHT_PARENTHESIS
                //KeyEvent.VK_UNDERSCORE               -> Key.UNDERSCORE
                //KeyEvent.VK_WINDOWS                  -> Key.WINDOWS
                //KeyEvent.VK_CONTEXT_MENU             -> Key.CONTEXT_MENU
                //KeyEvent.VK_FINAL                    -> Key.FINAL
                //KeyEvent.VK_CONVERT                  -> Key.CONVERT
                //KeyEvent.VK_NONCONVERT               -> Key.NONCONVERT
                //KeyEvent.VK_ACCEPT                   -> Key.ACCEPT
                //KeyEvent.VK_MODECHANGE               -> Key.MODECHANGE
                //KeyEvent.VK_KANA                     -> Key.KANA
                //KeyEvent.VK_KANJI                    -> Key.KANJI
                //KeyEvent.VK_ALPHANUMERIC             -> Key.ALPHANUMERIC
                //KeyEvent.VK_KATAKANA                 -> Key.KATAKANA
                //KeyEvent.VK_HIRAGANA                 -> Key.HIRAGANA
                //KeyEvent.VK_FULL_WIDTH               -> Key.FULL_WIDTH
                //KeyEvent.VK_HALF_WIDTH               -> Key.HALF_WIDTH
                //KeyEvent.VK_ROMAN_CHARACTERS         -> Key.ROMAN_CHARACTERS
                //KeyEvent.VK_ALL_CANDIDATES           -> Key.ALL_CANDIDATES
                //KeyEvent.VK_PREVIOUS_CANDIDATE       -> Key.PREVIOUS_CANDIDATE
                //KeyEvent.VK_CODE_INPUT               -> Key.CODE_INPUT
                //KeyEvent.VK_JAPANESE_KATAKANA        -> Key.JAPANESE_KATAKANA
                //KeyEvent.VK_JAPANESE_HIRAGANA        -> Key.JAPANESE_HIRAGANA
                //KeyEvent.VK_JAPANESE_ROMAN           -> Key.JAPANESE_ROMAN
                //KeyEvent.VK_KANA_LOCK                -> Key.KANA_LOCK
                //KeyEvent.VK_INPUT_METHOD_ON_OFF      -> Key.INPUT_METHOD_ON_OFF
                //KeyEvent.VK_CUT                      -> Key.CUT
                //KeyEvent.VK_COPY                     -> Key.COPY
                //KeyEvent.VK_PASTE                    -> Key.PASTE
                //KeyEvent.VK_UNDO                     -> Key.UNDO
                //KeyEvent.VK_AGAIN                    -> Key.AGAIN
                //KeyEvent.VK_FIND                     -> Key.FIND
                //KeyEvent.VK_PROPS                    -> Key.PROPS
                //KeyEvent.VK_STOP                     -> Key.STOP
                //KeyEvent.VK_COMPOSE                  -> Key.COMPOSE
                //KeyEvent.VK_ALT_GRAPH                -> Key.ALT_GRAPH
                //KeyEvent.VK_BEGIN                    -> Key.BEGIN
                KeyEvent.VK_UNDEFINED      -> Key.UNDEFINED
                else -> Key.UNKNOWN
            }
        })
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        ag.onRender {
            dispatch(renderEvent)
        }

        val motionListener = object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.MOVE)
            override fun mouseDragged(e: MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.DRAG)
        }
        val mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.DOWN)

            override fun mouseReleased(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.UP)

            override fun mouseMoved(e: java.awt.event.MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.MOVE)
            override fun mouseEntered(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.ENTER)

            override fun mouseDragged(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.DRAG)

            override fun mouseClicked(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.CLICK)

            override fun mouseExited(e: java.awt.event.MouseEvent) =
                dispatchME(e, com.soywiz.korev.MouseEvent.Type.EXIT)

            override fun mouseWheelMoved(e: MouseWheelEvent) {
                dispatch(mouseEvent {
                    this.scrollDeltaX = e.preciseWheelRotation
                    this.scrollDeltaY = e.preciseWheelRotation
                    this.scrollDeltaZ = e.preciseWheelRotation
                })
            }
        }

        val keyListener = object : KeyListener {
            override fun keyTyped(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.TYPE)
            override fun keyPressed(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.DOWN)
            override fun keyReleased(e: KeyEvent) = dispatchKE(e, com.soywiz.korev.KeyEvent.Type.UP)
        }

        // In both components
        frame.addKeyListener(keyListener)
        ag.glcanvas.addKeyListener(keyListener)

        ag.glcanvas.addMouseMotionListener(motionListener)
        ag.glcanvas.addMouseListener(mouseListener)

        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                dispatchReshapeEvent(0, 0, ag.glcanvas.width, ag.glcanvas.height)
            }
        })
        launchAsap(coroutineDispatcher) {
            entry()
        }
        var lastTime = PerformanceCounter.milliseconds
        while (true) {
            val currentTime = PerformanceCounter.milliseconds
            val elapsedTime = (currentTime - lastTime).milliseconds
            coroutineDispatcher.executePending()
            ag.glcanvas.repaint()
            delay((timePerFrame - elapsedTime).milliseconds.clamp(0.0, 32.0).milliseconds)
            lastTime = currentTime
        }

    }
}
