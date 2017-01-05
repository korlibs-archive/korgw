package com.soywiz.korui.light.awt

import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.light.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.net.URI
import java.util.concurrent.CancellationException
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener


class AwtLightComponents : LightComponents() {
	init {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		//UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
	}

	override fun create(type: LightType): Any = when (type) {
		LightType.FRAME -> JFrame2().apply {
			defaultCloseOperation = JFrame.EXIT_ON_CLOSE
		}
		LightType.CONTAINER -> JPanel2().apply {
			layout = null
		}
		LightType.BUTTON -> JButton()
		LightType.IMAGE -> JImage()
		LightType.PROGRESS -> JProgressBar(0, 100)
		else -> throw UnsupportedOperationException()
	}

	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		when (type) {
			LightClickEvent::class.java -> {
				(c as Component).addMouseListener(object : MouseAdapter() {
					override fun mouseClicked(e: MouseEvent) {
						EventLoop.queue {
							handler(LightClickEvent(e.x, e.y) as T)
						}
					}
				})
			}
			LightResizeEvent::class.java -> {
				fun send() {
					val cc = (c as JFrame2)
					val cp = cc.contentPane
					EventLoop.queue {
						handler(LightResizeEvent(cp.width, cp.height) as T)
					}
				}

				(c as Frame).addComponentListener(object : ComponentAdapter() {
					override fun componentResized(e: ComponentEvent) {
						send()
					}
				})
				send()
			}
		}
	}

	val Any.actualComponent: Component get() = if (this is JFrame2) this.panel else (this as Component)
	val Any.actualContainer: Container? get() = if (this is JFrame2) this.panel else (this as? Container)

	override fun setParent(c: Any, parent: Any?) {
		parent?.actualContainer?.add((c as Component), 0)
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		//println("setBounds[${c.javaClass.simpleName}]($x, $y, $width, $height) : Thread(${Thread.currentThread().id})")
		when (c) {
			is JFrame2 -> {
				c.panel.preferredSize = Dimension(width, height)
				//c.preferredSize = Dimension(width, height)
				c.pack()
				//c.contentPane.setBounds(x, y, width, height)
			}
			is JComponent -> {
				c.setBounds(x, y, width, height)
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		when (key) {
			LightProperty.VISIBLE -> {
				val visible = value as Boolean
				if (c is JFrame2) {
					if (!c.isVisible && visible) {
						c.setLocationRelativeTo(null)
					}
				}
				(c as Component).isVisible = visible
			}
			LightProperty.TEXT -> {
				val text = value as String
				(c as? JButton)?.text = text
				(c as? Frame)?.title = text
			}
			LightProperty.IMAGE -> {
				val bmp = value as Bitmap?
				val image = (c as? JImage)
				if (bmp is NativeImage) {
					image?.image = bmp.data as BufferedImage
				} else {
					image?.image = bmp?.toBMP32()?.toAwt()
				}
				image?.repaint()
			}
			LightProperty.ICON -> {
				val bmp = value as Bitmap?
				when (c) {
					is JFrame2 -> {
						c.iconImage = bmp?.toBMP32()?.toAwt()
					}
				}
			}
			LightProperty.IMAGE_SMOOTH -> {
				val value = value as Boolean
				when (c) {
					is JImage -> {
						c.smooth = value
					}
				}
			}
			LightProperty.BGCOLOR -> {
				val value = value as Int
				(c as? Component)?.background = Color(value, true)
			}
			LightProperty.PROGRESS_CURRENT -> {
				(c as? JProgressBar)?.value = value as Int
			}
			LightProperty.PROGRESS_MAX -> {
				(c as? JProgressBar)?.maximum = value as Int
			}
		}
		super.setProperty(c, key, value)
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
		val fd = FileDialog(c as JFrame2, "Open file", FileDialog.LOAD)
		fd.isVisible = true
		if (fd.files.isNotEmpty()) {
			LocalVfs(fd.files.first())
		} else {
			throw CancellationException()
		}
	}

	override fun repaint(c: Any) {
		(c as? Component)?.repaint()
	}

	override fun openURL(url: String): Unit {
		val desktop = Desktop.getDesktop()
		desktop.browse(URI(url))
	}
}

class JFrame2 : JFrame() {
	val panel = JPanel2().apply {
		layout = null
	}

	init {
		add(panel)
	}
}

class JPanel2 : JPanel() {
	override fun paintComponent(g: Graphics) {
	}
}

class JImage : JComponent() {
	var image: Image? = null
	var smooth: Boolean = false

	override fun paintComponent(g: Graphics) {
		val g2 = (g as? Graphics2D)
		if (image != null) {
			g2?.setRenderingHint(RenderingHints.KEY_INTERPOLATION, if (smooth) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
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