package com.soywiz.korui.awt

import com.soywiz.kimage.awt.toAwt
import com.soywiz.kimage.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.LightComponents
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.util.concurrent.CancellationException
import javax.swing.*

class AwtLightComponents : LightComponents() {
    init {
        //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    override fun create(type: String): Any = when (type) {
        TYPE_FRAME -> JFrame().apply {
            layout = null
        }
        TYPE_CONTAINER -> JPanel2().apply {
            layout = null
        }
        TYPE_BUTTON -> JButton("hello")
        TYPE_IMAGE -> JImage()
        else -> throw UnsupportedOperationException()
    }

    override fun <T : LightComponents.Event> setEventHandler(c: Any, type: Class<T>, handler: (T) -> Unit) {
        when (type) {
            LightComponents.ClickEvent::class.java -> {
                (c as Component).addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        handler(LightComponents.ClickEvent() as T)
                    }
                })
            }
            LightComponents.ResizeEvent::class.java -> {
                (c as Frame).addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        handler(LightComponents.ResizeEvent(e.component.width, e.component.height) as T)
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
        //(c as Component).repaint()
        //(c as Component).preferredSize = Dimension(width, height)
        //(c as Component).minimumSize = Dimension(width, height)
        //(c as Component).maximumSize = Dimension(width, height)
    }

    override fun setVisible(c: Any, visible: Boolean) {
        (c as Component).isVisible = visible
    }

    override fun setText(c: Any, text: String) {
        (c as? Button)?.label = text
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

    override fun setImage(c: Any, bmp: Bitmap?) {
        val image = (c as? JImage)
        image?.image = bmp?.toBMP32()?.toAwt()
        //label?.horizontalTextPosition = SwingConstants.LEFT
        //label?.verticalTextPosition = SwingConstants.TOP
        //label?.icon = if (bmp != null) ImageIcon(bmp.toBMP32().toAwt()) else null
    }

    override fun repaint(c: Any) {
        (c as? Component)?.repaint()
    }
}

class JPanel2 : JPanel() {
    override fun paintComponent(g: Graphics) {
    }
}

class JImage : JComponent() {
    var image: Image? = null

    override fun paintComponent(g: Graphics) {
        if (image != null) {
            g.drawImage(image, 0, 0, width, height, null)
        }
        //super.paintComponent(g)
    }
}