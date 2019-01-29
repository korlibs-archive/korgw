package com.soywiz.korgw

import com.soywiz.korim.bitmap.Bitmap
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.EAGL.*
import platform.Foundation.*
import platform.GLKit.*
import platform.UIKit.*
import platform.gles2.*

/*
open class GameViewController : GLKViewController() {
    var context: EAGLContext? = null

    //deinit {
    //    self.tearDownGL()
    //    if EAGLContext.current() === self.context {
    //        EAGLContext.setCurrent(nil)
    //    }
    //}

    override fun viewDidLoad() {
        super.viewDidLoad()

        context = EAGLContext(kEAGLRenderingAPIOpenGLES2)
        if (context == null) {
            println("Failed to create ES context")
        }

        val view = this.view as GLKView
        view.context = this.context!!
        view.drawableDepthFormat = GLKViewDrawableDepthFormat24
        this.setupGL()
    }

    override fun didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()

        if (this.isViewLoaded() && this.view.window != null) {
            //this.view = nil

            this.tearDownGL()


            if (EAGLContext.currentContext() === this.context) {
                EAGLContext.setCurrentContext(null)
            }
            this.context = null
        }
    }

    open fun setupGL() {
        EAGLContext.setCurrentContext(this.context)

        // Change the working directory so that we can use C code to grab resource files
        val path = NSBundle.mainBundle.resourcePath
        if (path != null) {
            NSFileManager.defaultManager.changeCurrentDirectoryPath(path)
        }

        engineInitialize()

        var width = 0.0
        var height = 0.0

        view.frame.useContents {
            width = this.size.width
            height = this.size.width
        }
        engineResize(width, height)
    }

    open fun tearDownGL() {
        EAGLContext.setCurrentContext(context)
        engineFinalize()
    }

    open fun engineInitialize() {
    }

    open fun engineFinalize() {
    }

    open fun engineResize(width: Double, height: Double) {
    }

    open fun engineRender() {
        glClearColor(1f, .5f, .2f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
    }

    external override fun glkView(view: GLKView, drawInRect: CValue<CGRect>) {
        engineRender()
    }
}
*/
