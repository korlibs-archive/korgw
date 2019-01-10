package com.soywiz.korgw

import com.jogamp.opengl.*
import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.event.*
import java.awt.event.MouseEvent
import javax.swing.*


actual val DefaultGameWindow: GameWindow = object : GameWindow() {
    val glp by lazy { GLProfile.getDefault() }
    val caps by lazy { GLCapabilities(glp) }
    val frame by lazy {
        object : JFrame() {
            override fun createRootPane(): JRootPane = super.createRootPane().apply {
                putClientProperty("apple.awt.fullscreenable", true)
            }
        }
    }
    override val ag: AGAwt = AGAwt(AGConfig())

    init {
        frame.add(ag.glcanvas)
    }

    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override var size: SizeInt = SizeInt(0, 0)
        set(value) {
            field = SizeInt(value.width, value.height)
            frame.setSize(value.width, value.height)
            frame.setLocationRelativeTo(null)

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
            println("screenSize=$screenSize, frameSize=$frameSize")
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

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        ag.onResized {
            dispatch(reshapeEvent {
                this.width = ag.backWidth
                this.height = ag.backHeight
            })
        }
        ag.onRender {
            dispatch(renderEvent)
        }
        ag.glcanvas.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.MOVE)
            override fun mouseDragged(e: MouseEvent) = dispatchME(e, com.soywiz.korev.MouseEvent.Type.DRAG)
        })
        ag.glcanvas.addMouseListener(object : MouseAdapter() {
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
        })
        entry()
        while (true) {
            frame.repaint()
            delay(16.milliseconds)
        }
    }
}
