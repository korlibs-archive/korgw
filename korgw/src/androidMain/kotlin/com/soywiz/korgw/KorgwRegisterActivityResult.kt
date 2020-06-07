package com.soywiz.korgw

import android.content.Intent

interface KorgwRegisterActivityResult {
    fun registerActivityResult(handler: (result: Int, data: Intent?) -> Unit): Int
}
