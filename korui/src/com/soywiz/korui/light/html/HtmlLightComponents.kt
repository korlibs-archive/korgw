package com.soywiz.korui.light.html

import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
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
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
class HtmlLightComponents : LightComponents() {
	init {
		addStyles("""
			.myButton {
				-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
				-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
				box-shadow:inset 0px 1px 0px 0px #ffffff;
				background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #ffffff), color-stop(1, #f6f6f6));
				background:-moz-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);
				background:-webkit-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);
				background:-o-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);
				background:-ms-linear-gradient(top, #ffffff 5%, #f6f6f6 100%);
				background:linear-gradient(to bottom, #ffffff 5%, #f6f6f6 100%);
				filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffff', endColorstr='#f6f6f6',GradientType=0);
				background-color:#ffffff;
				-moz-border-radius:6px;
				-webkit-border-radius:6px;
				border-radius:6px;
				border:1px solid #dcdcdc;
				display:inline-block;
				cursor:pointer;
				color:#666666;
				font-family:Arial;
				font-size:15px;
				font-weight:bold;
				padding:6px 24px;
				text-decoration:none;
				text-shadow:0px 1px 0px #ffffff;
			}
			.myButton:hover {
				background:-webkit-gradient(linear, left top, left bottom, color-stop(0.05, #f6f6f6), color-stop(1, #ffffff));
				background:-moz-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);
				background:-webkit-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);
				background:-o-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);
				background:-ms-linear-gradient(top, #f6f6f6 5%, #ffffff 100%);
				background:linear-gradient(to bottom, #f6f6f6 5%, #ffffff 100%);
				filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#f6f6f6', endColorstr='#ffffff',GradientType=0);
				background-color:#f6f6f6;
			}
			.myButton:active {
				position:relative;
				top:1px;
			}
		""")

		document["body"]["style"]["background"] = "#f0f0f0"
		val inputFile = document.methods["createElement"]("input")
		inputFile["type"] = "file"
		inputFile["style"]["visibility"] = "hidden"
		window["inputFile"] = inputFile
		window["selectedFiles"] = jsArray()
		document["body"].methods["appendChild"](inputFile)
	}

	fun addStyles(css: String) {
		val head = document["head"] ?: document.method("getElementsByTagName")("head")[0]
		val style = document.method("createElement")("style")

		style["type"] = "text/css"
		if (style["styleSheet"] != null) {
			style["styleSheet"]["cssText"] = css
		} else {
			style.method("appendChild")(document.method("createTextNode")(css))
		}

		head.method("appendChild")(style)
	}

	override fun create(type: String): Any {
		var e: JsDynamic? = null
		when (type) {
			TYPE_FRAME -> {
				e = document.method("createElement")("article")
				document["body"].method("appendChild")(e)
				window["mainFrame"] = e
			}
			TYPE_CONTAINER -> {
				e = document.method("createElement")("div")
			}
			TYPE_BUTTON -> {
				e = document.method("createElement")("input")
				e["className"] = "myButton"
				e["type"] = "button"
			}
			TYPE_PROGRESS -> {
				e = document.method("createElement")("progress")
			}
			TYPE_IMAGE -> {
				e = document.method("createElement")("canvas")
				e["style"]["imageRendering"] = "pixelated"
			}
			TYPE_LABEL -> {
				e = document.method("createElement")("div")
			}
			else -> {
				e = document.method("createElement")("div")
			}
		}

		e["style"]["position"] = "absolute"
		e["style"]["overflow"] = "hidden"
		e["style"]["left"] = "0px"
		e["style"]["top"] = "0px"
		e["style"]["width"] = "100px"
		e["style"]["height"] = "100px"

		return e!!
	}

	override fun setParent(c: Any, parent: Any?) {
		val child = c.asJsDynamic()
		if (child["parentNode"] != null) {
			child["parentNode"].method("removeChild")(child)
		}
		if (parent != null) {
			(parent.asJsDynamic()).method("appendChild")(child)
		}
	}

	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		val typeName = when (type) {
			LightClickEvent::class.java -> "click"
			LightResizeEvent::class.java -> "resize"
			else -> "unknown"
		}

		val node = if (type == LightResizeEvent::class.java) window else c.asJsDynamic()
		val dispatch = { e: JsDynamic? ->
			when (type) {
				LightClickEvent::class.java -> {
					handler(LightClickEvent(e["offsetX"].toInt(), e["offsetY"].toInt()) as T)
				}
				LightResizeEvent::class.java -> {
					if (window["mainFrame"] != null) {
						window["mainFrame"]["style"]["width"] = "${window["innerWidth"].toInt()}px"
						window["mainFrame"]["style"]["height"] = "${window["innerHeight"].toInt()}px"
					}
					handler(LightResizeEvent(window["innerWidth"].toInt(), window["innerHeight"].toInt()) as T)
				}
			}
		}
		node.method("addEventListener")(typeName, jsFunction(dispatch))
		if (type == LightResizeEvent::class.java) dispatch(null)
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

	override fun setImage(c: Any, bmp: Bitmap?) {
		if (bmp is NativeImage) {
			setCanvas(c, bmp.data.asJsDynamic())
		} else {
			setImage32(c, bmp?.toBMP32())
		}
	}

	private fun setCanvas(c: Any, bmp: JsDynamic?) {
		val targetCanvas = c.asJsDynamic()!!
		if (bmp != null) {
			targetCanvas["width"] = bmp["width"]
			targetCanvas["height"] = bmp["height"]
			val ctx = targetCanvas.methods["getContext"]("2d")
			HtmlImage.htmlCanvasClear(targetCanvas)
			ctx.methods["drawImage"](bmp, 0, 0)
		} else {
			HtmlImage.htmlCanvasClear(targetCanvas)
		}
	}

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
		childStyle["left"] = "${x}px"
		childStyle["top"] = "${y}px"
		childStyle["width"] = "${width}px"
		childStyle["height"] = "${height}px"
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

	suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile = suspendCoroutine { continuation ->
		val inputFile = window["inputFile"]
		var completedOnce = false
		var files = jsArray()

		val completed = {
			if (!completedOnce) {
				completedOnce = true

				window["selectedFiles"] = files

				//console.log('completed', files);
				if (files["length"].toInt() > 0) {
					val fileName = files[0]["name"].toJavaString()
					continuation.resume(SelectedFilesVfs[fileName])
				} else {
					continuation.resumeWithException(CancellationException())
				}
			}
		}

		inputFile["value"] = ""

		inputFile["onclick"] = jsFunctionRaw1 { e ->
			document["body"]["onfocus"] = jsFunctionRaw1 { e ->
				document["body"]["onfocus"] = null
				global.methods["setTimeout"](jsFunctionRaw1 { e ->
					completed()
				}, 2000)
			}
			Unit
		}

		inputFile["onchange"] = jsFunctionRaw1 { e ->
			files = e["target"]["files"]
			//var v = this.value;
			//console.log(v);
			completed()
		}

		inputFile.methods["click"]()
	}

	override fun openURL(url: String): Unit {
		window.methods["open"](url, "_blank")
	}
}

internal object SelectedFilesVfs : Vfs() {
	private fun _locate(name: String): JsDynamic? {
		val selectedFiles = window["selectedFiles"]
		val length = selectedFiles["length"].toInt()
		for (n in 0 until length) {
			val file = selectedFiles[n]
			if (file["name"]!!.eq(name.toJavaScriptString())) {
				return file
			}
		}
		return null
	}

	private fun jsstat(file: JsDynamic?): JsStat {
		return JsStat(file["size"].toDouble())
	}

	private fun locate(path: String): JsDynamic? = _locate(path.trim('/'))

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = locate(path) ?: throw FileNotFoundException(path)
		val jsstat = jsstat(jsfile)
		return object : AsyncStreamBase() {
			suspend fun _read(jsfile: JsDynamic, position: Double, len: Int): ByteArray = suspendCoroutine { c ->
				val reader = jsNew("FileReader")
				val slice = jsfile.method("slice")(position, position + len)

				reader["onload"] = jsFunctionRaw1 { e ->
					val result = reader["result"]
					//val u8array = window["Uint8Array"].new2(result)
					val u8array = jsNew("Uint8Array", result)
					val out = ByteArray(u8array["length"].toInt())
					(out.asJsDynamic()).method("setArraySlice")(0, u8array)
					c.resume(out)
				}

				reader["onerror"] = jsFunctionRaw1 { e ->
					c.resumeWithException(RuntimeException("error reading file"))
				}
				reader.method("readAsArrayBuffer")(slice)
			}

			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
				val data = _read(jsfile, position.toDouble(), len)
				System.arraycopy(data, 0, buffer, offset, data.size)
				data.size
			}

			suspend override fun getLength(): Long = jsstat.size.toLong()
			suspend override fun close() = Unit
		}.toAsyncStream()
	}

	suspend override fun stat(path: String): VfsStat {
		return jsstat(locate(path)).toStat(path, this)
	}
}
