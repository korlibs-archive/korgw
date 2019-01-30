package com.soywiz.korag

import com.jogamp.newt.opengl.*
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.*
import com.soywiz.kgl.*
import com.soywiz.korio.util.*

object AGFactoryAwt : AGFactory {
	override val supportsNativeFrame: Boolean = true
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGAwt(config)
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		val glp = GLProfile.getDefault()
		val caps = GLCapabilities(glp)
		val window = GLWindow.create(caps)
		window.title = title
		window.setSize(width, height)
		window.isVisible = true

		window.addGLEventListener(object : GLEventListener {
			override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = Unit
			override fun display(drawable: GLAutoDrawable) = Unit
			override fun init(drawable: GLAutoDrawable) = Unit
			override fun dispose(drawable: GLAutoDrawable) = Unit
		})

		return object : AGWindow() {
			override fun repaint() = Unit
			override val ag: AG = AGAwtNative(window)
		}
	}
}

abstract class AGAwtBase(val glDecorator: (KmlGl) -> KmlGl = { it }) : AGOpengl() {
	var glprofile = GLProfile.getDefault()
    //val glprofile = GLProfile.get( GLProfile.GL2 )
	var glcapabilities = GLCapabilities(glprofile).apply {
		stencilBits = 8
		depthBits = 24
	}
	lateinit var ad: GLAutoDrawable
	override lateinit var gl: KmlGl
	lateinit var glThread: Thread
	override var isGlAvailable: Boolean = false
	override var devicePixelRatio: Double = 1.0

	fun setAutoDrawable(d: GLAutoDrawable) {
		glThread = Thread.currentThread()
		ad = d
        if (!::gl.isInitialized) {
            //gl = KmlGlCached(JvmKmlGl(d.gl as GL2))
            gl = glDecorator(JvmKmlGl(d.gl as GL2))
        }
		isGlAvailable = true
	}

	val awtBase = this

	//val queue = Deque<(gl: GL) -> Unit>()
}

class AGAwt(val config: AGConfig, glDecorator: (KmlGl) -> KmlGl = { it }) : AGAwtBase(glDecorator), AGContainer {
	val glcanvas = GLCanvas(glcapabilities)
	override val nativeComponent = glcanvas

	override val ag: AG = this

	override fun offscreenRendering(callback: () -> Unit) {
		if (!glcanvas.context.isCurrent) {
			glcanvas.context.makeCurrent()
			try {
				callback()
			} finally {
				glcanvas.context.release()
			}
		} else {
			callback()
		}
	}

	override fun dispose() {
		glcanvas.disposeGLEventListener(glEventListener, true)
	}

	override fun repaint() {
		glcanvas.repaint()
		//if (initialized) {
		//	onRender(this)
		//}
	}

	private val tempFloat4 = FloatArray(4)

	override fun resized(width: Int, height: Int) {
		val (scaleX, scaleY) = glcanvas.getCurrentSurfaceScale(tempFloat4)
		devicePixelRatio = (scaleX + scaleY) / 2.0
		super.resized((width * scaleX).toInt(), (height * scaleY).toInt())
	}

	val glEventListener = object : GLEventListener {
		override fun reshape(d: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
			setAutoDrawable(d)

			//if (isJvm) resized(width, height)
		}

		var onReadyOnce = Once()

		override fun display(d: GLAutoDrawable) {
			setAutoDrawable(d)

			//while (true) {
			//	val callback = synchronized(queue) { if (queue.isNotEmpty()) queue.remove() else null } ?: break
			//	callback(gl)
			//}

			onReadyOnce {
                //println(glcanvas.chosenGLCapabilities.depthBits)
				ready()
			}
			onRender(awtBase)
			gl.flush()

			//gl.glClearColor(1f, 1f, 0f, 1f)
			//gl.glClear(GL.GL_COLOR_BUFFER_BIT)
			//d.swapBuffers()
		}

		override fun init(d: GLAutoDrawable) {
			contextVersion++
			setAutoDrawable(d)
			//println("c")
		}

		override fun dispose(d: GLAutoDrawable) {
			setAutoDrawable(d)
			//println("d")
		}
	}

	init {
		//((glcanvas as JoglNewtAwtCanvas).getNativeWindow() as JAWTWindow).setSurfaceScale(new float[] {2, 2});
		//glcanvas.nativeSurface.
		//println(glcanvas.nativeSurface.convertToPixelUnits(intArrayOf(1000)).toList())

		glcanvas.addGLEventListener(glEventListener)
	}

	//override fun readColor(bitmap: Bitmap32): Unit {
	//	checkErrors {
	//		gl.readPixels(
	//			0, 0, bitmap.width, bitmap.height,
	//			GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
	//			IntBuffer.wrap(bitmap.data)
	//		)
	//	}
	//}

	//override fun readDepth(width: Int, height: Int, out: FloatArray): Unit {
	//	val GL_DEPTH_COMPONENT = 0x1902
	//	checkErrors { gl.readPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL.GL_FLOAT, FloatBuffer.wrap(out)) }
	//}
}

class AGAwtNative(override val nativeComponent: Any, glDecorator: (KmlGl) -> KmlGl = { it }) : AGAwtBase(glDecorator) {

}
