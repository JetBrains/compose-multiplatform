import kotlinx.cinterop.objcPtr
import org.jetbrains.skia.*
import platform.Metal.*

/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun main() {
    val graphicsContext = object : GraphicsContext {
        private val device = MTLCreateSystemDefaultDevice() ?: throw IllegalStateException("Can't create MTLDevice")
        private val commandQueue = device.newCommandQueue() ?: throw IllegalStateException("Can't create MTLCommandQueue")
        private val directContext = DirectContext.makeMetal(device.objcPtr(), commandQueue.objcPtr())
        private var cachedSurface: Surface? = null

        override fun surface(width: Int, height: Int): Surface {
            val oldSurface = cachedSurface

            if (oldSurface != null && oldSurface.width == width && oldSurface.height == height) {
                return oldSurface
            }

            val descriptor = MTLTextureDescriptor()
            descriptor.width = width.toULong()
            descriptor.height = height.toULong()
            descriptor.usage = MTLTextureUsageShaderRead or MTLTextureUsageShaderWrite or MTLTextureUsageRenderTarget
            descriptor.textureType = MTLTextureType2D
            descriptor.pixelFormat = MTLPixelFormatBGRA8Unorm_sRGB
            descriptor.mipmapLevelCount = 1UL

            val texture = device.newTextureWithDescriptor(descriptor) ?: throw IllegalStateException("Can't create MTLTexture")

            val renderTarget = BackendRenderTarget.makeMetal(width, height, texture.objcPtr())

            return Surface.makeFromBackendRenderTarget(
                directContext,
                renderTarget,
                SurfaceOrigin.TOP_LEFT,
                SurfaceColorFormat.BGRA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
            ).also {
                cachedSurface = it
            } ?: throw IllegalStateException("Can't create Surface")
        }

        override fun waitUntilGPUFinishes() {
            val commandBuffer = commandQueue.commandBuffer() ?: return
            commandBuffer.commit()
            commandBuffer.waitUntilCompleted()
        }
    }

    runBenchmarks(graphicsContext = graphicsContext)
}
