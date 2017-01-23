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
		Application().frame("KorAG: Accelerated Graphics!", icon = KotlinLogoSvg.raster(0.125)) {
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
								vertexLayout = DefaultShaders.LAYOUT_DEBUG,
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
								vertexLayout = DefaultShaders.LAYOUT_DEBUG,
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