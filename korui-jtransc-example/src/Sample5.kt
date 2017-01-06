import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.sleep
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.bottom
import com.soywiz.korui.style.height
import com.soywiz.korui.style.relativeTo
import com.soywiz.korui.style.right
import com.soywiz.korui.ui.*

fun main(args: Array<String>) = EventLoop.main {
	val icon = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello World", icon = icon) {
		layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.COVER) {
			image(icon)
		}
	}
}

