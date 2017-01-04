package com.soywiz.korui.light.log

import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightProperty
import com.soywiz.korui.light.LightType

class LogLightComponents : LightComponents() {
	val log = arrayListOf<String>()
	var lastId = 0

	override fun create(type: LightType): Any {
		val id = lastId++
		log += "create($type)=$id"
		return id
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
		log += "setProperty($c,$key,$value)"
	}
}