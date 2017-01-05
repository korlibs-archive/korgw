import com.soywiz.korim.format.readBitmap
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
	//val icon = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello World") {
		//button("hello from korui") {
		//	for (file in ResourcesVfs.listRecursive()) {
		//		println(file)
		//	}
		//	alert("done!")
		//}
		vertical {
			horizontal {
				height = 40.pt
				button("hello from korui").click {
					for (file in ResourcesVfs.listRecursive()) {
						println(file)
					}
					alert("done!")
				}
				button("show image").click {
					//androidShowImage(ResourcesVfs["kotlin.png"].readBitmap())
				}
			}
			val progress = progress(0, 100)
			horizontal {
				height = 40.pt
				button("start").click {
					while (progress.current < progress.max) {
						progress.current++
						sleep(50)
					}
					alert("done!")
					progress.current = 0
				}
			}
		}

		relative {
			val hello = button("hello") {
				right = 10.pt
				bottom = 10.pt
			}.click {
			}

			val world = button("world") {
				relativeTo = hello
				right = 10.pt
			}.click {
			}
		}
		//button("hello from korui")
	}
}

