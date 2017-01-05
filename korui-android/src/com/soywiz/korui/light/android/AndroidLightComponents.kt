package com.soywiz.korui.light.android

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.AbsoluteLayout
import android.widget.Button
import android.widget.ProgressBar
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korui.light.*
import kotlin.coroutines.suspendCoroutine

class AndroidLightComponents : LightComponents() {
	val activity = KorioAndroidContext
	val scale = KorioAndroidContext.resources.displayMetrics.density

	override fun create(type: LightType): View {
		return when (type) {
			LightType.FRAME -> {
				val view = RootKoruiAbsoluteLayout(activity)
				view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT)
				activity.setContentView(view)
				view
			}
			LightType.BUTTON -> {
				Button(KorioAndroidContext)
			}
			LightType.CONTAINER -> {
				KoruiAbsoluteLayout(KorioAndroidContext)
			}
			LightType.PROGRESS -> {
				ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
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
		//println("--------------------------")
		//println("setBounds[${c.javaClass.simpleName}]($x, $y, $width, $height)")
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

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		val c = c as View
		when (key) {
			LightProperty.TEXT -> {
				val v = value as String
				(c as? Button)?.text = v
			}
			LightProperty.BGCOLOR -> {
			}
			LightProperty.PROGRESS_CURRENT -> {
				val v = value as Int
				(c as? ProgressBar)?.progress = v
			}
			LightProperty.PROGRESS_MAX -> {
				val v = value as Int
				(c as? ProgressBar)?.max = v
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
				val ctx = activity as KoruiActivity

				fun send() {
					val sizeX = ((cc.parent as View).width / scale).toInt()
					val sizeY = ((cc.parent as View).height / scale).toInt()
					println("LightResizeEvent($sizeX, $sizeY)")
					handler(LightResizeEvent(sizeX, sizeY) as T)
				}

				KuroiApp.resized { send() }
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