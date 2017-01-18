package korui.soywiz.com.koruiandroidexample1

import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korau.sound.readSound
import com.soywiz.korim.android.androidShowImage
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.geom.Anchor
import com.soywiz.korim.geom.ScaleMode
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korio.async.*
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.vfs.ExternalStorageVfs
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.Application
import com.soywiz.korui.frame
import com.soywiz.korui.geom.len.cm
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.light.android.KoruiActivity
import com.soywiz.korui.style.bottom
import com.soywiz.korui.style.height
import com.soywiz.korui.style.relativeTo
import com.soywiz.korui.style.right
import com.soywiz.korui.ui.*
import java.net.URI


@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class MainActivity : KoruiActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
	val svg = SVG("""
		<svg height="512" width="512">
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

	val requestPermissionSignal = Signal<Boolean>()

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		val result = (grantResults[0] == PackageManager.PERMISSION_GRANTED)
		requestPermissionSignal(result)
		//super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	suspend override fun requestPermission(name: String): Boolean = asyncFun {
		ActivityCompat.requestPermissions(this, arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), 0)
		requestPermissionSignal.waitOne()
	}

	suspend override fun main(args: Array<String>) = asyncFun {
		val that = this@MainActivity

		val external = ExternalStorageVfs();

		println("Listing $external")
		println(external.list().toList())
		println("Watching $external:")
		external.watch {
			println(it)
		}
		external["file.txt"].writeString("hello")
		println(external.list().toList())
		external["file1.txt"].writeString("hello")
		external["file2.txt"].writeString("hello")
		external["file3.txt"].writeString("hello")

		// Since this code is all generic, you can create
		// a common project containing this and referenced here!
		Application().frame("Hello World") {
			//button("hello from korui") {
			//	for (file in ResourcesVfs.listRecursive()) {
			//		println(file)
			//	}
			//	alert("done!")
			//}
			scrollPane {
				horizontal {
					button("hello from korui").click {
						for (file in ResourcesVfs.listRecursive()) {
							println(file)
						}
						alert("done!")
					}
					button("show image").click {
						//androidShowImage(ResourcesVfs["kotlin.png"].readBitmap())

						androidShowImage(NativeImage(512, 512, svg))
					}
				}
				val progress = progress(0, 100)
				horizontal {
					button("start").click {
						while (progress.current < progress.max) {
							progress.current++
							sleep(50)
						}
						alert("done!")
						progress.current = 0
					}
				}
				label("Name:")
				val name = textField("Test")
				val adult = checkBox("I'm and adult", checked = true)
				button("Apply") {
					height = 0.5.cm
				}.click {
					if (adult.checked) {
						alert("Hello ${name.text}!")
					} else {
						alert("Not an adult!")
					}
				}
				var y = 480f

				val canvas2 = agCanvas {
					//width = 100.pt
					height = 100.pt

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
						canvas2.repaint()
						y--
						if (y < 0) y = 480f
					}
				}

				layersKeepAspectRatio(anchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.SHOW_ALL) {
					style.height = 5.cm
					vectorImage(svg)
				}
			}

			relative {
				val sound = ResourcesVfs["sound.mp3"].readSound()
				val hello = button("sound") {
					right = 10.pt
					bottom = 10.pt
				}.click {
					sound.play()
					alert("Sound length: ${sound.lengthInMs}")
				}

				val world = button("websocket") {
					relativeTo = hello
					right = 10.pt
				}.click {
					val ws = WebSocketClient(URI("ws://echo.websocket.org"), debug = true)
					spawn {
						for (message in ws.onAnyMessage) {
							when (message) {
								is String -> println("recv.text: $message")
								is ByteArray -> println("recv.binary: ${message.toList()}")
							}

						}
					}
					ws.send("hello")
					ws.send(byteArrayOf(1, 2, 3, 4))
				}
			}

			//button("hello from korui")
		}

		//val layout = LinearLayout(this@MainActivity).apply {
		//	addView(LinearLayout(this@MainActivity).apply {
		//		addView(Button(this@MainActivity))
		//	})
		//}
		//setContentView(layout)
		Unit
	}
}
