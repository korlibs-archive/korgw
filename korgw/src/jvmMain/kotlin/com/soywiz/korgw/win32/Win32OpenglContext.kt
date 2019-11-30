package com.soywiz.korgw.win32

import Win32
import com.soywiz.kgl.KmlGl
import com.soywiz.korgw.platform.BaseOpenglContext
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinGDI

class Win32OpenglContext(val hWnd: WinDef.HWND, val doubleBuffered: Boolean = false) :
    BaseOpenglContext {
    val hDC = Win32.GetDC(hWnd)

    val pfd = WinGDI.PIXELFORMATDESCRIPTOR.ByReference()
    init {
        pfd.nSize = pfd.size().toShort()
        pfd.nVersion = 1
        pfd.dwFlags = WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL or (if (doubleBuffered) WinGDI.PFD_DOUBLEBUFFER else 0)
        //pfd.dwFlags = WinGDI.PFD_DRAW_TO_WINDOW or WinGDI.PFD_SUPPORT_OPENGL;
        pfd.iPixelType = WinGDI.PFD_TYPE_RGBA.toByte()
        pfd.cColorBits = 32
        //pfd.cColorBits = 24
        pfd.cDepthBits = 16
    }

    val pf = Win32.ChoosePixelFormat(hDC, pfd);

    init {
        Win32.SetPixelFormat(hDC, pf, pfd)
        //DescribePixelFormat(hDC, pf, sizeof(PIXELFORMATDESCRIPTOR), &pfd);

        //val attribs = intArrayOf(
        //    WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
        //    WGL_CONTEXT_MINOR_VERSION_ARB, 3
        //)
    }

    //hRC = wglCreateContextAttribsARB (hDC, null, attribs);
    val hRC = Win32.wglCreateContext(hDC);

    init {
        makeCurrent()
        println("GL_VERSION: " + Win32KmlGl.getString(Win32KmlGl.VERSION))
        println("GL_VENDOR: " + Win32KmlGl.getString(Win32KmlGl.VENDOR))
    }

    override fun makeCurrent() {
        Win32.wglMakeCurrent(hDC, hRC);
    }

    override fun releaseCurrent() {
        Win32.wglMakeCurrent(null, null);
    }

    override fun swapBuffers() {
        Win32.SwapBuffers(hDC)
    }
}
