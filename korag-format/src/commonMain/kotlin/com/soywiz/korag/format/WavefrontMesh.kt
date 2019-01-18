package com.soywiz.korag.format

import com.soywiz.kds.*

class WavefrontScene(
    val objects: Map<String, WavefrontMesh>
)

class WavefrontMesh(
    // (x, y, z, w), (u, v, w), (nx, ny, nz)
    val vertexData: FloatArray,
    val indices: IntArray
) {

}
