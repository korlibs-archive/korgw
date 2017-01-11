import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.vmax
import com.soywiz.korui.style.height
import com.soywiz.korui.ui.button
import com.soywiz.korui.ui.image
import com.soywiz.korui.ui.layersKeepAspectRatio
import com.soywiz.korui.ui.vertical

object Sample7 {
	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		val icon = ResourcesVfs["kotlin.png"].readBitmap()

		val svg = SVG("""
			<svg height="400" width="450">
			  <path id="lineAB" d="M 100 350 l 150 -300" stroke="red" stroke-width="3" fill="none" />
			  <path id="lineBC" d="M 250 50 l 150 300" stroke="red" stroke-width="3" fill="none" />
			  <path d="M 175 200 l 150 0" stroke="green" stroke-width="3" fill="none" />
			  <path d="M 100 350 q 150 -300 300 0" stroke="blue" stroke-width="5" fill="none" />
			  <!-- Mark relevant points -->
			  <g stroke="black" stroke-width="3" fill="black">
				<circle id="pointA" cx="100" cy="350" r="3" />
				<circle id="pointB" cx="250" cy="50" r="3" />
				<circle id="pointC" cx="400" cy="350" r="3" />
			  </g>
			  <!-- Label the points -->
			  <g font-size="30" font-family="sans-serif" fill="black" stroke="none" text-anchor="middle">
				<text x="100" y="350" dx="-30">A</text>
				<text x="250" y="50" dy="-10">B</text>
				<text x="400" y="350" dx="30">C</text>
			  </g>
			  Sorry, your browser does not support inline SVG.
			</svg>
		""")

		Application().frame("Hello World", icon = icon) {
			vertical {
				layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.SHOW_ALL) {
					height = 50.vmax
					val bmp = NativeImage(512, 512)
					val ctx = bmp.getContext2d()
					ctx.draw(svg)
					image(bmp)
				}
				button("yay!")
			}
		}
	}
}