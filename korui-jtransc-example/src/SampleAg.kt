@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.EventLoop
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.cm
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.style.height
import com.soywiz.korui.ui.agCanvas
import com.soywiz.korui.ui.button
import com.soywiz.korui.ui.vertical

object SampleAg {
	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		Application().frame("KorAG: Accelerated Graphics!") {
			vertical {
				button("yay!") {
					height = 1.cm
				}
				agCanvas {
					height = 100.percent - 1.cm

					val indices = ag.createIndexBuffer(shortArrayOf(0, 1, 2))
					val vertices = ag.createVertexBuffer(floatArrayOf(
						0f, 0f,
						640f, 0f,
						640f, 480f
					))

					onRender {
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
					}
				}
			}
		}
	}
}