package com.soywiz.korui.html

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.registerBitmapReading
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat
import com.soywiz.korio.vfs.js.JsStat
import com.soywiz.korui.LightClickEvent
import com.soywiz.korui.LightComponents
import com.soywiz.korui.LightEvent
import com.soywiz.korui.LightResizeEvent
import java.io.FileNotFoundException
import kotlin.coroutines.CoroutineIntrinsics

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

		addStyles('input, progress { -webkit-appearance: none; }');
		document.body.style.background = '#e0e0e0';
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
            case 'button': e = document.createElement('input'); e.type = 'button'; break;
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

	@JTranscMethodBody(target = "js", value = """
        var child = p0, newParent = p1;
        if (child.parentNode) child.parentNode.removeChild(child);
        if (newParent) newParent.appendChild(child);
    """)
	external override fun setParent(c: Any, parent: Any?)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, type = N.istr(p1), handler = p2;
		var node = (type == 'resize') ? window : child;
		function dispatch(e) {
            var arg = null;
            switch (type) {
                case 'click':
                    arg = {% CONSTRUCTOR com.soywiz.korui.LightClickEvent:()V %}();
                    break;
                case 'resize':
					if (window.mainFrame) {
						window.mainFrame.style.width = '' + window.innerWidth + 'px';
						window.mainFrame.style.height = '' + window.innerHeight + 'px';
					}
                    arg = {% CONSTRUCTOR com.soywiz.korui.LightResizeEvent:(II)V %}(window.innerWidth, window.innerHeight);
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

	@JTranscMethodBody(target = "js", value = """
        var child = p0, text = N.istr(p1);
		if (child.nodeName.toLowerCase() == 'article') {
			document.title = text;
		} else {
        	child.value = text;
		}
    """)
	external override fun setText(c: Any, text: String)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, key = N.istr(p1), value = N.istr(p2);
		switch (child.nodeName.toLowerCase()) {
			case 'progress':
			break;
		}
    """)
	external override fun setAttributeString(c: Any, key: String, value: String)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, key = N.istr(p1), value = p2;
		switch (child.nodeName.toLowerCase()) {
			case 'progress':
				switch (key) {
					case 'current': child.value = value; break;
					case 'max': child.max = value; break;
				}
			break;
		}
    """)
	external override fun setAttributeInt(c: Any, key: String, value: Int)


	override fun setImage(c: Any, bmp: Bitmap?) = setImage32(c, bmp?.toBMP32())

	@JTranscMethodBody(target = "js", value = """
        var child = p0, bmp = p1;
		if (child.getContext) { // Canvas
			var width = child.width;
			var height = child.height;
			var ctx = child.getContext('2d');
			ctx.clearRect(0, 0, width, height);
			if (bmp != null) {
				var bmpWidth = bmp["{% METHOD com.soywiz.korim.bitmap.Bitmap:getWidth %}"](); // int
				var bmpHeight = bmp["{% METHOD com.soywiz.korim.bitmap.Bitmap:getHeight %}"](); // int
				var bmpData = bmp["{% METHOD com.soywiz.korim.bitmap.Bitmap32:getData %}"](); // int[]
				child.width = bmpWidth;
				child.height = bmpHeight;
				//console.log(bmpData, bmpWidth, bmpHeight);
				var pixelCount = bmpData.length;
				var idata = ctx.createImageData(bmpWidth, bmpHeight);
				var idataData = idata.data;
				var m = 0;
				for (var n = 0; n < pixelCount; n++) {
					var c = bmpData.data[n];
					idataData[m++] = (c >>  0) & 0xFF;
					idataData[m++] = (c >>  8) & 0xFF;
					idataData[m++] = (c >> 16) & 0xFF;
					idataData[m++] = (c >> 24) & 0xFF;
				}
				ctx.putImageData(idata, 0, 0);
			}
		} else {
		}
    """)
	external private fun setImage32(c: Any, bmp: Bitmap32?)

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
        alert(message); // @TODO: Synchronous
		setTimeout(function() {
			continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](null);
		}, 0);
		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend override fun dialogAlert(c: Any, message: String)

	@JTranscMethodBody(target = "js", value = """
        var child = p0, message = N.istr(p1), continuation = p2;
		var result = prompt(message); // @TODO: Synchronous
		setTimeout(function() {
			if (result === null) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}']({% CONSTRUCTOR java.util.concurrent.CancellationException:()V %}());
			} else {
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](N.str(result));
			}
		}, 0);
		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend override fun dialogPrompt(c: Any, message: String): String

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
	init {
		registerBitmapReading()
	}

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

	private fun locate(path: String): Any? = _locate(path.trim('/'))

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = locate(path) ?: throw FileNotFoundException(path)
		val jsstat = jsstat(jsfile)
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
		return jsstat(locate(path)).toStat(path, this)
	}
}