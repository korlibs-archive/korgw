package com.soywiz.korgw

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.klock.DateTime
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import com.soywiz.korma.geom.setTo
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class KorgwActivity : Activity() {
    var gameWindow: AndroidGameWindow? = null
    private lateinit var mGLView: GLSurfaceView
    lateinit var ag: AGOpengl

    var fps: Int = 60

    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val reshapeEvent = ReshapeEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()

    val touches = Array(10) { Touch(it).apply { active = false } }

    //val touchEvents = Pool { TouchEvent() }

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
            val view = this

            init {
                var contextLost = false
                var surfaceChanged = false
                var initialized = false

                setEGLContextClientVersion(2)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                        //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                        println("---------------- onSurfaceCreated --------------")
                        contextLost = true
                    }

                    override fun onDrawFrame(unused: GL10) {
                        if (contextLost) {
                            contextLost = false
                            ag.contextVersion++
                        }
                        if (!initialized) {
                            initialized = true
                            ag.setViewport(0, 0, width, height)
                            gameWindow?.dispatch(initEvent)
                        }
                        if (surfaceChanged) {
                            surfaceChanged = false
                            ag.setViewport(0, 0, width, height)
                            gameWindow?.dispatch(reshapeEvent.apply {
                                this.x = 0
                                this.y = 0
                                this.width = view.width
                                this.height = view.height
                            })
                        }

                        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                        gameWindow?.coroutineDispatcher?.executePending()
                        gameWindow?.dispatch(renderEvent)
                    }

                    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                        println("---------------- onSurfaceChanged --------------")
                        //ag.contextVersion++
                        //GLES20.glViewport(0, 0, width, height)
                        surfaceChanged = true
                    }
                })
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                val gameWindow = gameWindow ?: return false
                val eventActionIndex = ev.actionIndex
                val eventAction = ev.action
                val eventX = ev.x
                val eventY = ev.y
                //println("onTouchEvent: $eventX, $eventY")
                gameWindow.coroutineDispatcher.dispatch(gameWindow.coroutineContext, Runnable {
                    gameWindow.dispatch(touchEvent.apply {
                        this.touch = touches[eventActionIndex]
                        this.type = when (eventAction) {
                            MotionEvent.ACTION_DOWN -> TouchEvent.Type.START
                            MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
                            MotionEvent.ACTION_UP -> TouchEvent.Type.END
                            else -> TouchEvent.Type.END
                        }
                        if (type == TouchEvent.Type.START) {
                            this.touch.active = true
                            this.touch.startTime = DateTime.now()
                            this.touch.start.setTo(eventX, eventY)
                        }
                        this.touch.currentTime = DateTime.now()
                        this.touch.current.setTo(eventX, eventY)
                        if (type == TouchEvent.Type.END) {
                            this.touch.active = false
                        }
                    })
                })
                return true
            }
        }

        setContentView(mGLView)

        Korio(this) {
            activityMain()
        }
    }

    override fun onResume() {
        //Looper.getMainLooper().
        println("---------------- onResume --------------")
        super.onResume()
    }

    override fun onPause() {
        println("---------------- onPause --------------")
        super.onPause()
    }

    override fun onStop() {
        println("---------------- onStop --------------")
        super.onStop()
    }

    abstract suspend fun activityMain(): Unit
}

