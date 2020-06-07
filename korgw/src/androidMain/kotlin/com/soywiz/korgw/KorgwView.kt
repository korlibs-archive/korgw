package com.soywiz.korgw

import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ViewGroup
import com.soywiz.kds.Pool
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import com.soywiz.korio.android.withAndroidContext
import kotlinx.coroutines.withContext
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Suppress("unused")
@SuppressLint("ViewConstructor")
abstract class KorgwView(
    val activity: Activity,
    private val playTutorial: Boolean,
    private val fastToolStart: Boolean,
    private val themeId: Int,
    private val widthParam: Int? = null,
    private val heightParam: Int? = null
) : ViewGroup(activity), KorgwRegisterActivityResult {

    //val context: Context get() = activity

    var gameWindow: AndroidGameWindow? = null

    private var mGLView: GLSurfaceView? = null
    lateinit var agOpenGl: AGOpengl

    private val pauseEvent = PauseEvent()
    private val resumeEvent = ResumeEvent()
    private val destroyEvent = DestroyEvent()
    private val renderEvent = RenderEvent()
    private val initEvent = InitEvent()
    private val disposeEvent = DisposeEvent()
    private val fullScreenEvent = FullScreenEvent()
    private val keyEvent = KeyEvent()
    private val mouseEvent = MouseEvent()
    private val touchEvent = TouchEvent()
    private val dropFileEvent = DropFileEvent()

    inner class KorgeViewAGOpenGL : AGOpengl() {

        override val gl: KmlGl = KmlGlAndroid()
        override val nativeComponent: Any get() = this@KorgwView
        override val gles: Boolean = true

        override fun repaint() {
            mGLView?.invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        agOpenGl = KorgeViewAGOpenGL()

        mGLView = object : GLSurfaceView(context) {

            val view = this

            init {

                var contextLost = false
                var surfaceChanged = false
                var initialized = false

                setEGLContextClientVersion(2)
                setRenderer(object : Renderer {
                    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                        contextLost = true
                    }

                    override fun onDrawFrame(unused: GL10) {

                        if (contextLost) {
                            contextLost = false
                            agOpenGl.contextLost()
                        }

                        if (!initialized) {
                            initialized = true
                            agOpenGl.setViewport(0, 0, width, height)
                            gameWindow?.dispatch(initEvent)
                        }

                        if (surfaceChanged) {
                            surfaceChanged = false
                            agOpenGl.setViewport(0, 0, width, height)
                            gameWindow?.dispatchReshapeEvent(0, 0, view.width, view.height)
                        }

                        gameWindow?.coroutineDispatcher?.executePending()
                        gameWindow?.dispatch(renderEvent)
                    }

                    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                        surfaceChanged = true
                    }
                })
            }

            private val touchesEventPool = Pool { TouchEvent() }
            private val coordinates = MotionEvent.PointerCoords()
            private var lastTouchEvent: TouchEvent = TouchEvent()

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(ev: MotionEvent): Boolean {

                val gameWindow = gameWindow ?: return false

                for (n in 0 until ev.pointerCount) {

                    val currentTouchEvent = synchronized(touchesEventPool) {
                        val currentTouchEvent = touchesEventPool.alloc()
                        currentTouchEvent.copyFrom(lastTouchEvent)

                        currentTouchEvent.startFrame(
                                when (ev.action) {
                                    MotionEvent.ACTION_DOWN -> TouchEvent.Type.START
                                    MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
                                    MotionEvent.ACTION_UP -> TouchEvent.Type.END
                                    else -> TouchEvent.Type.END
                                }
                        )

                        ev.getPointerCoords(n, coordinates)
                        currentTouchEvent.touch(ev.getPointerId(n), coordinates.x.toDouble(), coordinates.y.toDouble())

                        lastTouchEvent.copyFrom(currentTouchEvent)
                        currentTouchEvent
                    }

                    gameWindow.coroutineDispatcher.dispatch(gameWindow.coroutineContext, Runnable {
                        gameWindow.dispatch(currentTouchEvent)
                    })

                }

                return true
            }
        }

        addView(mGLView)

        val gameWindow = AndroidGameWindow(null, this)
        this.gameWindow = gameWindow
        Korio(context) {
            try {
                withAndroidContext(activity) {
                    withContext(coroutineContext + gameWindow) {
                        viewMain()
                    }
                }
            } finally {
                println("${javaClass::getName} completed!")
            }
        }
    }

    abstract fun viewMain()
}
