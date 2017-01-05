import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.height
import com.soywiz.korui.ui.*

fun main(args: Array<String>) = EventLoop.main {
	val icon = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello Frame!", icon = icon) {
		vertical {
			horizontal {
				//height = 80.pt
				height = 100.percent
				button("hello1") { height = 80.pt }
				button("hello2") { height = 80.pt }
			}
			horizontal {
				//height = 80.pt
				height = 100.percent
				button("world1") { height = 80.pt }
				button("world2") { height = 80.pt }
			}
			inline {
				//height = 80.pt
				height = 100.percent
				button("hello1") { height = 80.pt }
				button("hello2") { height = 80.pt }
			}
			button("world") {
				height = 100.percent
			}.click {
				alert("world")
			}
		}
	}
}

