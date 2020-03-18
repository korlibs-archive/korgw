package com.soywiz.korag.shaders

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ShadersTest {
	@Test
	fun testGlslGeneration() {
		val vs = VertexShader {
			IF(true.lit) {
				DefaultShaders.t_Temp0 setTo 1.lit * 2.lit
			} ELSE {
				DefaultShaders.t_Temp0 setTo 3.lit * 4.lit
			}
		}

		// @TODO: Optimizer phase!
        assertEqualsShader(vs) {
            "void main()" {
                +"vec4 temp0;"
                "if (true)" {
                    +"temp0 = (1 * 2);"
                }
                "else" {
                    +"temp0 = (3 * 4);"
                }
            }
        }
	}

    val fs = FragmentShader {
        DefaultShaders.apply {
            out setTo vec4(1.lit, 0.lit, 0.lit, 1.lit)
        }
    }

    @Test
    fun testGlslFragmentGenerationOld() {
        assertEqualsShader(fs, version = 100) {
            line("void main()") {
                line("gl_FragColor = vec4(1, 0, 0, 1);")
            }
        }
    }

    @Test
    fun testGlslFragmentGenerationNew() {
        assertEqualsShader(fs, version = 330) {
            line("void main()") {
                line("gl_FragColor = vec4(1, 0, 0, 1);")
            }
            //+"layout(location = 0) out vec4 fragColor;"
            //"void main()" {
            //    +"fragColor = vec4(1, 0, 0, 1);"
            //}
        }
    }

    fun assertEqualsShader(shader: Shader, version: Int = GlslGenerator.DEFAULT_VERSION, gles: Boolean = false, block: Indenter.() -> Unit) {
        assertEquals(
            Indenter {
                block()
            }.toString(),
            shader.toNewGlslString(gles = gles, version = version)
        )
    }
}
