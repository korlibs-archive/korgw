package com.soywiz.korui

import com.soywiz.korio.async.sync
import com.soywiz.korui.light.log.LogLightComponents
import org.junit.Assert
import org.junit.Test

class BasicTest {
	val lc = LogLightComponents()

	@Test
	fun name() = sync {
		val frame = Application(lc).frame("Title") {

		}
		Assert.assertEquals(
			listOf(
				"create(frame)=0",
				"setVisible(0,true)"
			),
			lc.log
		)
	}
}