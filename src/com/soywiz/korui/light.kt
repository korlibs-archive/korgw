package com.soywiz.korui

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.kimage.bitmap.Bitmap
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.awt.AwtLightComponents

open class LightComponents {
    companion object {
        val WINDOW_TYPE = "window"
        val BUTTON_TYPE = "button"
        val IMAGE_TYPE = "image"
        val LABEL_TYPE = "label"
    }


    //open fun destroy(obj: Any): Unit {
    //}

    open fun create(type: String): Any = throw UnsupportedOperationException()
    open fun setParent(c: Any, parent: Any?): Unit = throw UnsupportedOperationException()
    open fun setText(c: Any, text: String): Unit = throw UnsupportedOperationException()
    open fun setImage(c: Any, bmp: Bitmap): Unit = throw UnsupportedOperationException()
    open fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int): Unit = throw UnsupportedOperationException()
    suspend fun dialogOpenFile(filter: String): VfsFile = throw UnsupportedOperationException()
}

val light by lazy { _LightComponents() }

@JTranscMethodBody(target = "js", value = """
    return {% CONSTRUCTOR com.soywiz.korui.HtmlLightComponents:()V %}();
""")
fun _LightComponents() = AwtLightComponents()