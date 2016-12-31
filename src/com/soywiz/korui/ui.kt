package com.soywiz.korui

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.VfsFile

sealed class Length {
    class PX(val v: Int) : Length()
    class Percent(val v: Double) : Length()
}

val Int.px: Length get() = Length.PX(this)
val Int.percent: Length get() = Length.Percent(this.toDouble() / 100.0)

open class Component(val lc: LightComponents, val handle: Any) {
    var top: Length? = 0.percent
    var left: Length? = 0.percent
    var width: Length? = 100.percent
    var height: Length? = 100.percent

    private var parent: Component? = null
        set(newParent) {
            if (newParent != null) {
                newParent.children -= this
            }
            field = newParent
            newParent?.children?.add(this)
            lc.setParent(handle, newParent?.handle)
        }

    val children = arrayListOf<Component>()

    fun relayout() {

    }

    fun setBoundsInternal(x: Int, y: Int, width: Int, height: Int) {
        lc.setBounds(handle, x, y, width, height)
    }

    fun <T : Component> add(other: T): T {
        other.parent = this
        return other
    }

    fun onClick(handler: () -> Unit) {
        lc.setEventHandler(handle, LightComponents.EVENT_CLICK, handler)
    }

    var visible: Boolean = false
        set(value) {
            field = value
            lc.setVisible(handle, value)
        }
}

class Frame(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_FRAME)) {
    open suspend fun dialogOpenFile(filter: String = ""): VfsFile = asyncFun {
        lc.dialogOpenFile(handle, filter)
    }

    open suspend fun alert(message: String): Unit = asyncFun {
        lc.dialogAlert(handle, message)
    }
}

class Button(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_BUTTON)) {
}

fun LightComponents.frame(callback: Frame.() -> Unit) = Frame(this).apply { callback(); visible = true; relayout() }

fun LightComponents.createFrame(): Frame = Frame(this)
fun LightComponents.createButton(): Button = Button(this)

inline fun Component.button(callback: Button.() -> Unit) = add(Button(this.lc).apply { callback() })
