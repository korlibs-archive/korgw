package kgl

import com.soywiz.kmem.*
import java.awt.image.*
import java.nio.*

val KmlNativeBuffer.nioBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val KmlNativeBuffer.nioByteBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val KmlNativeBuffer.nioIntBuffer: java.nio.IntBuffer get() = this.arrayInt.jbuffer.apply { (this as Buffer).rewind() }
val KmlNativeBuffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.arrayFloat.jbuffer.apply { (this as Buffer).rewind() }
