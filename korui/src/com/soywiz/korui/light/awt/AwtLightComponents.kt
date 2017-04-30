package com.soywiz.korui.light.awt

import com.soywiz.korag.AG
import com.soywiz.korag.agFactory
import com.soywiz.korim.awt.AwtNativeImage
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.awt.transferTo
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
import javax.swing.border.EmptyBorder
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
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
			LightType.TEXT_AREA -> JScrollableTextArea()
			LightType.CHECK_BOX -> JCheckBox()
			LightType.SCROLL_PANE -> JScrollPane2()
			LightType.AGCANVAS -> {
				agg = agFactory.create()
				agg.nativeComponent as Component
			}
			else -> throw UnsupportedOperationException("Type: $type")
		}
		return LightComponentInfo(handle).apply {
			if (agg != null) {
				this.ag = agg!!
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		val uhandler = handler as (LightEvent) -> Unit

		when (type) {
			LightChangeEvent::class.java -> {
				var rc = c as Component
				if (rc is JScrollableTextArea) rc = rc.textArea
				val cc = rc as? JTextComponent
				cc?.document?.addDocumentListener(object : DocumentListener {
					override fun changedUpdate(e: DocumentEvent?) {
						uhandler(LightChangeEvent)
					}

					override fun insertUpdate(e: DocumentEvent?) {
						uhandler(LightChangeEvent)
					}

					override fun removeUpdate(e: DocumentEvent?) {
						uhandler(LightChangeEvent)
					}
				})
			}
			LightMouseEvent::class.java -> {
				val cc = c as Component
				val ev = LightMouseEvent()

				val adapter = object : MouseAdapter() {
					private fun populate(e: MouseEvent, ev: LightMouseEvent, type: LightMouseEvent.Type) {
						ev.type = type
						ev.x = e.x
						ev.y = e.y
						ev.buttons = 1 shl e.button
						ev.isAltDown = e.isAltDown
						ev.isCtrlDown = e.isControlDown
						ev.isShiftDown = e.isShiftDown
						ev.isMetaDown = e.isMetaDown
					}

					private fun handle(e: MouseEvent, type: LightMouseEvent.Type) {
						uhandler(ev.apply { populate(e, this, type) })
					}

					override fun mouseReleased(e: MouseEvent) = handle(e, LightMouseEvent.Type.UP)
					override fun mousePressed(e: MouseEvent) = handle(e, LightMouseEvent.Type.DOWN)
					override fun mouseClicked(e: MouseEvent) = handle(e, LightMouseEvent.Type.CLICK)
					override fun mouseMoved(e: MouseEvent) = handle(e, LightMouseEvent.Type.OVER)
					override fun mouseEntered(e: MouseEvent) = handle(e, LightMouseEvent.Type.ENTER)
					override fun mouseExited(e: MouseEvent) = handle(e, LightMouseEvent.Type.EXIT)
				}

				cc.addMouseListener(adapter)
				cc.addMouseMotionListener(adapter)
				//cc.addMouseWheelListener(adapter)
			}
			LightResizeEvent::class.java -> {
				fun send() {
					val cc = (c as JFrame2)
					val cp = cc.contentPane
					uhandler(LightResizeEvent(cp.width, cp.height))
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
				(c as? JScrollableTextArea)?.text = text
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
								image.image = bmp.toAwt()
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
					(c as? JScrollableTextArea)?.text ?:
					(c as? JTextComponent)?.text ?:
					(c as? AbstractButton)?.text ?:
					(c as? Frame)?.title
			}
			else -> super.getProperty(c, key)
		} as T
	}

	suspend override fun dialogAlert(c: Any, message: String) {
		JOptionPane.showMessageDialog(null, message)
	}

	suspend override fun dialogPrompt(c: Any, message: String): String {
		val jpf = JTextField()
		jpf.addAncestorListener(RequestFocusListener())
		val result = JOptionPane.showConfirmDialog(null, arrayOf(JLabel(message), jpf), "Reply:", JOptionPane.OK_CANCEL_OPTION)
		if (result != JFileChooser.APPROVE_OPTION) throw CancellationException()
		return jpf.text
	}

	suspend override fun dialogOpenFile(c: Any, filter: String): VfsFile {
		val fd = FileDialog(c as JFrame2, "Open file", FileDialog.LOAD)
		fd.isVisible = true
		return if (fd.files.isNotEmpty()) {
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

class JScrollableTextArea(val textArea: JTextArea = JTextArea()) : JScrollPane(textArea) {
	var text: String get() = textArea.text; set(value) {
		textArea.text = value
	}
}

class JScrollPane2(override val childContainer: JPanel = JPanel().apply { layout = null }) : JScrollPane(childContainer, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), ChildContainer {
	init {
		isOpaque = false
		val unitIncrement = 16
		verticalScrollBar.unitIncrement = unitIncrement
		horizontalScrollBar.unitIncrement = unitIncrement
		border = EmptyBorder(0, 0, 0, 0)
	}

	override fun paintComponent(g: Graphics) {
		g.clearRect(0, 0, width, height)
	}
}

class JPanel2 : JPanel() {
	init {
		isOpaque = false
	}

	//override fun paintComponent(g: Graphics) {
	//	g.clearRect(0, 0, width, height)
	//}
	//override fun paintComponent(g: Graphics) {
	//g.clearRect(0, 0, width, height)
	//}
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