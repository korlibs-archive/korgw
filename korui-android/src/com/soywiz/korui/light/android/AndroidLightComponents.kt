package com.soywiz.korui.light.android

import android.app.AlertDialog
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.widget.AbsoluteLayout
import android.widget.Button
import android.widget.ProgressBar
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korui.light.LightClickEvent
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightEvent
import com.soywiz.korui.light.LightResizeEvent
import org.intellij.lang.annotations.Language
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import kotlin.coroutines.suspendCoroutine

class AndroidLightComponents : LightComponents() {
	val activity = KorioAndroidContext
	val scale = KorioAndroidContext.resources.displayMetrics.density

	//fun xml(@Language("xml") xml: String): XmlPullParser {
	//	val factory = XmlPullParserFactory.newInstance()
	//	factory.isNamespaceAware = true
	//	val xpp = factory.newPullParser()
	//	xpp.setInput(StringReader(xml.trim()))
	//	return xpp
	//}
//
	//fun <T : View> createView(@Language("xml") xml: String): T = activity.layoutInflater.inflate(xml(xml), null) as T

	override fun create(type: LightComponents.Type): View {
		return when (type) {
			LightComponents.Type.FRAME -> {
				val activity = KorioAndroidContext
				val view = RootKoruiAbsoluteLayout(KorioAndroidContext)
				//val view = LinearLayout(KorioAndroidContext)
				activity.setContentView(view)
				view
			}
			LightComponents.Type.BUTTON -> {
				Button(KorioAndroidContext)
			}
			LightComponents.Type.CONTAINER -> {
				KoruiAbsoluteLayout(KorioAndroidContext)
				//LinearLayout(KorioAndroidContext)
			}
			LightComponents.Type.PROGRESS -> {
				ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {

				}

				//LinearLayout(KorioAndroidContext)
			}
			else -> {
				View(KorioAndroidContext)
			}
		}
	}

	override fun setParent(c: Any, parent: Any?) {
		println("$parent.addView($c)")
		(parent as ViewGroup).addView(c as View)
		//(parent as ViewGroup).requestLayout()
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
		println("--------------------------")
		println("setBounds: $c, $x, $y, $width, $height")
		if (c is View) {
			if (c is RootKoruiAbsoluteLayout) {

			} else {
				//println(" :::::::::::: $x,$y,$width,$height")
				val layoutParams = c.layoutParams
				if (layoutParams is AbsoluteLayout.LayoutParams) {
					layoutParams.x = (x * scale + 0.5f).toInt()
					layoutParams.y = (y * scale + 0.5f).toInt()
				}
				layoutParams.width = (width * scale + 0.5f).toInt()
				layoutParams.height = (height * scale + 0.5f).toInt()
				c.requestLayout()
			}
		}
	}

	override fun repaint(c: Any) {
		(c as View).requestLayout()
	}

	override fun setText(c: Any, text: String) {
		when (c) {
			is Button -> {
				c.text = text
			}
		}
	}

	override fun setAttributeInt(c: Any, key: String, value: Int) {
		when (key) {
			"background" -> {
			}
		}

		when (c) {
			is ProgressBar -> {
				when (key) {
					"current" -> c.progress = value
					"max" -> c.max = value
				}
			}
		}
	}

	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		when (type) {
			LightClickEvent::class.java -> {
				(c as View).setOnClickListener {
					handler(LightClickEvent(0, 0) as T)
				}
			}
			LightResizeEvent::class.java -> {
				val cc = (c as RootKoruiAbsoluteLayout)
				val ctx = KorioAndroidContext as KoruiActivity
				val viewSize = ctx.window.decorView

				fun send() {
					val display = ctx.windowManager.defaultDisplay
					val size = Point()
					display.getSize(size)
					println("LightResizeEvent: ${size.x}x${size.y}")
					handler(LightResizeEvent((size.x / scale).toInt(), (size.y / scale).toInt()) as T)
				}

				ctx.rotated { send() }
				send()
			}
		}
	}

	suspend override fun dialogAlert(c: Any, message: String): Unit = suspendCoroutine { c ->
		KorioAndroidContext.runOnUiThread {
			val dialog = AlertDialog.Builder(KorioAndroidContext)
				.setTitle(message)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok) { dialog, which ->
					c.resume(Unit)
				}
				//.setNegativeButton(android.R.string.no, android.content.DialogInterface.OnClickListener { dialog, which ->
				//	c.resume(false)
				//})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.show()

			dialog.show()
		}
	}
}