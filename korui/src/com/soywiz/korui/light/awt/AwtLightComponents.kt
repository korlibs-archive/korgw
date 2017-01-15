package com.soywiz.korui.light.awt

import com.soywiz.korag.AG
import com.soywiz.korag.agFactory
import com.soywiz.korim.awt.AwtNativeImage
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.awt.transferTo
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
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.text.JTextComponent


@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class AwtLightComponents : LightComponents() {
	init {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		//UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
	}

	override fun create(type: LightType): LightComponentInfo {
		var agg: AG? = null
		val handle: Component = when (type) {
			LightType.FRAME -> JFrame2().apply {
				defaultCloseOperation = JFrame.EXIT_ON_CLOSE
			}
			LightType.CONTAINER -> JPanel2().apply {
				layout = null
			}
			LightType.BUTTON -> JButton()
			LightType.IMAGE -> JImage()
			LightType.PROGRESS -> JProgressBar(0, 100)
			LightType.LABEL -> JLabel()
			LightType.TEXT_FIELD -> JTextField()
			LightType.CHECK_BOX -> JCheckBox()
			LightType.SCROLL_PANE -> JScrollPane2()
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				agg.nativeComponent as Component
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) this.ag = agg!!
		}
	}

	@Suppress("UNCHECKED_CAST")
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
		val actualParent = (parent as? ChildContainer)?.childContainer ?: parent?.actualContainer
		actualParent?.add((c as Component), 0)
		//println("$parent <- $c")
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
			is Component -> {
				if (c is JScrollPane2) {
					//c.preferredSize = Dimension(100, 100)
					//c.viewport.viewSize = Dimension(100, 100)
					c.viewport.setSize(width, height)
					val rightMost = c.childContainer.components.map { it.bounds.x + it.bounds.width }.max() ?: 0
					val bottomMost = c.childContainer.components.map { it.bounds.y + it.bounds.height }.max() ?: 0
					c.childContainer.preferredSize = Dimension(rightMost, bottomMost)
					c.setBounds(x, y, width, height)
					c.revalidate()
				} else {
					c.setBounds(x, y, width, height)
				}
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		when (key) {
			LightProperty.VISIBLE -> {
				val visible = key[value]
				if (c is JFrame2) {
					if (!c.isVisible && visible) {
						c.setLocationRelativeTo(null)
					}
				}
				(c as Component).isVisible = visible
			}
			LightProperty.TEXT -> {
				val text = key[value]
				(c as? JLabel)?.text = text
				(c as? JTextComponent)?.text = text
				(c as? AbstractButton)?.text = text
				(c as? Frame)?.title = text
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				val image = (c as? JImage)
				if (image != null) {
					if (bmp == null) {
						image.image = null
					} else {
						if (bmp is AwtNativeImage) {
							image.image = bmp.awtImage
						} else {
							if ((image.width != bmp.width) || (image.height != bmp.height)) {
								//println("*********************** RECREATED NATIVE IMAGE!")
								image.image = BufferedImage(bmp.width, bmp.height, BufferedImage.TYPE_INT_ARGB)
							}
							bmp.toBMP32().transferTo(image.image!!)
						}
					}
					image.repaint()
				}
			}
			LightProperty.ICON -> {
				val bmp = key[value]
				when (c) {
					is JFrame2 -> {
						c.iconImage = bmp?.toBMP32()?.toAwt()
					}
				}
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				when (c) {
					is JImage -> {
						c.smooth = v
					}
				}
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				(c as? Component)?.background = Color(v, true)
			}
			LightProperty.PROGRESS_CURRENT -> {
				(c as? JProgressBar)?.value = key[value]
			}
			LightProperty.PROGRESS_MAX -> {
				(c as? JProgressBar)?.maximum = key[value]
			}
			LightProperty.CHECKED -> {
				(c as? JCheckBox)?.isSelected = key[value]
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		return when (key) {
			LightProperty.CHECKED -> {
				(c as? JCheckBox)?.isSelected ?: false
			}
			LightProperty.TEXT -> {
				(c as? JLabel)?.text ?:
					(c as? JTextComponent)?.text ?:
					(c as? AbstractButton)?.text ?:
					(c as? Frame)?.title
			}
			else -> super.getProperty(c, key)
		} as T
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

	override fun getDpi(): Double {
		val sr = Toolkit.getDefaultToolkit().screenResolution
		return sr.toDouble()
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

interface ChildContainer {
	val childContainer: Container
}

class JScrollPane2(override val childContainer: JPanel = JPanel().apply { layout = null }) : JScrollPane(childContainer, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), ChildContainer {
	init {
		border = EmptyBorder(0, 0, 0, 0)
	}
	override fun paintComponent(g: Graphics) {
		g.clearRect(0, 0, width, height)
	}
}

class JPanel2 : JPanel() {
	override fun paintComponent(g: Graphics) {
		//g.clearRect(0, 0, width, height)
	}
}

class JImage : JComponent() {
	var image: BufferedImage? = null
	var smooth: Boolean = false

	override fun paintComponent(g: Graphics) {
		val g2 = (g as? Graphics2D)
		if (image != null) {
			g2?.setRenderingHint(RenderingHints.KEY_INTERPOLATION, if (smooth) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
			g.drawImage(image, 0, 0, width, height, null)
		} else {
			g.clearRect(0, 0, width, height)
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