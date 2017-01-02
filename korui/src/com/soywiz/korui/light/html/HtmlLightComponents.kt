package com.soywiz.korui.light.html

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.html.HtmlImage
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat
import com.soywiz.korio.vfs.js.JsStat
import com.soywiz.korui.light.LightClickEvent
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightEvent
import com.soywiz.korui.light.LightResizeEvent
import java.io.FileNotFoundException
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
class HtmlLightComponents : LightComponents() {
	init {
		_init()
	}

	@JTranscMethodBody(target = "js", value = """
		function addStyles(css) {
			var head = document.head || document.getElementsByTagName('head')[0];
			var style = document.createElement('style');

			style.type = 'text/css';
			if (style.styleSheet){
				style.styleSheet.cssText = css;
			} else {
				style.appendChild(document.createTextNode(css));
			}

			head.appendChild(style);
		}

		var css = [
			".myButton {",
			"	-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;",
			"	-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;",
			"	box-shadow:inset 0px 1px 0px 0px #ffffff;",
			"	background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #ffffff), color-stop(1, #f6f6f6));",
			"	background:-moz-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);",
			"	background:-webkit-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);",
			"	background:-o-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);",
			"	background:-ms-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);",
			"	background:linear-gradient(to bottom, #ffffff 5%, #f6f6f6 100%);",
			"	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffff', endColorstr='#f6f6f6',GradientType=0);",
			"	background-color:#ffffff;",
			"	-moz-border-radius:6px;",
			"	-webkit-border-radius:6px;",
			"	border-radius:6px;",
			"	border:1px solid #dcdcdc;",
			"	display:inline-block;",
			"	cursor:pointer;",
			"	color:#666666;",
			"	font-family:Arial;",
			"	font-size:15px;",
			"	font-weight:bold;",
			"	padding:6px 24px;",
			"	text-decoration:none;",
			"	text-shadow:0px 1px 0px #ffffff;",
			"}",
			".myButton:hover {",
			"	background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #f6f6f6), color-stop(1, #ffffff));",
			"	background:-moz-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);",
			"	background:-webkit-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);",
			"	background:-o-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);",
			"	background:-ms-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);",
			"	background:linear-gradient(to bottom, #f6f6f6 5%, #ffffff 100%);",
			"	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#f6f6f6', endColorstr='#ffffff',GradientType=0);",
			"	background-color:#f6f6f6;",
			"}",
			".myButton:active {",
			"	position:relative;",
			"	top:1px;",
			"}"
		].join('\n')

		//addStyles('input, progress { -webkit-appearance: none; }');
		addStyles(css);

		document.body.style.background = '#f0f0f0';
		var inputFile = document.createElement('input');
		inputFile.type = 'file';
		//inputFile.style.display = 'none';
		window.inputFile = inputFile;
		window.selectedFiles = [];
		document.body.appendChild(inputFile);
	""")
	external private fun _init()

	@JTranscMethodBody(target = "js", value = """
        var type = N.istr(p0);
        var e;
        switch (type) {
            case 'frame':
                e = document.createElement('article');
				document.body.appendChild(e);
				window.mainFrame = e;
				break;
            case 'container': e = document.createElement('div'); break;
            case 'button': e = document.createElement('input'); e.className = 'myButton'; e.type = 'button'; break;
			case 'progress': e = document.createElement('progress'); break;
            case 'image':
                e = document.createElement('canvas');
				e.style.imageRendering = 'pixelated';
				break;
            case 'label': e = document.createElement('div'); break;
            default: e = document.createElement('div'); break;
        }
        e.style.position = 'absolute';
		e.style.overflow = 'hidden';
        e.style.left = e.style.top = '0px';
        e.style.width = e.style.height = '100px';
        return e;
    """)
	external override fun create(type: String): Any

	override fun setParent(c: Any, parent: Any?) {
		val child = c.asJsDynamic()
		if (child["parentNode"] != null) {
			child["parentNode"].method("removeChild")(child)
		}
		if (parent != null) {
			(parent.asJsDynamic()).method("appendChild")(child)
		}
	}

	@JTranscMethodBody(target = "js", value = """
        var child = p0, type = N.istr(p1), handler = p2;
		var node = (type == 'resize') ? window : child;
		function dispatch(e) {
            var arg = null;
            switch (type) {
                case 'click':
                    arg = {% CONSTRUCTOR com.soywiz.korui.light.LightClickEvent:(II)V %}(e.offsetX, e.offsetY);
                    break;
                case 'resize':
					if (window.mainFrame) {
						window.mainFrame.style.width = '' + window.innerWidth + 'px';
						window.mainFrame.style.height = '' + window.innerHeight + 'px';
					}
                    arg = {% CONSTRUCTOR com.soywiz.korui.light.LightResizeEvent:(II)V %}(window.innerWidth, window.innerHeight);
                    break;
            }
            handler['{% METHOD kotlin.jvm.functions.Function1:invoke %}'](arg);
        }
        node.addEventListener(type, dispatch);
		if (type == 'resize') dispatch();
    """)
	external fun _setEventHandler(c: Any, type: String, handler: (Any) -> Unit)

	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		_setEventHandler(c, when (type) {
			LightClickEvent::class.java -> "click"
			LightResizeEvent::class.java -> "resize"
			else -> ""
		}, handler as (Any) -> Unit)
	}

	override fun setText(c: Any, text: String) {
		val child = c.asJsDynamic()
		if (child["nodeName"].toJavaString().toLowerCase() == "article") {
			document["title"] = text
		} else {
			child["value"] = text
		}
	}

	override fun setAttributeString(c: Any, key: String, value: String) {
		val child = c.asJsDynamic()
		when (child["nodeName"].toJavaString().toLowerCase()) {
			"progress" -> {

			}
		}
	}

	override fun setAttributeInt(c: Any, key: String, value: Int) {
		val child = c.asJsDynamic()
		when (child["nodeName"].toJavaString().toLowerCase()) {
			"progress" -> {
				when (key) {
					"current" -> child["value"] = value
					"max" -> child["max"] = value
				}
			}
		}
	}

	override fun setAttributeBitmap(handle: Any, key: String, value: Bitmap?) {
		val child = handle.asJsDynamic()
		when (child["nodeName"].toJavaString().toLowerCase()) {
			"article" -> {
				when (key) {
					"icon" -> {
						if (value != null) {
							val href = HtmlImage.htmlCanvasToDataUrl(HtmlImage.bitmapToHtmlCanvas(value.toBMP32()))

							var link = document.getMethod("querySelector")("link[rel*='icon']")
							if (link == null) {
								link = document.getMethod("createElement")("link")
							}
							link["type"] = "image/x-icon"
							link["rel"] = "shortcut icon"
							link["href"] = href
							document.getMethod("getElementsByTagName")("head")[0].getMethod("appendChild")(link)
						}
					}
				}
			}
		}
	}

	override fun setImage(c: Any, bmp: Bitmap?) = setImage32(c, bmp?.toBMP32())

	private fun setImage32(c: Any, bmp: Bitmap32?) {
		if (bmp != null) {
			HtmlImage.htmlCanvasSetSize(c.asJsDynamic()!!, bmp.width, bmp.height)
			HtmlImage.renderToHtmlCanvas(bmp, c.asJsDynamic()!!)
		} else {
			HtmlImage.htmlCanvasClear(c.asJsDynamic()!!)
		}
	}

	override fun setVisible(c: Any, visible: Boolean) {
		val child = c.asJsDynamic()
		if (child != null) child["style"]["display"] = if (visible) "block" else "none"
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		val child = c.asJsDynamic()
		val childStyle = child["style"]
		childStyle["left"] = "${x}px";
		childStyle["top"] = "${y}px";
		childStyle["width"] = "${width}px";
		childStyle["height"] = "${height}px";
	}

	override fun repaint(c: Any) {
	}

	suspend override fun dialogAlert(c: Any, message: String) = suspendCoroutine<Unit> { c ->
		window.getMethod("alert")(message)
		window.getMethod("setTimeout")({
			c.resume(Unit)
		}.toJsDynamic(), 0)
	}

	suspend override fun dialogPrompt(c: Any, message: String): String = suspendCoroutine { c ->
		val result = window.getMethod("prompt")(message).toJavaStringOrNull()
		window.getMethod("setTimeout")({
			if (result == null) {
				c.resumeWithException(CancellationException())
			} else {
				c.resume(result)
			}
		}.toJsDynamic(), 0)
	}

	@JTranscMethodBody(target = "js", value = """
        var child = p0, message = N.istr(p1), continuation = p2;
		var inputFile = window.inputFile;
		var files = [];
		var completedOnce = false;

		function completed() {
			if (completedOnce) return;
			completedOnce = true;
			window.selectedFiles = files;
			//console.log('completed', files);
			if (files.length > 0) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](N.str(files[0].name));
			} else {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}']({% CONSTRUCTOR java.util.concurrent.CancellationException:()V %}());
			}
		}

		inputFile.value = '';

		inputFile.onclick = function() {
			//console.log('onclick!');

			document.body.onfocus = function() {
				document.body.onfocus = null;
				setTimeout(function() {
					completed()
				}, 2000);
			};
		};

		inputFile.onchange = function (e) {
			files = e.target.files;
			//var v = this.value;
			//console.log(v);
			completed();
		};

		inputFile.click();

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun _dialogOpenFile(c: Any, filter: String): String

	suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile = asyncFun {
		SelectedFilesVfs[_dialogOpenFile(c, filter)]
	}

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}

internal object SelectedFilesVfs : Vfs() {
	@JTranscMethodBody(target = "js", value = """
		var name = N.istr(p0);
		var selectedFiles = window.selectedFiles;
		for (var n = 0; n < selectedFiles.length; n++) {
			var file = selectedFiles[n];
			if (file.name == name) return file;
		}
		return null;
    """)
	external private fun _locate(name: String): Any?

	@JTranscMethodBody(target = "js", value = """
		var file = p0 || { size : -1 };
		var stat = {% CONSTRUCTOR com.soywiz.korio.vfs.js.JsStat:(D)V %}(file.size);
		return stat;
    """)
	external private fun jsstat(file: Any?): JsStat

	private fun locate(path: String): Any? = SelectedFilesVfs._locate(path.trim('/'))

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = SelectedFilesVfs.locate(path) ?: throw FileNotFoundException(path)
		val jsstat = SelectedFilesVfs.jsstat(jsfile)
		return object : AsyncStreamBase() {
			@JTranscMethodBody(target = "js", value = """
				var file = p0, position = p1, len = p2, continuation = p3;
				var reader = new FileReader();
				var slice = file.slice(position, position + len);
				reader.onload = function(e) {
					var result = reader.result;
					var u8array = new Uint8Array(result);
					var out = new JA_B(u8array.length);
					out.setArraySlice(0, u8array);

					//console.log('read', result, slice, e, position, len, continuation);
					//console.log(result);
					continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
				};
				reader.onerror = function(e) {
					continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('error reading file'));
				};
				reader.readAsArrayBuffer(slice);
				return this['{% METHOD #CLASS:getSuspended %}']();
		    """)
			external suspend fun _read(jsfile: Any, position: Double, len: Int): ByteArray

			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
				val data = _read(jsfile, position.toDouble(), len)
				System.arraycopy(data, 0, buffer, offset, data.size)
				data.size
			}

			suspend override fun getLength(): Long = jsstat.size.toLong()
			suspend override fun close() = Unit

			@Suppress("unused")
			private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
		}.toAsyncStream()
	}

	suspend override fun stat(path: String): VfsStat {
		return SelectedFilesVfs.jsstat(locate(path)).toStat(path, this)
	}
}
