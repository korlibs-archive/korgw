import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.async
import com.soywiz.korio.async.sleep
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.px
import com.soywiz.korui.ui.*
import java.util.concurrent.CancellationException

fun main(args: Array<String>) = EventLoop.main {
	val image = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello Frame!") {
		val c1 = RGBA(220, 220, 220, 255)
		val c2 = RGBA(255, 255, 255, 255)

		image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) c1 else c2 })) {
			setSize(100.percent, 100.percent)
		}

		var askButton: Button? = null
		var loadImage: Image? = null

		vertical {
			//width = 50.percent
			button("Alert!") {
				alert("hello")
			}.apply {
				style.width = 50.percent
			}
			horizontal {
				button("Hi")
				button("Hello")
				button("World")
			}
			val p = progress(0, 100)
			async {
				for (n in 0..100) {
					p.set(n, 100)
					sleep(16)
				}
			}
			askButton = button("What's your name...?") {
				try {
					askButton?.text = prompt("My name is:")
				} catch (c: CancellationException) {
					askButton?.text = "What's your name again...?"
				}
			}
			image(image).apply {
				style.size.setToScale(0.5, 0.5)
			}
			spacer()
			button("Load Image...") {
				try {
					val file = dialogOpenFile()
					println("File opened...")
					println(file.stat())
					loadImage?.image = file.readBitmap()
				} catch (c: CancellationException) {
					println("Cancelled!")
					alert("Cancelled!")
					//loadImage?.image = null
				}
				loadImage?.setSize(200.px, 200.px)
			}
			loadImage = image(image)
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

