@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.EventLoop
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.ui.agCanvas

object SampleAg {
	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		Application().frame("KorAG: Accelerated Graphics!") {
			agCanvas {
				//println("clear")
				ag.clear(Colors.BLUE)

				val indices = ag.createIndexBuffer(shortArrayOf(0, 1, 2))
				val vertices = ag.createVertexBuffer(floatArrayOf(
					0f, 0f,
					1f, 0f,
					1f, 1f
				))

				ag.draw(
					vertices, indices,
					DefaultShaders.PROGRAM_DEBUG,
					AG.DrawType.TRIANGLES,
					DefaultShaders.FORMAT_DEBUG,
					3
				)
			}
		}
	}
}