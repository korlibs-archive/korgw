import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.bottom
import com.soywiz.korui.style.relativeTo
import com.soywiz.korui.style.right
import com.soywiz.korui.ui.button
import com.soywiz.korui.ui.click
import com.soywiz.korui.ui.relative

fun main(args: Array<String>) = EventLoop.main {
	val icon = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello Frame!", icon = icon) {
		relative {
			val button1 = button("hello") {
				right = 50.pt
				bottom = 50.pt
			}.click {
				alert("hello")
			}
			val button2 = button("world") {
				relativeTo = button1
				right = 50.pt
			}.click {
				alert("world")
			}
		}
	}
}

