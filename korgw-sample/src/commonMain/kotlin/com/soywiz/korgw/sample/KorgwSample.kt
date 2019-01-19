package com.soywiz.korgw.sample

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*
import kotlin.math.*
import com.soywiz.korio.Korio

class KCube {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = KCube()
            app.run()
        }

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
        val vertexLayout = VertexLayout(point, a_col)

        private val FLOATS_PER_VERTEX = vertexLayout.totalSize / Int.SIZE_BYTES /*Float.SIZE_BYTES is not defined*/
    }

    val cubeSize = 1f

    var width = 400
    var height = 400
    var ar = (width.toFloat() / height.toFloat())
    val aov = Angle.fromDegrees(45f)
    val near = 1f
    val far = 20f
    val xPos = 0f
    val yPos = 0f
    val zPos = -5f
    var rquad = Angle.fromDegrees(0)
    var rotAxis = Vector3D(1f, 1f, 1f)

    val rs = AG.RenderState(depthFunc = AG.CompareMode.LESS_EQUAL)

    var projMat = Matrix3D().setToPerspective(aov, ar, near, far)
    var viewMat = Matrix3D().setToTranslation(xPos, yPos, zPos)
    var modlMat = Matrix3D().identity()
    private val uniformValues = AG.UniformValues()

    fun run() = Korio {
        DefaultGameWindow.loop {
            //TODO: why can't I use width and height vars here ?
            configure(400, 400, "Multicolored cube", fullscreen = false)
            addEventListener<ReshapeEvent> {
                reshape(it, ag)
            }
            addEventListener<RenderEvent> {
                render(ag)
            }
        }
    }

    fun reshape(event: ReshapeEvent, ag: AG) {
        width = max(event.width, 1)
        height = max(event.height, 1)
        ar = (width.toFloat() / height.toFloat())
        ag.setViewport(0, 0, width, height)
        projMat.setToPerspective(aov, ar, near, far)
    }

    private lateinit var vertexBuffer: AG.Buffer
    private var vertexCount: Int = 0

    fun render(ag: AG) {
        ag.clear(color = Colors.BLACK, depth = far)

        if (!::vertexBuffer.isInitialized) {
            vertexBuffer = ag.createVertexBuffer()
            vertexBuffer.upload(floatArrayListOf().apply {
                this += getCubeVertices()
            }.also { vertexCount = it.size / FLOATS_PER_VERTEX }.toFloatArray())
        }

        ag.draw(
            vertexBuffer,
            program = prog,
            type = AG.DrawType.TRIANGLES,
            vertexLayout = vertexLayout,
            vertexCount = vertexCount ,
            uniforms = uniformValues.apply {
                this[u_ProjMat] = projMat
                this[u_ViewMat] = viewMat
                this[u_ModMat] = modlMat.identity().setToRotation(rquad, rotAxis)
            },
            renderState = rs
        )

        rquad += 0.5.degrees

    }

    fun getCubeVertices(): FloatArray = floatArrayOf(
        -cubeSize, -cubeSize, -cubeSize,  1f, 0f, 0f,  //p1
        -cubeSize, -cubeSize, +cubeSize,  1f, 0f, 0f,  //p2
        -cubeSize, +cubeSize, +cubeSize,  1f, 0f, 0f,  //p3
        -cubeSize, -cubeSize, -cubeSize,  1f, 0f, 0f,  //p1
        -cubeSize, +cubeSize, +cubeSize,  1f, 0f, 0f,  //p3
        -cubeSize, +cubeSize, -cubeSize,  1f, 0f, 0f,  //p4

        +cubeSize, +cubeSize, -cubeSize,  0f, 1f, 0f,  //p5
        -cubeSize, -cubeSize, -cubeSize,  0f, 1f, 0f,  //p1
        -cubeSize, +cubeSize, -cubeSize,  0f, 1f, 0f,  //p4
        +cubeSize, +cubeSize, -cubeSize,  0f, 1f, 0f,  //p5
        +cubeSize, -cubeSize, -cubeSize,  0f, 1f, 0f,  //p7
        -cubeSize, -cubeSize, -cubeSize,  0f, 1f, 0f,  //p1

        +cubeSize, -cubeSize, +cubeSize,  0f, 0f, 1f,  //p6
        -cubeSize, -cubeSize, -cubeSize,  0f, 0f, 1f,  //p1
        +cubeSize, -cubeSize, -cubeSize,  0f, 0f, 1f,  //p7
        +cubeSize, -cubeSize, +cubeSize,  0f, 0f, 1f,  //p6
        -cubeSize, -cubeSize, +cubeSize,  0f, 0f, 1f,  //p2
        -cubeSize, -cubeSize, -cubeSize,  0f, 0f, 1f,  //p1

        +cubeSize, +cubeSize, +cubeSize,  0f, 1f, 1f,  //p8
        +cubeSize, +cubeSize, -cubeSize,  0f, 1f, 1f,  //p5
        -cubeSize, +cubeSize, -cubeSize,  0f, 1f, 1f,  //p4
        +cubeSize, +cubeSize, +cubeSize,  0f, 1f, 1f,  //p8
        -cubeSize, +cubeSize, -cubeSize,  0f, 1f, 1f,  //p4
        -cubeSize, +cubeSize, +cubeSize,  0f, 1f, 1f,  //p3

        +cubeSize, +cubeSize, +cubeSize,  1f, 1f, 0f,  //p8
        -cubeSize, +cubeSize, +cubeSize,  1f, 1f, 0f,  //p3
        +cubeSize, -cubeSize, +cubeSize,  1f, 1f, 0f,  //p6
        -cubeSize, +cubeSize, +cubeSize,  1f, 1f, 0f,  //p3
        -cubeSize, -cubeSize, +cubeSize,  1f, 1f, 0f,  //p2
        +cubeSize, -cubeSize, +cubeSize,  1f, 1f, 0f,  //p6

        +cubeSize, +cubeSize, +cubeSize,  1f, 0f, 1f,  //p8
        +cubeSize, -cubeSize, -cubeSize,  1f, 0f, 1f,  //p7
        +cubeSize, +cubeSize, -cubeSize,  1f, 0f, 1f,  //p5
        +cubeSize, -cubeSize, -cubeSize,  1f, 0f, 1f,  //p7
        +cubeSize, +cubeSize, +cubeSize,  1f, 0f, 1f,  //p8
        +cubeSize, -cubeSize, +cubeSize,  1f, 0f, 1f   //p6
    )
}
