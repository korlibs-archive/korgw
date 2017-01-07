package com.soywiz.korui.geom.len

val Int.mm: Length get() = Length.MM(this)
//val Int.px: Length get() = Length.PX(this)
val Int.pt: Length get() = Length.PT(this)
val Int.em: Length get() = Length.EM(this)
val Int.percent: Length get() = Length.Ratio(this.toDouble() / 100.0)
val Double.ratio: Length get() = Length.Ratio(this)
