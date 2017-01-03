package com.soywiz.korui.util

import java.util.*

class ObjectPool<T>(val gen: () -> T) {
	@PublishedApi
	internal val items = LinkedList<T>()

	inline fun alloc(callback: (T) -> Unit) {
		val it = allocUnsafe()
		try {
			callback(it)
		} finally {
			releaseUnsafe(it)
		}
	}

	fun allocUnsafe(): T = if (items.isNotEmpty()) items.removeFirst() else gen()

	fun releaseUnsafe(v: T) {
		items += v
	}
}