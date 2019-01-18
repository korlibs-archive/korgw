package com.soywiz.korag.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class OBJTest {
    @Test
    fun test() = suspendTest {
        val scene = resourcesVfs["cube.obj"].readOBJScene()
        assertEquals(listOf("Cube"), scene.objects.keys.toList())

        //assertEquals(listOf(), scene.vertexData.toList())
        //assertEquals(listOf(), scene.indices.toList())
    }
}
