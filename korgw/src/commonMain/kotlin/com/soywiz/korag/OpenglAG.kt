package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.kmem.*
import com.soywiz.korag.internal.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.math.min

abstract class AGOpengl : AG() {
    open val isGlAvailable = true
    abstract val gl: KmlGl

    open val glSlVersion: Int? = null
    open val gles: Boolean = false
    open val webgl: Boolean = false

    override var devicePixelRatio: Double = 1.0

    //val queue = Deque<(gl: GL) -> Unit>()

    override fun createBuffer(kind: Buffer.Kind): Buffer = GlBuffer(kind)

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        super.setViewport(x, y, width, height)
        //println("setViewport: $x, $y, $width, $height")
        if (isGlAvailable) {
            gl.viewport(x, y, width, height)
        }
    }

    open fun setSwapInterval(value: Int) {
        //gl.swapInterval = 0
    }

    override fun setBackBuffer(width: Int, height: Int) {
        //checkErrors { gl.Flush() }
        gl.bindTexture(gl.TEXTURE_2D, 0)
        gl.bindRenderbuffer(gl.RENDERBUFFER, 0)
        gl.bindFramebuffer(gl.FRAMEBUFFER, 0)
        setViewport(0, 0, width, height)
    }

    var lastRenderContextId = 0

    inner class GlRenderBuffer : RenderBuffer() {
        var cachedVersion = -1
        override val id = lastRenderContextId++

        val ftex get() = tex as GlTexture

        val depth = FBuffer(4)
        val framebuffer = FBuffer(4)

        override fun set() {
            //val width = this.width.nextPowerOfTwo
            //val height = this.height.nextPowerOfTwo
            gl.apply {
                if (dirty) {
                    dirty = false
                    setSwapInterval(0)

                    if (cachedVersion != contextVersion) {
                        cachedVersion = contextVersion
                        genRenderbuffers(1, depth)
                        genFramebuffers(1, framebuffer)
                    }

                    bindTexture(TEXTURE_2D, ftex.tex)
                    texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR)
                    texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR)
                    texImage2D(TEXTURE_2D, 0, RGBA, width, height, 0, RGBA, UNSIGNED_BYTE, null)
                    bindTexture(TEXTURE_2D, 0)
                    bindRenderbuffer(RENDERBUFFER, depth.getInt(0))
                    renderbufferStorage(RENDERBUFFER, DEPTH_COMPONENT16, width, height)
                }

                bindFramebuffer(FRAMEBUFFER, framebuffer.getInt(0))
                framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D, ftex.tex, 0)
                framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER, depth.getInt(0))
                setViewport(0, 0, width, height)
            }
        }

        override fun close() {
            gl.apply {
                deleteFramebuffers(1, framebuffer)
                deleteRenderbuffers(1, depth)
                framebuffer.setInt(0, 0)
                depth.setInt(0, 0)
            }
        }

        override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
    }

    override fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()

    private fun BlendEquation.toGl() = when (this) {
        BlendEquation.ADD -> gl.FUNC_ADD
        BlendEquation.SUBTRACT -> gl.FUNC_SUBTRACT
        BlendEquation.REVERSE_SUBTRACT -> gl.FUNC_REVERSE_SUBTRACT
    }

    private fun BlendFactor.toGl() = when (this) {
        BlendFactor.DESTINATION_ALPHA -> gl.DST_ALPHA
        BlendFactor.DESTINATION_COLOR -> gl.DST_COLOR
        BlendFactor.ONE -> gl.ONE
        BlendFactor.ONE_MINUS_DESTINATION_ALPHA -> gl.ONE_MINUS_DST_ALPHA
        BlendFactor.ONE_MINUS_DESTINATION_COLOR -> gl.ONE_MINUS_DST_COLOR
        BlendFactor.ONE_MINUS_SOURCE_ALPHA -> gl.ONE_MINUS_SRC_ALPHA
        BlendFactor.ONE_MINUS_SOURCE_COLOR -> gl.ONE_MINUS_SRC_COLOR
        BlendFactor.SOURCE_ALPHA -> gl.SRC_ALPHA
        BlendFactor.SOURCE_COLOR -> gl.SRC_COLOR
        BlendFactor.ZERO -> gl.ZERO
    }

    fun TriangleFace.toGl() = when (this) {
        TriangleFace.FRONT -> gl.FRONT
        TriangleFace.BACK -> gl.BACK
        TriangleFace.FRONT_AND_BACK -> gl.FRONT_AND_BACK
        TriangleFace.NONE -> gl.FRONT
    }

    fun CompareMode.toGl() = when (this) {
        CompareMode.ALWAYS -> gl.ALWAYS
        CompareMode.EQUAL -> gl.EQUAL
        CompareMode.GREATER -> gl.GREATER
        CompareMode.GREATER_EQUAL -> gl.GEQUAL
        CompareMode.LESS -> gl.LESS
        CompareMode.LESS_EQUAL -> gl.LEQUAL
        CompareMode.NEVER -> gl.NEVER
        CompareMode.NOT_EQUAL -> gl.NOTEQUAL
    }

    fun StencilOp.toGl() = when (this) {
        StencilOp.DECREMENT_SATURATE -> gl.DECR
        StencilOp.DECREMENT_WRAP -> gl.DECR_WRAP
        StencilOp.INCREMENT_SATURATE -> gl.INCR
        StencilOp.INCREMENT_WRAP -> gl.INCR_WRAP
        StencilOp.INVERT -> gl.INVERT
        StencilOp.KEEP -> gl.KEEP
        StencilOp.SET -> gl.REPLACE
        StencilOp.ZERO -> gl.ZERO
    }

    private val TEMP_MAX_MATRICES = 1024
    val tempBuffer1 = FBuffer(4)
    val tempBuffer = FBuffer(4 * 16 * TEMP_MAX_MATRICES)
    val tempBufferM2 = FBuffer(4 * 2 * 2)
    val tempBufferM3 = FBuffer(4 * 3 * 3)
    val tempBufferM4 = FBuffer(4 * 4 * 4)
    val tempF32 = tempBuffer.f32

    private val tempFloats = FloatArray(16 * TEMP_MAX_MATRICES)
    private val mat3dArray = arrayOf(Matrix3D())

    override fun draw(
        vertices: Buffer,
        program: Program,
        type: DrawType,
        vertexLayout: VertexLayout,
        vertexCount: Int,
        indices: Buffer?,
        offset: Int,
        blending: Blending,
        uniforms: UniformValues,
        stencil: StencilState,
        colorMask: ColorMaskState,
        renderState: RenderState,
        scissor: Scissor?
    ) {
        val vattrs = vertexLayout.attributes
        val vattrspos = vertexLayout.attributePositions

        if (scissor != null) {
            gl.enable(gl.SCISSOR_TEST)
            gl.scissor(scissor.x, backHeight - scissor.y - scissor.height, scissor.width, scissor.height)
        } else {
            gl.disable(gl.SCISSOR_TEST)
        }

        checkBuffers(vertices, indices)
        val glProgram = getProgram(program)
        (vertices as GlBuffer).bind(gl)
        (indices as? GlBuffer?)?.bind(gl)
        glProgram.use()


        val totalSize = vertexLayout.totalSize
        for (n in 0 until vattrspos.size) {
            val att = vattrs[n]
            if (att.active) {
                val off = vattrspos[n]
                val loc = glProgram.getAttribLocation(att.name)
                val glElementType = att.type.glElementType
                val elementCount = att.type.elementCount
                if (loc >= 0) {
                    gl.enableVertexAttribArray(loc)
                    gl.vertexAttribPointer(loc, elementCount, glElementType, att.normalized, totalSize, off)
                }
            }
        }
        var textureUnit = 0
        //for ((uniform, value) in uniforms) {
        for (n in 0 until uniforms.uniforms.size) {
            val uniform = uniforms.uniforms[n]
            val uniformName = uniform.name
            val uniformType = uniform.type
            val value = uniforms.values[n]
            val location = gl.getUniformLocation(glProgram.id, uniformName)
            val declArrayCount = uniform.arrayCount
            val stride = uniform.type.elementCount

            //println("uniform: $uniform, arrayCount=$arrayCount, stride=$stride")

            when (uniformType) {
                VarType.TextureUnit -> {
                    val unit = value as TextureUnit
                    gl.activeTexture(gl.TEXTURE0 + textureUnit)
                    val tex = (unit.texture as GlTexture?)
                    tex?.bindEnsuring()
                    tex?.setFilter(unit.linear)
                    gl.uniform1i(location, textureUnit)
                    textureUnit++
                }
                VarType.Mat2, VarType.Mat3, VarType.Mat4 -> {
                    val matArray = when (value) {
                        is Array<*> -> value
                        is Matrix3D -> mat3dArray.also { it[0].copyFrom(value) }
                        else -> error("Not an array or a matrix3d")
                    } as Array<Matrix3D>
                    val arrayCount = min(declArrayCount, matArray.size)

                    val matSize = when (uniformType) {
                        VarType.Mat2 -> 2; VarType.Mat3 -> 3; VarType.Mat4 -> 4; else -> -1
                    }

                    for (n in 0 until arrayCount) {
                        matArray[n].copyToFloatWxH(tempFloats, matSize, matSize, MajorOrder.COLUMN, n * stride)
                    }
                    tempBuffer.setFloats(0, tempFloats, 0, stride * arrayCount)

                    if (webgl) {
                    //if (true) {
                        val tb = when (uniformType) {
                            VarType.Mat2 -> tempBufferM2
                            VarType.Mat3 -> tempBufferM3
                            VarType.Mat4 -> tempBufferM4
                            else -> tempBufferM4
                        }

                        for (n in 0 until arrayCount) {
                            val itLocation = if (arrayCount == 1) location else gl.getUniformLocation(glProgram.id, uniform.indexNames[n])
                            arraycopy(tempBuffer.f32, n * stride, tb.f32, 0, stride)
                            //println("[WEBGL] uniformName[$uniformName]=$itLocation, tb: ${tb.f32.size}")
                            when (uniform.type) {
                                VarType.Mat2 -> gl.uniformMatrix2fv(itLocation, 1, false, tb)
                                VarType.Mat3 -> gl.uniformMatrix3fv(itLocation, 1, false, tb)
                                VarType.Mat4 -> gl.uniformMatrix4fv(itLocation, 1, false, tb)
                                else -> invalidOp("Don't know how to set uniform matrix ${uniform.type}")
                            }
                        }
                    } else {
                        //println("[NO-WEBGL] uniformName[$uniformName]=$location ")
                        when (uniform.type) {
                            VarType.Mat2 -> gl.uniformMatrix2fv(location, arrayCount, false, tempBuffer)
                            VarType.Mat3 -> gl.uniformMatrix3fv(location, arrayCount, false, tempBuffer)
                            VarType.Mat4 -> gl.uniformMatrix4fv(location, arrayCount, false, tempBuffer)
                            else -> invalidOp("Don't know how to set uniform matrix ${uniform.type}")
                        }
                    }
                }
                VarType.Float1, VarType.Float2, VarType.Float3, VarType.Float4 -> {
                    var arrayCount = declArrayCount
                    when (value) {
                        is Number -> tempBuffer.setAlignedFloat32(0, value.toFloat())
                        is Vector3D -> tempBuffer.setFloats(0, value.data, 0, stride)
                        is FloatArray -> {
                            arrayCount = min(declArrayCount, value.size / stride)
                            tempBuffer.setFloats(0, value, 0, stride * arrayCount)
                        }
                        is Array<*> -> {
                            arrayCount = min(declArrayCount, value.size)
                            for (n in 0 until value.size) {
                                val vector = value[n] as Vector3D
                                tempBuffer.setFloats(n * stride, vector.data, 0, stride)
                            }
                        }
                        else -> error("Unknown type '$value'")
                    }
                    //if (true) {
                    if (webgl) {
                        val tb = tempBufferM2
                        for (n in 0 until arrayCount) {
                            val itLocation = if (arrayCount == 1) location else gl.getUniformLocation(glProgram.id, uniform.indexNames[n])
                            val f32 = tb.f32
                            //println("uniformName[$uniformName] = $itLocation")
                            arraycopy(tempBuffer.f32, 0, tb.f32, 0, stride)

                            when (uniform.type) {
                                VarType.Float1 -> gl.uniform1f(itLocation, f32[0])
                                VarType.Float2 -> gl.uniform2f(itLocation, f32[0], f32[1])
                                VarType.Float3 -> gl.uniform3f(itLocation, f32[0], f32[1], f32[2])
                                VarType.Float4 -> gl.uniform4f(itLocation, f32[0], f32[1], f32[2], f32[3])
                                else -> Unit
                            }
                        }
                    } else {
                        when (uniform.type) {
                            VarType.Float1 -> gl.uniform1fv(location, arrayCount, tempBuffer)
                            VarType.Float2 -> gl.uniform2fv(location, arrayCount, tempBuffer)
                            VarType.Float3 -> gl.uniform3fv(location, arrayCount, tempBuffer)
                            VarType.Float4 -> gl.uniform4fv(location, arrayCount, tempBuffer)
                            else -> Unit
                        }
                    }
                }
                else -> invalidOp("Don't know how to set uniform ${uniform.type}")
            }
        }

        if (blending.enabled) {
            gl.enable(gl.BLEND)
            gl.blendEquationSeparate(blending.eqRGB.toGl(), blending.eqA.toGl())
            gl.blendFuncSeparate(
                blending.srcRGB.toGl(), blending.dstRGB.toGl(),
                blending.srcA.toGl(), blending.dstA.toGl()
            )
        } else {
            gl.disable(gl.BLEND)
        }

        if (renderState.frontFace == FrontFace.BOTH) {
            gl.disable(gl.CULL_FACE)
        } else {
            gl.enable(gl.CULL_FACE)
            gl.frontFace(if (renderState.frontFace == FrontFace.CW) gl.CW else gl.CCW)
        }

        gl.depthMask(renderState.depthMask)
        gl.depthRangef(renderState.depthNear, renderState.depthFar)
        gl.lineWidth(renderState.lineWidth)

        if (renderState.depthFunc != CompareMode.ALWAYS) {
            gl.enable(gl.DEPTH_TEST)
            gl.depthFunc(renderState.depthFunc.toGl())
        } else {
            gl.disable(gl.DEPTH_TEST)
        }

        gl.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)

        if (stencil.enabled) {
            gl.enable(gl.STENCIL_TEST)
            gl.stencilFunc(stencil.compareMode.toGl(), stencil.referenceValue, stencil.readMask)
            gl.stencilOp(
                stencil.actionOnDepthFail.toGl(),
                stencil.actionOnDepthPassStencilFail.toGl(),
                stencil.actionOnBothPass.toGl()
            )
            gl.stencilMask(stencil.writeMask)
        } else {
            gl.disable(gl.STENCIL_TEST)
            gl.stencilMask(0)
        }

        if (indices != null) {
            gl.drawElements(type.glDrawMode, vertexCount, gl.UNSIGNED_SHORT, offset)
        } else {
            gl.drawArrays(type.glDrawMode, offset, vertexCount)
        }

        //glSetActiveTexture(gl.TEXTURE0)

        for (n in 0 until vattrs.size) {
            val att = vattrs[n]
            if (att.active) {
                val loc = glProgram.getAttribLocation(att.name).toInt()
                if (loc >= 0) {
                    gl.disableVertexAttribArray(loc)
                }
            }
        }
    }

    val DrawType.glDrawMode: Int
        get() = when (this) {
            DrawType.POINTS -> gl.POINTS
            DrawType.LINE_STRIP -> gl.LINE_STRIP
            DrawType.LINE_LOOP -> gl.LINE_LOOP
            DrawType.LINES -> gl.LINES
            DrawType.TRIANGLE_STRIP -> gl.TRIANGLE_STRIP
            DrawType.TRIANGLE_FAN -> gl.TRIANGLE_FAN
            DrawType.TRIANGLES -> gl.TRIANGLES
        }

    val VarType.glElementType: Int
        get() = when (this.kind) {
            VarKind.TBYTE -> gl.BYTE
            VarKind.TUNSIGNED_BYTE -> gl.UNSIGNED_BYTE
            VarKind.TSHORT -> gl.SHORT
            VarKind.TUNSIGNED_SHORT -> gl.UNSIGNED_SHORT
            VarKind.TINT -> gl.UNSIGNED_INT
            VarKind.TFLOAT -> gl.FLOAT
        }

    private val programs = HashMap<Program, GlProgram>()
    fun getProgram(program: Program): GlProgram = programs.getOrPut(program) { GlProgram(gl, program) }

    inner class GlProgram(val gl: KmlGl, val program: Program) : Closeable {
        var cachedVersion = -1
        var id: Int = 0
        var fragmentShaderId: Int = 0
        var vertexShaderId: Int = 0

        val cachedAttribLocations = FastStringMap<Int>()

        fun getAttribLocation(name: String): Int {
            return cachedAttribLocations.getOrPut(name) {
                gl.getAttribLocation(id, name)
            }
        }

        private fun String.replaceVersion(version: Int) = this.replace("#version 100", "#version $version")

        private fun ensure() {
            if (cachedVersion != contextVersion) {
                val oldCachedVersion = cachedVersion
                cachedVersion = contextVersion
                id = gl.createProgram()

                println("OpenglAG: Created program ${program.name} with id $id because contextVersion: $oldCachedVersion != $contextVersion")

                //println("GL_SHADING_LANGUAGE_VERSION: $glslVersionInt : $glslVersionString")

                val guessedGlSlVersion = glSlVersion ?: gl.versionInt
                val usedGlSlVersion = GlslGenerator.FORCE_GLSL_VERSION?.toIntOrNull() ?: when (guessedGlSlVersion) {
                    460 -> 460
                    in 300..450 -> 100
                    else -> guessedGlSlVersion
                }

                if (GlslGenerator.DEBUG_GLSL) {
                    println("GLSL version: requested=$glSlVersion, guessed=$guessedGlSlVersion, forced=${GlslGenerator.FORCE_GLSL_VERSION}. used=$usedGlSlVersion")
                }

                val fragResult = program.fragment.toNewGlslStringResult(gles = gles, version = usedGlSlVersion)
                val vertResult = program.vertex.toNewGlslStringResult(gles = gles, version = usedGlSlVersion)

                fragmentShaderId = createShader(gl.FRAGMENT_SHADER, fragResult.result)
                vertexShaderId = createShader(gl.VERTEX_SHADER, vertResult.result)
                gl.attachShader(id, fragmentShaderId)
                gl.attachShader(id, vertexShaderId)
                if (fragResult.generator.newGlSlVersion) {
                    //println("****************************")
                    //gl.bindAttribLocation(id, 0, fragResult.generator.gl_FragColor)
                }
                gl.linkProgram(id)
                tempBuffer1.setInt(0, 0)
                gl.getProgramiv(id, gl.LINK_STATUS, tempBuffer1)
            }
        }

        private fun createShader(type: Int, str: String): Int {
            val shaderId = gl.createShader(type)

            gl.shaderSource(shaderId, str)
            gl.compileShader(shaderId)

            val out = gl.getShaderiv(shaderId, gl.COMPILE_STATUS)
            val errorInt = gl.getError()
            if (out != gl.TRUE) {
                val error = gl.getShaderInfoLog(shaderId)
                throw RuntimeException("Error Compiling Shader : $errorInt : '$error' : source='$str', gl.versionInt=${gl.versionInt}, gl.versionString='${gl.versionString}', gl=$gl")
            }
            return shaderId
        }

        fun use() {
            ensure()
            gl.useProgram(id)
        }

        fun unuse() {
            ensure()
            gl.useProgram(0)
        }

        override fun close() {
            gl.deleteShader(fragmentShaderId)
            gl.deleteShader(vertexShaderId)
            gl.deleteProgram(id)
        }
    }

    // @TODO: Kotlin inline bug
    //Back-end (JVM) Internal error: wrong code generated
    //org.jetbrains.kotlin.codegen.CompilationException Back-end (JVM) Internal error: Couldn't transform method node:
    //clear (IFIZZZ)V:
    override fun clear(
        color: RGBA,
        depth: Float,
        stencil: Int,
        clearColor: Boolean,
        clearDepth: Boolean,
        clearStencil: Boolean
    ) {
        //println("CLEAR: $color, $depth")
        var bits = 0
        gl.disable(gl.SCISSOR_TEST)
        if (clearColor) {
            bits = bits or gl.COLOR_BUFFER_BIT
            gl.clearColor(color.rf, color.gf, color.bf, color.af)
        }
        if (clearDepth) {
            bits = bits or gl.DEPTH_BUFFER_BIT
            gl.clearDepthf(depth)
        }
        if (clearStencil) {
            bits = bits or gl.STENCIL_BUFFER_BIT
            gl.stencilMask(-1)
            gl.clearStencil(stencil)
        }
        gl.clear(bits)
    }

    override fun createTexture(premultiplied: Boolean): Texture = GlTexture(this.gl, premultiplied)

    inner class GlBuffer(kind: Buffer.Kind) : Buffer(kind) {
        var cachedVersion = -1
        private var id = -1
        val glKind = if (kind == Buffer.Kind.INDEX) gl.ELEMENT_ARRAY_BUFFER else gl.ARRAY_BUFFER

        override fun afterSetMem() {
        }

        override fun close() {
            fbuffer(4) { buffer ->
                buffer.setInt(0, this.id)
                gl.deleteBuffers(1, buffer)
            }
            id = -1
        }

        fun getGlId(gl: KmlGl): Int {
            if (cachedVersion != contextVersion) {
                cachedVersion = contextVersion
                dirty = true
                id = -1
            }
            if (id < 0) {
                id = fbuffer(4) {
                    gl.genBuffers(1, it)
                    it.getInt(0)
                }
            }
            if (dirty) {
                _bind(gl, id)
                if (mem != null) {
                    gl.bufferData(glKind, memLength, mem!!, gl.STATIC_DRAW)
                }
            }
            return id
        }

        fun _bind(gl: KmlGl, id: Int) {
            gl.bindBuffer(glKind, id)
        }

        fun bind(gl: KmlGl) {
            _bind(gl, getGlId(gl))
        }
    }

    open fun prepareUploadNativeTexture(bmp: NativeImage) {
    }

    inner class GlTexture(val gl: KmlGl, override val premultiplied: Boolean) : Texture() {
        var cachedVersion = -1
        val texIds = FBuffer(4)

        val tex: Int
            get() {
                if (cachedVersion != contextVersion) {
                    cachedVersion = contextVersion
                    invalidate()
                    gl.genTextures(1, texIds)
                }
                return texIds.getInt(0)
            }

        fun createBufferForBitmap(bmp: Bitmap?): FBuffer? {
            return when (bmp) {
                null -> null
                is NativeImage -> unsupported("Should not call createBufferForBitmap with a NativeImage")
                is Bitmap8 -> {
                    val mem = FBuffer(bmp.area)
                    arraycopy(bmp.data, 0, mem.arrayByte, 0, bmp.area)
                    @Suppress("USELESS_CAST")
                    return mem
                }
                is Bitmap32 -> {
                    val abmp: Bitmap32 =
                        if (premultiplied) bmp.premultipliedIfRequired() else bmp.depremultipliedIfRequired()
                    //println("BMP: Bitmap32")
                    //val abmp: Bitmap32 = bmp
                    val mem = FBuffer(abmp.area * 4)
                    arraycopy(abmp.data.ints, 0, mem.arrayInt, 0, abmp.area)
                    @Suppress("USELESS_CAST")
                    return mem
                }
                else -> unsupported()
            }
        }

        override fun actualSyncUpload(source: BitmapSourceBase, bmp: Bitmap?, requestMipmaps: Boolean) {
            this.mipmaps = false

            val bytesPerPixel = if (source.rgba) 4 else 1
            val type = if (source.rgba) {
                //if (source is NativeImage) gl.BGRA else gl.RGBA
                gl.RGBA
            } else {
                gl.LUMINANCE
            }

            if (bmp is NativeImage) {
                prepareUploadNativeTexture(bmp)
                if (bmp.area != 0) {
                    gl.texImage2D(gl.TEXTURE_2D, 0, type, type, gl.UNSIGNED_BYTE, bmp)
                }
            } else {
                val buffer = createBufferForBitmap(bmp)
                if (buffer != null && source.width != 0 && source.height != 0 && buffer.size != 0) {
                    gl.texImage2D(
                        gl.TEXTURE_2D, 0, type,
                        source.width, source.height,
                        0, type, gl.UNSIGNED_BYTE, buffer
                    )
                }
                //println(buffer)
            }

            if (requestMipmaps && source.width.isPowerOfTwo && source.height.isPowerOfTwo) {
                //println(" - mipmaps")
                this.mipmaps = true
                bind()
                setFilter(true)
                setWrapST()
                //println("actualSyncUpload,generateMipmap.SOURCE: ${source.width},${source.height}, source=$source, bmp=$bmp, requestMipmaps=$requestMipmaps")
                //printStackTrace()
                gl.generateMipmap(gl.TEXTURE_2D)
            } else {
                //println(" - nomipmaps")
            }
        }

        override fun bind(): Unit = gl.bindTexture(gl.TEXTURE_2D, tex)
        override fun unbind(): Unit = gl.bindTexture(gl.TEXTURE_2D, 0)

        private var closed = false
        override fun close(): Unit {
            super.close()
            if (!closed) {
                closed = true
                gl.deleteTextures(1, texIds)
            }
        }

        fun setFilter(linear: Boolean) {
            val minFilter = if (this.mipmaps) {
                if (linear) gl.LINEAR_MIPMAP_NEAREST else gl.NEAREST_MIPMAP_NEAREST
            } else {
                if (linear) gl.LINEAR else gl.NEAREST
            }
            val magFilter = if (linear) gl.LINEAR else gl.NEAREST

            setWrapST()
            setMinMag(minFilter, magFilter)
        }

        private fun setWrapST() {
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        }

        private fun setMinMag(min: Int, mag: Int) {
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, min)
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, mag)
        }
    }

    override fun readColor(bitmap: Bitmap32) {
        fbuffer(bitmap.area * 4) { buffer ->
            gl.readPixels(
                0, 0, bitmap.width, bitmap.height,
                gl.RGBA, gl.UNSIGNED_BYTE, buffer
            )
            buffer.getAlignedArrayInt32(0, bitmap.data.ints, 0, bitmap.area)
            //println("readColor.HASH:" + bitmap.computeHash())
        }
    }

    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        val area = width * height
        fbuffer(area * 4) { buffer ->
            gl.readPixels(
                0, 0, width, height, gl.DEPTH_COMPONENT, gl.FLOAT,
                buffer
            )
            buffer.getAlignedArrayFloat32(0, out, 0, area)
        }
    }

    override fun readColorTexture(texture: Texture, width: Int, height: Int) {
        gl.apply {
            texture.bind()
            copyTexImage2D(TEXTURE_2D, 0, RGBA, 0, 0, width, height, 0)
            texture.unbind()
        }
    }
}


val KmlGl.versionString by Extra.PropertyThis<KmlGl, String> {
    getString(SHADING_LANGUAGE_VERSION)
}

val KmlGl.versionInt by Extra.PropertyThis<KmlGl, Int> {
    versionString.replace(".", "").trim().toIntOrNull() ?: 100
}
