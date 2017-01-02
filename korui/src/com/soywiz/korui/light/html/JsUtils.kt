package com.soywiz.korui.light.html

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.JsBoundedMethod
import com.jtransc.js.JsDynamic
import com.jtransc.js.get

// @TODO: Remove after upgrading to JTransc 0.5.5
@JTranscMethodBody(target = "js", value = "debugger;")
external fun jsDebugger(): Unit

fun jsNew(clazz: String): JsDynamic? = global[clazz].new2()
fun jsNew(clazz: String, arg1: Any?): JsDynamic? = global[clazz].new2(arg1)
fun jsNew(clazz: String, arg1: Any?, arg2: Any?): JsDynamic? = global[clazz].new2(arg1, arg2)

@JTranscMethodBody(target = "js", value = """
	var clazz = p0, rawArgs = p1;
	var args = [null];
	for (var n = 0; n < rawArgs.length; n++) args.push(N.unbox(rawArgs.data[n]));
	return new (Function.prototype.bind.apply(clazz, args));
""")
external fun JsDynamic?.new2(vararg args: Any?): JsDynamic?

val global: JsDynamic
	@JTranscMethodBody(target = "js", value = "return (typeof(window) != 'undefined') ? window : global;")
	get() = throw java.lang.RuntimeException()

val console: JsDynamic get() = global["console"]!!

@JTranscMethodBody(target = "js", value = """
	var handler = p0;
	return function() {
		return N.unbox(handler['{% METHOD kotlin.jvm.functions.Function0:invoke %}']());
	};
""")
fun <TR> Function0<TR>.toJsDynamic2(): JsDynamic? = throw NotImplementedError()

@JTranscMethodBody(target = "js", value = """
	var handler = p0;
	return function(p1) {
		return N.unbox(handler['{% METHOD kotlin.jvm.functions.Function1:invoke %}'](N.box(p1)));
	};
""")
fun <T1, TR> Function1<T1, TR>.toJsDynamic2(): JsDynamic? = throw NotImplementedError()

fun <TR> jsFunction(v: Function0<TR>): JsDynamic? = v.toJsDynamic2()
fun <T1, TR> jsFunction(v: Function1<T1, TR>): JsDynamic? = v.toJsDynamic2()
fun <T1, T2, TR> jsFunction(v: Function2<T1, T2, TR>): JsDynamic? = v.toJsDynamic2()

@JTranscMethodBody(target = "js", value = """
	var handler = p0;
	return function(p1, p2) {
		return N.unbox(handler['{% METHOD kotlin.jvm.functions.Function2:invoke %}'](N.box(p1), N.box(p2)));
	};
""")
fun <T1, T2, TR> Function2<T1, T2, TR>.toJsDynamic2(): JsDynamic? = throw NotImplementedError()

@JTranscMethodBody(target = "js", value = "return p0|0;")
external fun JsDynamic?.toInt(): Int

@JTranscMethodBody(target = "js", value = "return +p0;")
external fun JsDynamic?.toDouble(): Double

@JTranscMethodBody(target = "js", value = "return p0 == p1;")
external fun JsDynamic?.eq(that: JsDynamic?): Boolean

@JTranscMethodBody(target = "js", value = "return N.istr(p0);")
external fun String.toJavaScriptString(): JsDynamic?

@JTranscMethodBody(target = "js", value = "return p0;")
external fun <T : Any?> JsDynamic?.asJavaType(): T

class JsMethods(val obj: JsDynamic?) {
	operator fun get(name: String) = JsBoundedMethod(obj, name)
}

val JsDynamic?.methods: JsMethods get() = JsMethods(this)

@JTranscMethodBody(target = "js", value = """
	var out = [];
	for (var n = 0; n < p0.length; n++) out.push(N.unbox(p0.data[n]));
	return out;
""")
external fun jsArray(vararg items: Any?): JsDynamic?
