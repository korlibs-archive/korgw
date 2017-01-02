package com.soywiz.korui.light.awt

import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.light.LightClickEvent
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightEvent
import com.soywiz.korui.light.LightResizeEvent
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CancellationException
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener


class AwtLightComponents : LightComponents() {
	init {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		//UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
	}

	override fun create(type: String): Any = when (type) {
		TYPE_FRAME -> JFrame().apply {
			layout = null
			defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		}
		TYPE_CONTAINER -> JPanel2().apply {
			layout = null
		}
		TYPE_BUTTON -> JButton()
		TYPE_IMAGE -> JImage()
		TYPE_PROGRESS -> JProgressBar(0, 100)
		else -> throw UnsupportedOperationException()
	}

	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		when (type) {
			LightClickEvent::class.java -> {
				(c as Component).addMouseListener(object : MouseAdapter() {
					override fun mouseClicked(e: MouseEvent) {
						handler(LightClickEvent(e.x, e.y) as T)
					}
				})
			}
			LightResizeEvent::class.java -> {
				(c as Frame).addComponentListener(object : ComponentAdapter() {
					override fun componentResized(e: ComponentEvent) {
						val c = e.component
						if (c is JFrame) {
							c.contentPane.setSize(c.size)
							//println("Resized!: ${c.size}")
						}
						handler(LightResizeEvent(e.component.width, e.component.height) as T)
					}
				})
			}
		}
	}

	override fun setParent(c: Any, parent: Any?) {
		(parent as? Container)?.add((c as Component), 0)
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
		if (c is JFrame) {
			if (!c.isVisible && visible) {
				c.setLocationRelativeTo(null)
			}
		}
		(c as Component).isVisible = visible
	}

	override fun setText(c: Any, text: String) {
		(c as? JButton)?.text = text
		(c as? Frame)?.title = text
	}

	suspend override fun dialogAlert(c: Any, message: String) = asyncFun {
		JOptionPane.showMessageDialog(null, message)
	}

	suspend override fun dialogPrompt(c: Any, message: String): String = asyncFun {
		val jpf = JTextField()
		jpf.addAncestorListener(RequestFocusListener())
		val result = JOptionPane.showConfirmDialog(null, arrayOf(JLabel(message), jpf), "Reply:", JOptionPane.OK_CANCEL_OPTION)
		if (result == JFileChooser.APPROVE_OPTION) {
			jpf.text
		} else {
			throw CancellationException()
		}
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
		//(c as? Component)?.repaint()
		//label?.horizontalTextPosition = SwingConstants.LEFT
		//label?.verticalTextPosition = SwingConstants.TOP
		//label?.icon = if (bmp != null) ImageIcon(bmp.toBMP32().toAwt()) else null
	}

	override fun setAttributeBitmap(handle: Any, key: String, value: Bitmap?) {
		when (handle) {
			is JFrame -> {
				when (key) {
					"icon" -> {
						handle.iconImage = value?.toBMP32()?.toAwt()
					}
				}
			}
		}
	}

	override fun repaint(c: Any) {
		(c as? Component)?.repaint()
	}

	override fun setAttributeString(c: Any, key: String, value: String) {
	}

	override fun setAttributeInt(c: Any, key: String, value: Int) {
		when (c) {
			is JProgressBar -> {
				when (key) {
					"current" -> c.value = value
					"max" -> c.maximum = value
				}
			}
		}
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

class RequestFocusListener(private val removeListener: Boolean = true) : AncestorListener {
	override fun ancestorAdded(e: AncestorEvent) {
		val component = e.component
		component.requestFocusInWindow()
		if (removeListener) component.removeAncestorListener(this)
	}

	override fun ancestorMoved(e: AncestorEvent) {}

	override fun ancestorRemoved(e: AncestorEvent) {}
}