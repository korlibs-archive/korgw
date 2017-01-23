import com.soywiz.korim.bitmap.raster
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.async
import com.soywiz.korio.async.withTimeout
import com.soywiz.korte.Template
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.style.height
import com.soywiz.korui.ui.*

object SampleTe {
	lateinit var name: TextField
	lateinit var surname: TextField
	lateinit var adult: CheckBox

	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		Application().frame("Korte", icon = KotlinLogoSvg.raster(0.125)) {
		//Application().frame("Korte") {
			var template: TextArea? = null
			var output: TextArea? = null
			var error: TextArea? = null

			fun executeTemplate() {
				async {
					try {
						withTimeout(1000) {
							output?.text = Template(template?.text ?: "")()
							error?.text = ""
						}
					} catch (e: Throwable) {
						val message = e.message ?: e.javaClass.name
						error?.text = message + "\n" + e.stackTrace.toList().take(6).joinToString("\n")
					}
				}
			}

			vertical {
				template = textArea("""
					---
					name: Korte
					list: [This,is,a,test]
					---
					Hello from {{ name|upper }}:
					<ul>
						{% for item in list|sort -%}
							{% if item|length % 2 == 0 %}<li>{{ loop.index0 }}: {{ item }} : {{ item|length }}</li>{% endif %}
						{% end %}
					</ul>
				""".trimIndent()) {
					height = 50.percent
					onChange {
						executeTemplate()
					}
				}
				horizontal {
					height = 50.percent
					output = textArea("") {
					}
					error = textArea("") {
					}
				}
			}
			executeTemplate()
		}
	}
}