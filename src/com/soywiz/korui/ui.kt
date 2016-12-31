package com.soywiz.korui

import com.soywiz.kimage.bitmap.Bitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.util.spawn

interface Length {
    object AUTO : Length {
        override fun calc(size: Int): Int = size
        override fun scale(ratio: Double): Length = AUTO
    }
    class PX(val v: Int) : Length {
        override fun calc(size: Int): Int = v
        override fun scale(ratio: Double): Length = PT((this.v * ratio).toInt())
    }
    class PT(val v: Int) : Length {
        override fun calc(size: Int): Int = v
        override fun scale(ratio: Double): Length = PT((this.v * ratio).toInt())
    }
    class Ratio(val ratio: Double) : Length {
        override fun calc(size: Int): Int = (ratio * size).toInt()
        override fun scale(ratio: Double): Length = Ratio(this.ratio * ratio)
    }
    fun calc(size: Int): Int
    fun scale(ratio: Double): Length
}

val Int.px: Length get() = Length.PX(this)
val Int.pt: Length get() = Length.PT(this)
val Int.percent: Length get() = Length.Ratio(this.toDouble() / 100.0)
val Double.ratio: Length get() = Length.Ratio(this)

open class Component(val lc: LightComponents, val handle: Any) {
    var top: Length = 0.percent
    var left: Length = 0.percent
    var width: Length = 100.percent
    var height: Length = 100.percent
    var valid = false
    var actualBounds: Rectangle = Rectangle()

    var parent: Container? = null
        set(newParent) {
            if (newParent != null) {
                newParent.children -= this
            }
            field = newParent
            newParent?.children?.add(this)
            lc.setParent(handle, newParent?.handle)
            newParent?.invalidate()
        }

    val children = arrayListOf<Component>()

    fun relayout() {
        if (valid) return
        valid = true
        relayoutInternal()
        setBoundsInternal(actualBounds)
        for (child in children) {
            child.relayout()
        }
    }

    open protected fun relayoutInternal() {
    }

    fun invalidate() {
        valid = false
        parent?.invalidate()
        invalidateDescendants()
    }

    fun invalidateDescendants() {
        valid = false
        for (child in children) child.invalidateDescendants()
    }

    open fun setBoundsInternal(bounds: Rectangle) {
        lc.setBounds(handle, bounds.x, bounds.y, bounds.width, bounds.height)
    }

    fun onClick(handler: suspend () -> Unit) {
        lc.setEventHandler<LightComponents.ClickEvent>(handle) {
            spawn(handler)
        }
    }

    var visible: Boolean = false
        set(value) {
            field = value
            lc.setVisible(handle, value)
        }

    override fun toString(): String = javaClass.simpleName
}

open class Container(lc: LightComponents, type: String) : Component(lc, lc.create(type)) {
    fun <T : Component> add(other: T): T {
        other.parent = this
        return other
    }
}

class Frame(lc: LightComponents) : Container(lc, LightComponents.TYPE_FRAME) {
    open suspend fun dialogOpenFile(filter: String = ""): VfsFile = asyncFun {
        lc.dialogOpenFile(handle, filter)
    }

    open suspend fun alert(message: String): Unit = asyncFun {
        lc.dialogAlert(handle, message)
    }

    override fun relayoutInternal() {
        for (child in children) {
            child.actualBounds.set(0, 0, child.width.calc(actualBounds.width), child.height.calc(actualBounds.height))
        }
    }

    override fun setBoundsInternal(bounds: Rectangle) {
    }
}

open class Layout(lc: LightComponents) : Container(lc, LightComponents.TYPE_CONTAINER) {
    val padding = Padding()

    init {
        width = 100.percent
        height = 100.percent
    }
}

open class VerticalLayout(lc: LightComponents) : Layout(lc) {
    override fun relayoutInternal() {
        val (_, _, width, height) = actualBounds
        //var y = this.actualBounds.y
        var y2 = 0
        for (child in children) {
            child.actualBounds.set(0, y2, child.width.calc(width), child.height.calc(height))
            y2 += child.actualBounds.height
        }
    }
}

data class Padding(var left: Length = 0.px, var top: Length = 0.px, var right: Length = 0.px, var bottom: Length = 0.px) {
}

data class Rectangle(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) {
    fun set(that: Rectangle) = set(that.x, that.y, that.width, that.height)

    fun set(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }
}

class Button(lc: LightComponents, val text: String) : Component(lc, lc.create(LightComponents.TYPE_BUTTON)) {
    init {
        width = 100.percent
        height = 32.pt
        lc.setText(handle, text)
    }
}

class Spacer(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_CONTAINER)) {
    init {
        width = 100.percent
        height = 32.pt
    }
}

class Image(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_IMAGE)) {
    var image: Bitmap? = null
        set(newImage) {
            lc.setImage(handle, newImage)
            if (newImage != null) {
                width = newImage.width.px
                height = newImage.height.px
            }
        }
}

class Application(val light: LightComponents = defaultLight) {
    val frames = arrayListOf<Frame>()

    init {
        EventLoop.setInterval(16) {
            for (frame in frames.filter { !it.valid }) {
                frame.relayout()
                light.repaint(frame.handle)
            }
        }
    }
}

fun Application.frame(callback: Frame.() -> Unit): Frame {
    val frame = Frame(this.light).apply {
        actualBounds.width = 100
        actualBounds.height = 100
        callback()
        visible = true
        invalidate()
    }
    light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
    light.setEventHandler<LightComponents.ResizeEvent>(frame.handle) { e ->
        frame.actualBounds.width = e.width
        frame.actualBounds.height = e.height
        frame.invalidate()
    }
    frames += frame
    return frame
}

fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.width = width; this.height = height }

inline fun Container.button(text: String, callback: Button.() -> Unit) = add(Button(this.lc, text).apply { callback() })
inline fun Container.image(bitmap: Bitmap, callback: Image.() -> Unit) = add(Image(this.lc).apply { image = bitmap; callback() })
inline fun Container.image(bitmap: Bitmap) = add(Image(this.lc).apply { image = bitmap })
inline fun Container.spacer() = add(Spacer(this.lc))

inline fun Container.vertical(callback: VerticalLayout.() -> Unit): VerticalLayout = add(VerticalLayout(this.lc).apply { callback() })
