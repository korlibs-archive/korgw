package com.soywiz.korui

import com.soywiz.korio.async.sync
import com.soywiz.korui.light.log.LogLightComponents
import com.soywiz.korui.ui.button
import org.junit.Assert
import org.junit.Test

class BasicTest {
	val lc = LogLightComponents()

	@Test
	fun name() = sync {
		val frame = Application(lc).frame("Title") {
			button("Hello")
		}
		Assert.assertEquals(
			"""
				create(FRAME)=0
				setProperty(0,LightProperty[TEXT],Title)
				setBounds(0,0,0,640,480)
				create(BUTTON)=1
				setProperty(1,LightProperty[TEXT],Hello)
				setParent(1,0)
				setBounds(0,0,0,640,480)
				setProperty(0,LightProperty[VISIBLE],true)
				setBounds(1,0,0,640,480)
				setBounds(0,0,0,640,480)
			""".trimIndent(),
			lc.log.joinToString("\n")
		)
	}
}