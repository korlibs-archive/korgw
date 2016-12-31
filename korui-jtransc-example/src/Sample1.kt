import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.*

fun main(args: Array<String>) = EventLoop.main {
	val image = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello Frame!") {
		val c1 = RGBA.blend(Colors.BLACK, Colors.WHITE, 256)
		val c2 = RGBA.blend(Colors.BLACK, Colors.WHITE, 200)

		image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) c1 else c2 })) {
			setSize(100.percent, 100.percent)
		}

		vertical {
			width = 50.percent
			button("hello") {
				onClick { alert("hello") }
			}.apply {
				width = 50.percent
			}
			button("world") { onClick { alert("world") } }
			image(image).apply {
				setSize(width.scale(0.5), height.scale(0.5))
			}
			spacer()
			button("test") { onClick { alert("world") } }
			image(image)
		}

		//image(Bitmap32(50, 50, { _, _ -> Colors.WHITE })) {
		//    setBoundsInternal(0, 0, 100, 100)
		//}
		/*
		button {
			top = 50.percent
			width = 50.percent
			height = 50.percent
			setBoundsInternal(0, 0, 100, 100)
			onClick {
				println("click!")
				spawn {
					println("click [work]!")
					alert("Button pressed!")
					try {
						val file = dialogOpenFile()
						println(file.readString())
					} catch (t: CancellationException) {
						println("cancelled!")
					}
				}
			}
		}
		*/
	}
}

