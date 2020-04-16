package com.soywiz.korag.shader.gl

import com.soywiz.korag.shader.*

fun Shader.toNewGlslString(gles: Boolean = true, version: Int = GlslGenerator.DEFAULT_VERSION, compatibility: Boolean = true) =
	GlslGenerator(this.type, gles, version, compatibility).generate(this.stm)

fun Shader.toNewGlslStringResult(gles: Boolean = true, version: Int = GlslGenerator.DEFAULT_VERSION, compatibility: Boolean = true) =
    GlslGenerator(this.type, gles, version, compatibility).generateResult(this.stm)

fun VertexShader.toGlSlString(gles: Boolean = true, compatibility: Boolean = true) = GlslGenerator(ShaderType.VERTEX, gles, compatibility = compatibility).generate(this.stm)
fun FragmentShader.toGlSlString(gles: Boolean = true, compatibility: Boolean = true) = GlslGenerator(ShaderType.FRAGMENT, gles, compatibility = compatibility).generate(this.stm)
