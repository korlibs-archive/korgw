package com.soywiz.korui.light

import com.soywiz.korag.AG
import com.soywiz.korag.agFactory
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.CanvasNativeImage
import com.soywiz.korim.format.HtmlImage
import com.soywiz.korio.CancellationException
import com.soywiz.korio.FileNotFoundException
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.closeable
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.typedarray.copyRangeTo
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.browser.document
import kotlin.browser.window

actual object NativeLightsComponentsFactory : LightComponentsFactory {
	actual override fun create(): LightComponents = HtmlLightComponents()
}

var windowInputFile: HTMLInputElement? = null
var selectedFiles = arrayOf<File>()
var mainFrame: HTMLElement? = null

@Suppress("unused")
class HtmlLightComponents : LightComponents() {
	val tDevicePixelRatio = window.devicePixelRatio.toDouble();
	val devicePixelRatio = when {
		tDevicePixelRatio <= 0.0 -> 1.0
		tDevicePixelRatio.isNaN() -> 1.0
		tDevicePixelRatio.isInfinite() -> 1.0
		else -> tDevicePixelRatio
	}

	init {
		addStyles("""
			body {
				font: 11pt Arial;
			}
			.BUTTON {
				-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
				-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
				box-shadow:inset 0px 1px 0px 0px #ffffff;
				background:linear-gradient(to bottom, #ffffff 5%, #f6f6f6 100%);
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
			.BUTTON:hover {
				background:linear-gradient(to bottom, #f6f6f6 5%, #ffffff 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:active {
				padding-top: 7px;
				padding-bottom: 5px;

				background:linear-gradient(to bottom, #f0f0f0 5%, #f6f6f6 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:focus {
				/*outline: auto 5px -webkit-focus-ring-color;*/
				outline: auto 1px black;
			}
			.TEXT_AREA {
				white-space: nowrap;
				resize: none;
			}
		""")

		document.body?.style?.background = "#f0f0f0"
		val inputFile = document.createElement("input") as HTMLInputElement
		inputFile.type = "file"
		inputFile.style.visibility = "hidden"
		windowInputFile = inputFile
		selectedFiles = arrayOf()
		document.body?.appendChild(inputFile)
	}

	fun addStyles(css: String) {
		val head: HTMLHeadElement = document.head ?: document.getElementsByTagName("head")[0] as HTMLHeadElement
		val style = document.createElement("style") as HTMLStyleElement

		style.type = "text/css"
		if (style.asDynamic().styleSheet != null) {
			style.asDynamic().styleSheet.cssText = css
		} else {
			style.appendChild(document.createTextNode(css))
		}

		head.appendChild(style)
	}

	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		val handle: HTMLElement = when (type) {
			LightType.FRAME -> {
				(document.createElement("article") as HTMLElement).apply {
					this.className = "FRAME"
					document.body?.appendChild(this)
					mainFrame = this
					mainFrame?.style?.visibility = "hidden"
				}
			}
			LightType.CONTAINER -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "CONTAINER"
				}
			}
			LightType.SCROLL_PANE -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "SCROLL_PANE"
				}
			}
			LightType.BUTTON -> {
				(document.createElement("input") as HTMLInputElement).apply {
					this.className = "BUTTON"
					this.type = "button"
				}
			}
			LightType.PROGRESS -> {
				(document.createElement("progress") as HTMLElement).apply {
					this.className = "PROGRESS"
				}
			}
			LightType.IMAGE -> {
				(document.createElement("canvas") as HTMLCanvasElement)!!.apply {
					this.className = "IMAGE"
					this.style.imageRendering = "pixelated"
				}
			}
			LightType.LABEL -> {
				(document.createElement("label") as HTMLElement).apply {
					this.className = "LABEL"
				}
			}
			LightType.TEXT_FIELD -> {
				(document.createElement("input") as HTMLInputElement)!!.apply {
					this.className = "TEXT_FIELD"
					this.type = "text"
				}
			}
			LightType.TEXT_AREA -> {
				(document.createElement("textarea") as HTMLElement).apply {
					this.className = "TEXT_AREA"
					//this["type"] = "text"
				}
			}
			LightType.CHECK_BOX -> {
				(document.createElement("label") as HTMLElement).apply {
					this.className = "CHECK_BOX"
					this.asDynamic()["data-type"] = "checkbox"
					val input: HTMLInputElement = document.createElement("input") as HTMLInputElement
					input.apply {
						this.className = "TEXT_FIELD"
						this.type = "checkbox"
					}
					this.appendChild(input)
					this.appendChild(document.createElement("span")!!)
				}
			}
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				agg?.nativeComponent as HTMLCanvasElement
			}
			else -> {
				(document.createElement("div") as HTMLElement).apply {
					this.className = "UNKNOWN"
				}
			}
		}

		handle.apply {
			val style = this.style
			style.position = "absolute"

			val overflow = when (type) {
				LightType.SCROLL_PANE, LightType.TEXT_AREA, LightType.TEXT_FIELD -> true
				else -> false
			}

			style.overflowY = if (overflow) "auto" else "hidden"
			style.overflowX = if (overflow) "auto" else "hidden"
			style.left = "0px"
			style.top = "0px"
			style.width = "100px"
			style.height = "100px"
		}

		return LightComponentInfo(handle).apply {
			if (agg != null) this.ag = agg
		}
	}

	override fun setParent(c: Any, parent: Any?) {
		val child = c as HTMLElement
		child.parentNode?.removeChild(child)
		if (parent != null) {
			(parent as HTMLElement).appendChild(child)
		}
	}

	private fun EventTarget.addCloseableEventListener(name: String, func: (Event) -> Unit): Closeable {
		this.addEventListener(name, func)
		return Closeable { this.removeEventListener(name, func) }
	}

	override fun addHandler(c: Any, listener: LightMouseHandler): Closeable {
		val node = c as HTMLElement

		val info = LightMouseHandler.Info()
		fun process(e: MouseEvent, buttons: Int) = info.apply {
			this.x = (e.offsetX.toInt() * devicePixelRatio).toInt()
			this.y = (e.offsetY.toInt() * devicePixelRatio).toInt()
			this.buttons = buttons
		}

		return listOf(
			node.addCloseableEventListener("click", { listener.click2(process(it as MouseEvent, 1)) }),
			node.addCloseableEventListener("mouseover", { listener.over2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mousemove", { listener.over2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mouseup", { listener.up2(process(it as MouseEvent, 0)) }),
			node.addCloseableEventListener("mousedown", { listener.down2(process(it as MouseEvent, 0)) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightChangeHandler): Closeable {
		val node = c as HTMLElement
		val info = LightChangeHandler.Info()

		return listOf(
			node.addCloseableEventListener("change", { listener.changed2(info) }),
			node.addCloseableEventListener("keypress", { listener.changed2(info) }),
			node.addCloseableEventListener("input", { listener.changed2(info) }),
			node.addCloseableEventListener("textInput", { listener.changed2(info) }),
			node.addCloseableEventListener("paste", { listener.changed2(info) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightResizeHandler): Closeable {
		val node = window
		val info = LightResizeHandler.Info()

		fun send() {
			if (mainFrame != null) {

				mainFrame?.style?.width = "${window.innerWidth}px"
				mainFrame?.style?.height = "${window.innerHeight}px"
			}

			listener.resized2(info.apply {
				width = window.innerWidth.toInt()
				height = window.innerHeight.toInt()
			})
		}

		send()

		return listOf(
			node.addCloseableEventListener("resize", { send() })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightKeyHandler): Closeable {
		val node = c as HTMLElement
		val info = LightKeyHandler.Info()

		fun process(e: KeyboardEvent) = info.apply {
			this.keyCode = e.keyCode
		}

		return listOf(
			node.addCloseableEventListener("keydown", { listener.down2(process(it as KeyboardEvent)) }),
			node.addCloseableEventListener("keyup", { listener.up2(process(it as KeyboardEvent)) }),
			node.addCloseableEventListener("keypress", { listener.typed2(process(it as KeyboardEvent)) })
		).closeable()
	}

	override fun addHandler(c: Any, listener: LightGamepadHandler): Closeable {
		return super.addHandler(c, listener)
	}

	override fun addHandler(c: Any, listener: LightTouchHandler): Closeable {
		val node = c as HTMLElement

		fun process(e: Event, preventDefault: Boolean): List<LightTouchHandler.Info> {
			val out = arrayListOf<LightTouchHandler.Info>()
			val touches = e.asDynamic().changedTouches
			for (n in 0 until touches.length.toInt()) {
				val touch = touches[n]
				out += LightTouchHandler.Info().apply {
					this.x = (touch.pageX * devicePixelRatio)
					this.y = (touch.pageY * devicePixelRatio)
					this.id = touch.identifier
				}
			}
			if (preventDefault) e.preventDefault()
			return out
		}

		return listOf(
			node.addCloseableEventListener("touchstart", { for (info in process(it, preventDefault = false)) listener.start2(info) }),
			node.addCloseableEventListener("touchend", { for (info in process(it, preventDefault = false)) listener.end2(info) }),
			node.addCloseableEventListener("touchmove", { for (info in process(it, preventDefault = true)) listener.move2(info) })
		).closeable()
	}

	override fun <T> callAction(c: Any, key: LightAction<T>, param: T) {
		when (key) {
			LightAction.FOCUS -> {
				val child = c.asDynamic()
				child.focus()
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		val child = c as HTMLElement
		val childOrDocumentBody = if (child.nodeName.toLowerCase() == "article") document.body else child
		val nodeName = child.nodeName.toLowerCase()
		when (key) {
			LightProperty.TEXT -> {
				val v = key[value]
				if (nodeName == "article") {
					document.title = v
				} else if (nodeName == "input" || nodeName == "textarea") {
					(child as HTMLInputElement).value = v
				} else {
					if ((child.asDynamic()["data-type"]) == "checkbox") {
						(child.querySelector("span") as HTMLSpanElement)?.innerText = v
					} else {
						child.innerText = v
					}
				}
			}
			LightProperty.PROGRESS_CURRENT -> {
				val v = key[value]
				(child as HTMLInputElement).value = "$v"
			}
			LightProperty.PROGRESS_MAX -> {
				val v = key[value]
				(child as HTMLInputElement).max = "$v"
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				childOrDocumentBody?.style?.background = colorString(v)
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				child.style.imageRendering = if (v) "auto" else "pixelated"
			}
			LightProperty.ICON -> {
				val v = key[value]
				if (v != null) {
					val href = HtmlImage.htmlCanvasToDataUrl(HtmlImage.bitmapToHtmlCanvas(v.toBMP32()))

					var link: HTMLLinkElement? = document.querySelector("link[rel*='icon']") as? HTMLLinkElement?
					if (link == null) {
						link = document.createElement("link") as HTMLLinkElement
					}
					link.type = "image/x-icon"
					link.rel = "shortcut icon"
					link.href = href
					document.getElementsByTagName("head")[0]?.appendChild(link)
				}
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				if (bmp is CanvasNativeImage) {
					setCanvas(c, bmp.canvas)
				} else {
					setImage32(c, bmp?.toBMP32())
				}
			}
			LightProperty.VISIBLE -> {
				val v = key[value]
				if (child != null) child.style.display = if (v) "block" else "none"
			}
			LightProperty.CHECKED -> {
				val v = key[value]
				(child.querySelector("input[type=checkbox]") as HTMLInputElement).checked = v
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		val child = c as HTMLElement

		when (key) {
			LightProperty.TEXT -> {
				return (child as HTMLInputElement).value as T
			}
			LightProperty.CHECKED -> {
				val input = (child as HTMLInputElement).querySelector("input[type=checkbox]")
				val checked: Boolean = input.asDynamic().checked
				return checked as T
			}
		}
		return super.getProperty(c, key)
	}


	fun colorString(c: Int) = "RGBA(${RGBA.getR(c)},${RGBA.getG(c)},${RGBA.getB(c)},${RGBA.getAf(c)})"

	private fun setCanvas(c: Any, bmp: HTMLCanvasElement?) {
		val targetCanvas = c as HTMLCanvasElement
		if (bmp != null) {
			targetCanvas.width = bmp.width
			targetCanvas.height = bmp.height
			val ctx = targetCanvas.getContext("2d") as CanvasRenderingContext2D
			HtmlImage.htmlCanvasClear(targetCanvas)
			ctx.drawImage(bmp, 0.0, 0.0)
		} else {
			HtmlImage.htmlCanvasClear(targetCanvas)
		}
	}

	private fun setImage32(c: Any, bmp: Bitmap32?) {
		val canvas = c as HTMLCanvasElement
		if (bmp != null) {
			HtmlImage.htmlCanvasSetSize(canvas, bmp.width, bmp.height)
			HtmlImage.renderToHtmlCanvas(bmp, canvas)
		} else {
			HtmlImage.htmlCanvasClear(canvas)
		}
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		val child = c as HTMLElement
		val childStyle = child.style
		childStyle.left = "${x}px"
		childStyle.top = "${y}px"
		childStyle.width = "${width}px"
		childStyle.height = "${height}px"

		if (child is HTMLCanvasElement) {
			child.width = (width * devicePixelRatio).toInt()
			child.height = (height * devicePixelRatio).toInt()
		}
	}

	override fun repaint(c: Any) {
		mainFrame?.style?.visibility = "visible"
	}

	suspend override fun dialogAlert(c: Any, message: String) = korioSuspendCoroutine<Unit> { c ->
		window.alert(message)
		window.setTimeout({
			c.resume(Unit)
		}, 0)
	}

	suspend override fun dialogPrompt(c: Any, message: String): String = korioSuspendCoroutine { c ->
		val result = window.prompt(message)
		window.setTimeout({
			if (result == null) {
				c.resumeWithException(CancellationException("cancelled"))
			} else {
				c.resume(result)
			}
		}, 0)
	}

	suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile = korioSuspendCoroutine { continuation ->
		val inputFile = windowInputFile
		var completedOnce = false
		var files = arrayOf<File>()

		val completed = {
			if (!completedOnce) {
				completedOnce = true

				selectedFiles = files

				//console.log('completed', files);
				if (files.size.toInt() > 0) {
					val fileName = files[0].name
					continuation.resume(SelectedFilesVfs[fileName])
				} else {
					continuation.resumeWithException(CancellationException("cancel"))
				}
			}
		}

		windowInputFile?.value = ""

		windowInputFile?.onclick = {
			document.body?.onfocus = {
				document.body?.onfocus = null
				window.setTimeout({
					completed()
				}, 2000)
			}
			Unit
		}

		windowInputFile?.onchange = { e ->
			files = e?.target.asDynamic()["files"]
			//var v = this.value;
			//console.log(v);
			completed()
		}

		inputFile?.click()
	}

	override fun openURL(url: String): Unit {
		window.open(url, "_blank")
	}

	override fun getDpi(): Double {
		return (window.devicePixelRatio.toInt() * 96).toDouble()
	}
}

internal object SelectedFilesVfs : Vfs() {
	private fun _locate(name: String): File? {
		val length = selectedFiles.size.toInt()
		for (n in 0 until length) {
			val file = selectedFiles[n]
			if (file.name!! == name) {
				return file
			}
		}
		return null
	}

	private fun jsstat(file: File?): JsStat {
		return JsStat(file?.size?.toDouble() ?: 0.0)
	}

	private fun locate(path: String): File? = _locate(path.trim('/'))

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = locate(path) ?: throw FileNotFoundException(path)
		val jsstat = jsstat(jsfile)
		return object : AsyncStreamBase() {
			suspend fun _read(jsfile: File, position: Double, len: Int): ByteArray = korioSuspendCoroutine { c ->
				val reader = FileReader()
				// @TODO: Blob.slice should use Double
				val djsfile = jsfile.asDynamic()
				val slice = djsfile.slice(position, (position + len))

				reader.onload = {
					val result = reader.result
					c.resume(Int8Array(result.unsafeCast<ArrayBuffer>()).unsafeCast<ByteArray>())
				}

				reader.onerror = {
					c.resumeWithException(RuntimeException("error reading file"))
				}
				reader.readAsArrayBuffer(slice)
			}

			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				val data = _read(jsfile, position.toDouble(), len)
				data.copyRangeTo(0, buffer, offset, data.size)
				return data.size
			}

			suspend override fun getLength(): Long = jsstat.size.toLong()
			suspend override fun close() = Unit
		}.toAsyncStream()
	}

	suspend override fun stat(path: String): VfsStat {
		return jsstat(locate(path)).toStat(path, this)
	}
}

data class JsStat(val size: Double, var isDirectory: Boolean = false) {
	fun toStat(path: String, vfs: Vfs): VfsStat = vfs.createExistsStat(path, isDirectory = isDirectory, size = size.toLong())
}