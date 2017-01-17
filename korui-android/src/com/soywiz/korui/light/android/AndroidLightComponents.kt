@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korui.light.android

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.soywiz.korim.android.toAndroidBitmap
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korio.android.KorioApp
import com.soywiz.korui.light.*
import kotlin.coroutines.suspendCoroutine

class AndroidLightComponents : LightComponents() {
	val activity = KorioAndroidContext
	//val scale = KorioAndroidContext.resources.displayMetrics.density
	//fun scaled(v: Double): Double = v * scale + 0.5f
	//fun scaled(v: Int): Double = v * scale + 0.5f
	//fun scaled_rev(v: Double): Double = v / scale
	//fun scaled_rev(v: Int): Int = v / scale

	fun scaled(v: Double): Double = v
	fun scaled(v: Int): Int = v
	fun scaled_rev(v: Double): Double = v
	fun scaled_rev(v: Int): Int = v

	override fun create(type: LightType): LightComponentInfo {
		val handle = when (type) {
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
			LightType.LABEL -> {
				TextView(activity)
			}
			LightType.TEXT_FIELD -> {
				EditText(activity).apply {
					setSingleLine()
				}
			}
			LightType.CHECK_BOX -> {
				CheckBox(activity)
			}
			LightType.SCROLL_PANE -> {
				ScrollView2(activity)
			}
			LightType.IMAGE -> {
				ImageView(activity)
			}
			else -> {
				View(KorioAndroidContext)
			}
		}
		return LightComponentInfo(handle)
	}

	override fun setParent(c: Any, parent: Any?) {
		println("$parent.addView($c)")
		val actualParent = (parent as? ChildContainer)?.group ?: parent
		(actualParent as ViewGroup).addView(c as View)
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
					layoutParams.x = scaled(x)
					layoutParams.y = scaled(y)
				}
				layoutParams.width = scaled(width)
				layoutParams.height = scaled(height)
				c.requestLayout()
			}
		}
	}

	override fun repaint(c: Any) {
		(c as View).requestLayout()
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		val cc = c as View
		when (key) {
			LightProperty.TEXT -> {
				val v = key[value]
				(cc as? TextView)?.text = v
			}
			LightProperty.CHECKED -> {
				val v = key[value]
				(cc as? CheckBox)?.isChecked = v
			}
			LightProperty.BGCOLOR -> {
			}
			LightProperty.PROGRESS_CURRENT -> {
				val v = key[value]
				(cc as? ProgressBar)?.progress = v
			}
			LightProperty.PROGRESS_MAX -> {
				val v = key[value]
				(cc as? ProgressBar)?.max = v
			}
			LightProperty.IMAGE -> {
				val v = key[value]
				(cc as? ImageView)?.setImageBitmap(v?.toAndroidBitmap())
			}
		}
	}

	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
		val cc = c as View
		return key[when (key) {
			LightProperty.CHECKED -> (cc as? CheckBox)?.isChecked
			LightProperty.TEXT -> (cc as? TextView)?.text?.toString()
			else -> super.getProperty(c, key)
		}]
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : LightEvent> setEventHandlerInternal(c: Any, type: Class<T>, handler: (T) -> Unit) {
		when (type) {
			LightClickEvent::class.java -> {
				(c as View).setOnClickListener {
					handler(LightClickEvent(0, 0) as T)
				}
			}
			LightResizeEvent::class.java -> {
				val cc = (c as RootKoruiAbsoluteLayout)
				//val ctx = activity as KoruiActivity

				fun send() {
					val sizeX = scaled_rev((cc.parent as View).width)
					val sizeY = scaled_rev((cc.parent as View).height)
					println("LightResizeEvent($sizeX, $sizeY)")
					handler(LightResizeEvent(sizeX, sizeY) as T)
				}

				KorioApp.resized { send() }
				send()
			}
		}
	}

	suspend override fun dialogAlert(c: Any, message: String): Unit = suspendCoroutine { c ->
		KorioAndroidContext.runOnUiThread {
			val dialog = AlertDialog.Builder(KorioAndroidContext)
				.setTitle(message)
				.setMessage(message)
				.setPositiveButton(android.R.string.ok) { _, _ ->
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

	override fun getDpi(): Double {
		val metrics = activity.resources.displayMetrics
		return metrics.densityDpi.toDouble()
	}
}

interface ChildContainer {
	val group: ViewGroup
}

class ScrollView2(context: Context, override val group: KoruiAbsoluteLayout = KoruiAbsoluteLayout(context)) : ScrollView(context), ChildContainer {
	init {
		addView(group)
	}
}