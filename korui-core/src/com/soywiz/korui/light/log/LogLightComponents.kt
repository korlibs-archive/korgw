package com.soywiz.korui.light.log

import com.soywiz.korui.light.LightComponents

class LogLightComponents : LightComponents() {
    val log = arrayListOf<String>()
    var lastId = 0

    override fun create(type: String): Any {
        val id = lastId++
        log += "create($type)=$id"
        return id
    }

    override fun setVisible(c: Any, visible: Boolean) {
        log += "setVisible($c,$visible)"
    }
}