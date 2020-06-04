package com.soywiz.korgw

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.google.gson.Gson
import com.soywiz.kds.Pool
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korge.Korge
import com.soywiz.korio.Korio
import com.soywiz.korio.android.withAndroidContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Suppress("unused")
@SuppressLint("ViewConstructor")
open class KorgwView(
    private val activity: Activity,
    private val playTutorial: Boolean,
    private val fastToolStart: Boolean,
    private val themeId: Int,
    private val widthParam: Int? = null,
    private val heightParam: Int? = null,
    private val callback: KorgwCallbackProtocol?
) : AbstractToolParentView(activity), ToolManager {

    var gameWindow: AndroidGameWindow? = null

    private var mGLView: GLSurfaceView? = null
    lateinit var agOpenGl: AGOpengl

    private lateinit var settings: KorgwSetting

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

        val gameWindow = AndroidGameWindow(activity, this)
        this.gameWindow = gameWindow
        Korio(context) {
            try {

                val settingsJson = Gson().toJson(settings)

                withAndroidContext(activity) {
                    withContext(coroutineContext + gameWindow) {
                        Korge(
                                Korge.Config(
                                        module = ToolModule(
                                                ToolModule.DEFAULT_WIDTH,
                                                ToolModule.DEFAULT_HEIGHT,
                                                ToolExecutor(
                                                        type = CommandType.Start,
                                                        params = settingsJson,
                                                        countdown = !fastToolStart,
                                                        playTutorial = playTutorial,
                                                        themeId = themeId,
                                                        toolManager = this@KorgwView,
                                                        callback = { result ->

                                                            toolStatusCallback?.invoke(result)

                                                            when (result.type) {
                                                                ToolCallbackType.GAME_FINISHED -> {
                                                                    println("------------> RESULT TOOL $result")
                                                                    val statistics = ToolResult.parseSplitAttention((result as GameFinishedToolCallback).statistics)
                                                                    callback?.toolDidFinishWithResult(statistics)
                                                                }
                                                                ToolCallbackType.TUTORIAL_FINISHED -> {
                                                                    callback?.tutorialFinish()
                                                                }
                                                                ToolCallbackType.TUTORIAL_CANCELLED -> {
                                                                    callback?.tutorialCancelled()
                                                                }
                                                                ToolCallbackType.INVALID_GAME_TYPE -> {
                                                                    callback?.invalidGameType()
                                                                }
                                                                else -> {
                                                                }
                                                            }
                                                        }
                                                )
                                        )
                                )
                        )
                    }
                }
            } finally {
                println("${javaClass::getName} completed!")
            }
        }
    }

    override fun cleanUpResources() {

    }

    override fun updateFromSetting(korgwSetting: KorgwSetting?) {
        korgwSetting?.let {
            this.settings = korgwSetting
        }
    }

    override fun updateBluetoothUI() {

    }

    override fun updateBluetoothUI(checkScan: Boolean) {

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun playSound(assetName: String) {
        CoroutineScope(Dispatchers.IO).launch {

            val player = MediaPlayer()

            val descriptor: AssetFileDescriptor
            val assetManager: AssetManager = context.assets

            descriptor = assetManager.openFd(assetName)
            player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length);

            player.setOnCompletionListener {
                player.reset()
                player.release()
                descriptor.close()
            }

            player.prepare()
            player.start()
        }
    }
}
