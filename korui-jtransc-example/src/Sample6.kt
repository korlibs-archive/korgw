import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.geom.len.vmin
import com.soywiz.korui.style.height
import com.soywiz.korui.style.padding
import com.soywiz.korui.ui.*

object Sample6 {
	lateinit var name: TextField
	lateinit var surname: TextField
	lateinit var adult: CheckBox

	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		val icon = ResourcesVfs["kotlin.png"].readBitmap()

		Application().frame("Hello World", icon = icon) {
			padding.setTo(8.pt)
			vertical {
				padding.setTo(8.pt)
				horizontal {
					label("Name:")
					name = textField("Test")
				}
				horizontal {
					label("Surname:")
					surname = textField("Test Test")
				}
				horizontal {
					adult = checkBox("I'm an adult") {
						checked = true
					}
				}
				spacer()
				button("Submit!") {
					height = 10.vmin
				}.click {
					if (!adult.checked) {
						alert("Not an adult!")
					} else {
						alert("Hello ${name.text} ${surname.text}!")
					}
				}
			}
		}
	}
}