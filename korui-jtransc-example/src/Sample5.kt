import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.ui.image
import com.soywiz.korui.ui.layersKeepAspectRatio

fun main(args: Array<String>) = EventLoop.main {
	val icon = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello World", icon = icon) {
		layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.COVER) {
			image(icon)
		}
	}
}
