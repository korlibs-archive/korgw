package com.soywiz.korui.awt

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.LightComponents
import java.awt.Button
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CancellationException
import javax.swing.*

class AwtLightComponents : LightComponents() {
    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    override fun create(type: String): Any = when (type) {
        TYPE_FRAME -> JFrame().apply {
            layout = null
        }
        TYPE_BUTTON -> JButton("hello")
        else -> throw UnsupportedOperationException()
    }

    override fun setEventHandler(c: Any, type: String, handler: () -> Unit) {
        when (type) {
            EVENT_CLICK -> {
                (c as Component).addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        handler()
                    }
                })
            }
        }
    }

    override fun setParent(c: Any, parent: Any?) {
        (parent as? Container)?.add((c as Component))
        (parent as? JFrame)?.pack()
    }

    override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
        (c as Component).setBounds(x, y, width, height)
    }

    override fun setVisible(c: Any, visible: Boolean) {
        (c as Component).isVisible = visible
    }

    suspend override fun dialogAlert(c: Any, message: String) = asyncFun {
        JOptionPane.showMessageDialog(null, message)
    }

    suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile = asyncFun {
        val fc = JFileChooser()
        val result = fc.showOpenDialog((c as Component))
        if (result == JFileChooser.APPROVE_OPTION) {
            LocalVfs(fc.selectedFile)
        } else {
            throw CancellationException()
        }
    }
}