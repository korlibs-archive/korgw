package com.soywiz.korui.light

import android.app.Activity
import kotlin.coroutines.CoroutineContext

actual object NativeLightsComponentsFactory : LightComponentsFactory {
    actual override fun create(
        context: CoroutineContext,
        nativeCtx: Any?
    ): LightComponents = AndroidLightComponents(context[AndroidCoroutineContext.Key]!!.context as Activity)
}
