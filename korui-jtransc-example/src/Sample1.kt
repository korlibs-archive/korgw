import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.async
import com.soywiz.korio.async.sleep
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.Padding
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.geom.len.vh
import com.soywiz.korui.style.*
import com.soywiz.korui.ui.*
import java.util.concurrent.CancellationException

fun main(args: Array<String>) = EventLoop.main {
	val image = ResourcesVfs["kotlin.png"].readBitmap()

	//val url = UrlVfs("http://127.0.0.1")

	Application().frame("Hello Frame!") {
		icon = image
		val c1 = RGBA(220, 220, 220, 255)
		val c2 = RGBA(255, 255, 255, 255)

		layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.COVER) {
			image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) c1 else c2 })) {
				width = 50.pt
				height = 50.pt
				smooth = false
				//setSize(100.percent, 100.percent)
			}
		}

		var askButton: Button? = null
		var loadImage: Image? = null

		//style.padding.setTo(8.px)

		padding = Padding(8.pt)

		vertical {
			padding = Padding(8.pt)
			//width = 50.percent

			horizontal {
				button("Alert!") {
					width = 50.percent
				}.click {
					alert("hello")
				}
			}
			horizontal {
				padding.setTo(8.pt)
				button("Hi") { width = 100.pt }
				button("Hello") { width = 200.pt }
				button("World")
			}
			inline {
				padding.setTo(8.pt)
				button("Hi") { width = 100.pt }
				button("Hello") { width = 200.pt }
				button("World")
			}
			val p = progress(0, 100)
			async {
				for (n in 0..100) {
					p.set(n, 100)
					sleep(16)
				}
			}
			askButton = button("What's your name...?").click {
				try {
					askButton?.text = "Hello, " + prompt("My name is:") + "!"
				} catch (c: CancellationException) {
					askButton?.text = "What's your name again...?"
				}
			}
			inline {
				image(image) {
					style.defaultSize.setToScale(0.5, 0.5)
					//style.size.setTo(this.image!!.width.pt, this.image!!.height.pt).setToScale(0.5, 0.5)
					//style.size.setToScale(0.5, 0.5)
				}
			}
			//spacer()
			button("Load Image...").click {
				try {
					val file = dialogOpenFile()
					println("File opened...")
					println(file.stat())
					loadImage?.image = file.readNativeImage()
				} catch (c: CancellationException) {
					println("Cancelled!")
					alert("Cancelled!")
					//loadImage?.image = null
				}
				//loadImage?.setSize(200.pt, 200.pt)
			}
			scrollPane {
				height = 50.vh
				inline {
					loadImage = image(image)
				}
			}
		}

		relative {
			button("FLOATING") {
				right = 16.pt
				bottom = 16.pt
				minHeight = 10.pt
				maxHeight = 10.percent

				minWidth = 50.pt
				maxWidth = 20.percent
			}
		}
	}
}

