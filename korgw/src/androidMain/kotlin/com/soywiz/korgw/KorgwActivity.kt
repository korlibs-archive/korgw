package com.soywiz.korgw

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class KorgwActivity : Activity() {
    var gameWindow: AndroidGameWindow? = null
    private lateinit var mGLView: GLSurfaceView
    lateinit var ag: AGOpengl

    var fps: Int = 60

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("KorgwActivity", "onCreate")
        //println("KorgwActivity.onCreate")

        //ag = AGOpenglFactory.create(this).create(this, AGConfig())
        ag = object : AGOpengl() {
            override val gl: KmlGl = KmlGlAndroid()
            override val nativeComponent: Any get() = this@KorgwActivity
            override val gles: Boolean = true
            override val checkErrors: Boolean = true

            override fun repaint() {
                mGLView.invalidate()
            }
        }

        mGLView = object : GLSurfaceView(this) {
            private val mRenderer: KorgwRenderer
            init {
                setEGLContextClientVersion(2)
                mRenderer = KorgwRenderer()
                setRenderer(mRenderer)
            }
        }

        setContentView(mGLView)

        Korio(this) {
            main()
        }
    }

    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()

    inner class KorgwRenderer : GLSurfaceView.Renderer {
        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
            gameWindow?.dispatch(initEvent)
        }

        override fun onDrawFrame(unused: GL10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            gameWindow?.coroutineDispatcher?.executePending()
            gameWindow?.dispatch(renderEvent)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            gameWindow?.dispatch(reshapeEvent)
        }
    }

    abstract suspend fun main(): Unit
}

