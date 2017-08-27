package com.soywiz.korui.light.haxelime

import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightComponentsFactory

class HaxeLimeLightComponentsFactory : LightComponentsFactory() {
	override fun create(): LightComponents = HaxeLimeLightComponents()
}

class HaxeLimeLightComponents : LightComponents() {

}