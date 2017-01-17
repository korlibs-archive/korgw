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
import com.soywiz.korui.ui.*

object SampleSvg {
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

		val svg2 = SVG("""
			<svg width="120" height="240" version="1.1" xmlns="http://www.w3.org/2000/svg">
			  <defs>
			      <linearGradient id="Gradient1">
			        <stop stop-color="red" offset="0%"/>
			        <stop stop-color="black" stop-opacity="0" offset="50%"/>
			        <stop stop-color="blue" offset="100%"/>
			      </linearGradient>
			  </defs>

			  <rect x="10" y="10" rx="15" ry="15" width="100" height="100" fill="url(#Gradient1)"/>
			</svg>
		""")

		val svg3 = SVG("""
			<svg xmlns="http://www.w3.org/2000/svg" width="98" height="20">
				<linearGradient id="a" x2="0" y2="100%">
					<stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
					<stop offset="1" stop-opacity=".1"/>
				</linearGradient>
				<rect rx="3" width="98" height="20" fill="#555"/>
				<rect rx="3" x="37" width="61" height="20" fill="#9f9f9f"/>
				<path fill="#9f9f9f" d="M37 0h4v20h-4z"/>
				<rect rx="3" width="98" height="20" fill="url(#a)"/>
				<g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
					<text x="19.5" y="15" fill="#010101" fill-opacity=".3">build</text>
					<text x="19.5" y="14">build</text>
					<text x="66.5" y="15" fill="#010101" fill-opacity=".3">unknown</text>
					<text x="66.5" y="14">unknown</text>
				</g>
			</svg>
		""")

		//val svg2 = SVG("""
		//	<svg width="120" height="240" version="1.1" xmlns="http://www.w3.org/2000/svg">
		//	  <defs>
		//	      <linearGradient id="Gradient1">
		//	        <stop class="stop1" offset="0%"/>
		//	        <stop class="stop2" offset="50%"/>
		//	        <stop class="stop3" offset="100%"/>
		//	      </linearGradient>
		//	      <linearGradient id="Gradient2" x1="0" x2="0" y1="0" y2="1">
		//	        <stop offset="0%" stop-color="red"/>
		//	        <stop offset="50%" stop-color="black" stop-opacity="0"/>
		//	        <stop offset="100%" stop-color="blue"/>
		//	      </linearGradient>
		//	      <style type="text/css"><![CDATA[
		//	        #rect1 { fill: url(#Gradient1); }
		//	        .stop1 { stop-color: red; }
		//	        .stop2 { stop-color: black; stop-opacity: 0; }
		//	        .stop3 { stop-color: blue; }
		//	      ]]></style>
		//	  </defs>
		//
		//	  <rect x="10" y="10" rx="15" ry="15" width="100" height="100" fill="url(#Gradient2)"/>
		//	  <rect id="rect1" x="10" y="120" rx="15" ry="15" width="100" height="100"/>
		//	</svg>
		//""")

		//awtShowImageAndWait(BitmapFontGenerator.generate("Arial", 32, "abcdef").atlas)

		Application().frame("Hello World", icon = icon) {
			scrollPane {
				button("yay!")
				layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.SHOW_ALL) {
					height = 50.vmax
					vectorImage(svg, 512, 512)
				}
				button("yay!")
				inline {
					vectorImage(svg3, 98, 20)
				}
				inline {
					vectorImage(svg2, 120, 240)
				}
			}
		}
	}
}
