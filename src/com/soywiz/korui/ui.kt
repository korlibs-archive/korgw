package com.soywiz.korui

open class Component {

}

class Frame(val lc: LightComponents) {

}

fun LightComponents.createFrame(): Frame = Frame(this)
