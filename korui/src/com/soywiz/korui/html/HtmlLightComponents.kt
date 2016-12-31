package com.soywiz.korui.html

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.LightClickEvent
import com.soywiz.korui.LightComponents
import com.soywiz.korui.LightEvent
import com.soywiz.korui.LightResizeEvent

class HtmlLightComponents : LightComponents() {
	@JTranscMethodBody(target = "js", value = """
        var type = N.istr(p0);
        var e;
        switch (type) {
            case 'frame': e = document.createElement('div'); document.body.appendChild(e); break;
            case 'container': e = document.createElement('div'); break;
            case 'button': e = document.createElement('input'); e.type = 'button'; break;
            case 'image': e = document.createElement('img'); break;
            case 'label': e = document.createElement('div'); break;
            default: e = document.createElement('div'); break;
        }
        e.style.position = 'absolute';
        e.style.left = e.style.top = '0px';
        e.style.width = e.style.height = '100px';
        return e;
    """)
	external override fun create(type: String): Any

	@JTranscMethodBody(target = "js", value = """
        var child = p0, newParent = p1;
        if (child.parentNode) child.parentNode.removeChild(child);
        if (newParent) newParent.appendChild(child);
    """)
	external override fun setParent(c: Any, parent: Any?)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, type = N.istr(p1), handler = p2;
        child.addEventListener(type, function(e) {
            var arg = null;
            switch (type) {
                case 'click':
                    arg = {% CONSTRUCTOR com.soywiz.korui.LightClickEvent:()V %}();
                    break;
                case 'resize':
                    arg = {% CONSTRUCTOR com.soywiz.korui.LightResizeEvent:(II)V %}(100, 100);
                    break;
            }
            handler['{% METHOD kotlin.jvm.functions.Function1:invoke %}'](arg);
        });
    """)
	external fun _setEventHandler(c: Any, type: String, handler: (Any) -> Unit)

	override fun <T : LightEvent> setEventHandler(c: Any, type: Class<T>, handler: (T) -> Unit) {
		_setEventHandler(c, when (type) {
			LightClickEvent::class.java -> "click"
			LightResizeEvent::class.java -> "resize"
			else -> ""
		}, handler as (Any) -> Unit)
	}

	@JTranscMethodBody(target = "js", value = """
        var child = p0, text = N.istr(p1);
        child.value = text;
    """)
	external override fun setText(c: Any, text: String)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, image = p1;
    """)
	external override fun setImage(c: Any, bmp: Bitmap?)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, visible = p1;
    """)
	external override fun setVisible(c: Any, visible: Boolean)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, x = p1, y = p2, width = p3, height = p4;
        child.style.left = '' + x + 'px';
        child.style.top = '' + y + 'px';
        child.style.width = '' + width + 'px';
        child.style.height = '' + height + 'px';
    """)
	external override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int)

	@JTranscMethodBody(target = "js", value = """
        var child = p0;
    """)
	external override fun repaint(c: Any)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, message = N.istr(p1), continuation = p2;
        alert(message);
    """)
	external suspend override fun dialogAlert(c: Any, message: String)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, message = N.istr(p1), continuation = p2;
    """)
	external suspend override fun dialogPrompt(c: Any, message: String): String

	@JTranscMethodBody(target = "js", value = """
        var child = p0, message = N.istr(p1), continuation = p2;
    """)
	external suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile
}