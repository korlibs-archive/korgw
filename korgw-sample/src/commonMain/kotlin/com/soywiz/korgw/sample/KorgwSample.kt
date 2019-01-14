//package com.soywiz.korgw.sample

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*

/*
fun main(args: Array<String>) = Korio {
    DefaultGameWindow.loop {
        configure(1280, 720, "hello", fullscreen = false)
        addEventListener<MouseEvent> { e ->
            if (e.type == MouseEvent.Type.CLICK) {
                toggleFullScreen()
            }
            //    //fullscreen = !fullscreen
            //    configure(1280, 720, "KORGW!", fullscreen = false)
            //}
            //println(e)
        }
        var n = 0
        addEventListener<RenderEvent> {
            //println("render")
            ag.clear(Colors.GREEN.mix(Colors.RED, (n % 60).toDouble() / 60))
            n++
            //ag.flip()
        }
    }
}
*/

fun main(args: Array<String>) = KCube.main(args)

class KCube {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = KCube()
            app.run()
        }
    }

    var init = false

    var rquad = 0.5f
    var rotAxisX = 1f
    var rotAxisY = 1f
    var rotAxisZ = 1f

    val u_ProjMat = Uniform("u_ProjMat", VarType.Mat4)
    val u_ViewMat = Uniform("u_ViewMat", VarType.Mat4)
    val u_ModMat = Uniform("u_ModMat", VarType.Mat4)
    val point = Attribute("point", VarType.Float3, normalized = false)
    val a_col = Attribute("a_Col", VarType.Float3, normalized = true)
    val v_col = Varying("v_Col", VarType.Float3)
    val prog = Program(
        vertex = VertexShader {
            SET(v_col, a_col)
            SET(out, u_ProjMat * u_ViewMat * u_ModMat * vec4(point, 1f.lit))
        },
        fragment = FragmentShader {
            SET(out, vec4(v_col, 1f.lit))
        },
        name = "MY_PROG"
    )

    fun run() = DefaultGameWindow.mainLoop {
            configure(600, 600, "Multicolored cube", fullscreen = false)
            addEventListener<RenderEvent> {
                render(ag)
            }
        }

    fun init(ag:AG) {
        //val gl = (ag as AGAwt).gl ??
        //gl.clearColor(0f,0f,1f,1f)
        //gl.clearDepthf(1.0f)
        //gl.enable(gl.DEPTH_TEST)
        // gl.depthFunc(gl.LEQUAL)
    }

    private var vertices: AG.Buffer? = null
    private var verticesData: FloatArrayList? = null

    fun render(ag:AG) {
        if (!init) {
            init(ag)
            init = true
        }
        //ag.clear(color = Colors.ALICEBLUE, depth = 0f)
        ag.clear(color = Colors.ALICEBLUE, depth = 1f)

        if (vertices == null) {
            vertices = ag.createVertexBuffer()

            // Updating
            verticesData = FloatArrayList()

            fun vertex(x: Float, y: Float, z: Float, color: RGBA) {
                verticesData!!.add(floatArrayOf(x, y, z, color.rf, color.gf, color.bf))
            }

            fun quad(x: Float, y: Float, width: Float, height: Float, color: RGBA, z: Float) {
                val x0 = x
                val x1 = x + width
                val y0 = y
                val y1 = y + height

                vertex(x0, y0, z, color)
                vertex(x1, y0, z, color)
                vertex(x0, y1, z, color)

                vertex(x0, y1, z, color)
                vertex(x1, y0, z, color)
                vertex(x1, y1, z, color)
            }

            fun quad2(x: Float, y: Float, width: Float, height: Float, color: RGBA, z: Float) {
                quad(x - 0.5f, y - 0.5f, width, height, color, z)
            }

            quad2(.0f, .0f, 1f, 1f, Colors.RED, .5f)
            quad2(.5f, .5f, 1f, 1f, Colors.GREEN, .9f)
            quad2(.9f, .9f, 1f, 1f, Colors.BLUE, .75f)

            vertices!!.upload(verticesData!!.toFloatArray())
        }

        //println("$width, $height")

        ag.draw(
            vertices!!,
            program = prog,
            type = AG.DrawType.TRIANGLES,
            vertexLayout = VertexLayout(point,a_col),
            vertexCount = verticesData!!.size / 6,
            uniforms = AG.UniformValues(
                //u_ProjMat to perspective(45.0f, (ag.backWidth.toFloat() / ag.backHeight.toFloat()), 1.0f, 20.0f).toMatrix3D(),
                //u_ProjMat to Matrix3D(),//.setToOrtho(0f, 0f, +2f, +2f, 0f, 1f),
                //u_ProjMat to Matrix3D().setToOrtho2(0f, 0f, +2f, +2f, 0f, 1f),
                u_ProjMat to Matrix3D(),
                u_ViewMat to Matrix3D(),
                u_ModMat to Matrix3D()
            ),
            renderState = AG.RenderState(depthFunc = AG.CompareMode.LESS_EQUAL)
        )

        rquad -= 0.05f

    }
}

/*
const val M00 = 0
const val M10 = 1
const val M20 = 2
const val M30 = 3

const val M01 = 4
const val M11 = 5
const val M21 = 6
const val M31 = 7

const val M02 = 8
const val M12 = 9
const val M22 = 10
const val M32 = 11

const val M03 = 12
const val M13 = 13
const val M23 = 14
const val M33 = 15

fun Matrix3D.setToColumnMajor(
    a00: Float, a01: Float, a02: Float, a03: Float,
    a10: Float, a11: Float, a12: Float, a13: Float,
    a20: Float, a21: Float, a22: Float, a23: Float,
    a30: Float, a31: Float, a32: Float, a33: Float
): Matrix3D {
    val v = data
    v[M00] = a00; v[M10] = a10; v[M20] = a20; v[M30] = a30
    v[M01] = a01; v[M11] = a11; v[M21] = a21; v[M31] = a31
    v[M02] = a02; v[M12] = a12; v[M22] = a22; v[M32] = a32
    v[M03] = a03; v[M13] = a13; v[M23] = a23; v[M33] = a33
    return this
}

fun Matrix3D.setToOrtho2(left: Float, top: Float, right: Float, bottom: Float, near: Float, far: Float): Matrix3D {

    val te = this.data
    val w = 1f / ( right - left )
    val h = 1f / ( top - bottom )
    val p = 1f / ( far - near )

    val x = ( right + left ) * w
    val y = ( top + bottom ) * h
    val z = ( far + near ) * p

    te[ 0 ] = 2f * w;	te[ 4 ] = 0f;	te[ 8 ] = 0f;	te[ 12 ] = - x
    te[ 1 ] = 0f;	te[ 5 ] = 2f * h;	te[ 9 ] = 0f;	te[ 13 ] = - y
    te[ 2 ] = 0f;	te[ 6 ] = 0f;	te[ 10 ] = - 2f * p;	te[ 14 ] = - z
    te[ 3 ] = 0f;	te[ 7 ] = 0f;	te[ 11 ] = 0f;	te[ 15 ] = 1f

    return this

}
*/
