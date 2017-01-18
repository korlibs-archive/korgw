@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korim.awt.awtShowImageAndWait
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.raster
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.sleep
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.cm
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.height
import com.soywiz.korui.style.width
import com.soywiz.korui.ui.*

object SampleAg {
	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		val kotlinSvgLogo = SVG("""
			<svg version="1.1" id="Capa_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
				 width="595" height="595" viewBox="0 0 595 595" enable-background="new 0 0 595 595" xml:space="preserve">
			<g style="overflow:hidden; text-anchor: middle; font-size:180; font-weight: bold; font-family: Arial, Helvetica, sans-serif">


			   <linearGradient id="SVGID_1_" gradientUnits="userSpaceOnUse" x1="37.6749" y1="167.2581" x2="221.4579" y2="-36.2803">
				<stop  offset="0" style="stop-color:#1681AE"/>
				<stop  offset="1" style="stop-color:#67629D"/>
			</linearGradient>
			<polygon fill="url(#SVGID_1_)" points="27.6,26.6 27.6,313.1 301.8,26.6 "/>
			<linearGradient id="SVGID_2_" gradientUnits="userSpaceOnUse" x1="407.669" y1="44.1906" x2="33.2947" y2="460.4711">
				<stop  offset="0" style="stop-color:#D37D36"/>
				<stop  offset="0.4271" style="stop-color:#CF784C"/>
				<stop  offset="1" style="stop-color:#BB5C8A"/>
			</linearGradient>
			<polygon fill="url(#SVGID_2_)" points="561.7,26.6 301.8,26.6 27.6,313.1 27.6,560.7 "/>
			<linearGradient id="SVGID_3_" gradientUnits="userSpaceOnUse" x1="261.7665" y1="601.0799" x2="413.5801" y2="425.2715">
				<stop  offset="0" style="stop-color:#1681AE"/>
				<stop  offset="1" style="stop-color:#67629D"/>
			</linearGradient>
			<polygon fill="url(#SVGID_3_)" points="296.8,291.6 27.6,560.7 27.6,562.8 563.8,562.8 563.8,558.6 "/>
			</g>

			</svg>
		""")
		val logo = kotlinSvgLogo.raster(0.125)

		//awtShowImageAndWait(logo)

		Application().frame("KorAG: Accelerated Graphics!", icon = logo) {
			var y = 480f
			vertical {
				button("yay!") {
					height = 1.cm
					click {
						y += 100f
					}
				}
				horizontal {
					height = 100.percent - 1.cm
					val canvas = agCanvas {
						width = 100.pt
						height = 100.percent - 1.cm

						val indices = ag.createIndexBuffer(shortArrayOf(0, 1, 2))
						val vertices = ag.createVertexBuffer()

						onRender {
							vertices.upload(floatArrayOf(
								0f, 0f,
								640f, 0f,
								640f, y
							))

							//println("clear")
							ag.clear(Colors.BLUE)

							ag.draw(
								vertices, indices,
								program = DefaultShaders.PROGRAM_DEBUG_WITH_PROJ,
								type = AG.DrawType.TRIANGLES,
								vertexFormat = DefaultShaders.FORMAT_DEBUG,
								vertexCount = 3,
								uniforms = mapOf(
									DefaultShaders.u_ProjMat to Matrix4().setToOrtho(0f, 0f, 640f, 480f, -1f, +1f)
								)
							)

							y--
						}
					}
					button("AG") {
						width = 10.pt
						click {
							y += 20f
						}
					}
					val canvas2 = agCanvas {
						width = 100.pt
						height = 100.percent - 1.cm

						val vertices = ag.createVertexBuffer()

						var y2 = 0.0

						mouseOver {
							y2 = (mouseY.toDouble() / actualHeight.toDouble()) * 480.0
						}

						onRender {
							vertices.upload(floatArrayOf(
								0f, 0f,
								640f, 0f,
								640f, y2.toFloat()
							))

							//println("clear")
							ag.clear(Colors.BLUE)

							ag.draw(
								vertices,
								program = DefaultShaders.PROGRAM_DEBUG_WITH_PROJ,
								type = AG.DrawType.TRIANGLES,
								vertexFormat = DefaultShaders.FORMAT_DEBUG,
								vertexCount = 3,
								uniforms = mapOf(
									DefaultShaders.u_ProjMat to Matrix4().setToOrtho(0f, 0f, 640f, 480f, -1f, +1f)
								)
							)
						}
					}
					spawnAndForget {
						while (true) {
							sleep(1000 / 60)
							canvas.repaint()
							canvas2.repaint()
						}
					}
				}


			}
		}
	}
}