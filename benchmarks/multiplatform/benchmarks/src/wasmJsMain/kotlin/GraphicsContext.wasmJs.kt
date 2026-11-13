import org.jetbrains.skia.*

class SdlGraphicsContext : GraphicsContext {
    private var contextPointer = 0
    private var contextWidth = 0
    private var contextHeight = 0
    private var directContext: DirectContext? = null

    private fun makeCurrent() {
        if (!makeGlContextCurrent(contextPointer)) {
            error("Cannot make WebGL / Native GL context current for SdlGraphicsContext")
        }
    }

    private fun ensureContext(width: Int, height: Int): DirectContext {
        require(width > 0 && height > 0) { "GPU surface dimensions must be positive, got ${width}x$height" }

        if (contextPointer != 0 && (contextWidth != width || contextHeight != height)) {
            makeCurrent()
            directContext?.close()
            directContext = null
            if (!deleteGlContext(contextPointer)) {
                error("Cannot delete previous Native GL context for SdlGraphicsContext")
            }
            contextPointer = 0
        }

        if (contextPointer == 0) {
            contextPointer = createGlContext(width, height)
            if (contextPointer == 0) {
                error("Cannot create ${width}x$height WebGL / Native GL context for SdlGraphicsContext")
            }
            contextWidth = width
            contextHeight = height
        }

        makeCurrent()
        return directContext ?: DirectContext.makeGL().also { directContext = it }
    }

    override fun surface(width: Int, height: Int): Surface {
        val context = ensureContext(width, height)
        check(contextWidth == width && contextHeight == height) {
            "Native GL context is ${contextWidth}x$contextHeight but requested surface is ${width}x$height"
        }
        val renderTarget = BackendRenderTarget.makeGL(
            width = width,
            height = height,
            sampleCnt = 0,
            stencilBits = 8,
            fbId = 0,
            fbFormat = FramebufferFormat.GR_GL_RGBA8
        )
        return Surface.makeFromBackendRenderTarget(
            context,
            renderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
        ) ?: error("Failed to create GPU Surface for SdlGraphicsContext")
    }

    override suspend fun awaitGPUCompletion() {
        makeCurrent()
        val context = directContext ?: error("GPU context has not been initialized")
        context.flush()
        context.submit(true)
        nativeGlFinish()
    }

    override fun present() {
        if (contextPointer != 0) {
            nativeGlPresent(contextPointer)
            makeCurrent()
        }
    }

    override fun cleanup() {
        if (contextPointer != 0) {
            makeCurrent()
            try {
                directContext?.resetAll()
            } catch (t: Throwable) {
                // Ignore if not supported
            }
        }
    }
}

private fun nativeGlPresent(contextId: Int): Unit =
    js("(function() { var nativeGL = globalThis.__skikoNativeGL; if (nativeGL && typeof nativeGL.present === 'function') { try { nativeGL.present(contextId); } catch (e) {} } if (nativeGL && typeof nativeGL.pollEvents === 'function') { try { nativeGL.pollEvents(); } catch (e) {} } })()")

private fun createGlContextJs(width: Int, height: Int): JsNumber =
    js("(function() { var canvas = document.createElement('canvas'); canvas.width = width; canvas.height = height; return (globalThis.__skikoGL && typeof globalThis.__skikoGL.createContext === 'function') ? globalThis.__skikoGL.createContext(canvas, { alpha: 1, depth: 1, stencil: 8, antialias: 0, majorVersion: 2 }) : 0; })()")

private fun createGlContext(width: Int, height: Int): Int =
    createGlContextJs(width, height).toInt()

private fun makeGlContextCurrentJs(contextId: Int): JsBoolean =
    js("(globalThis.__skikoGL && typeof globalThis.__skikoGL.makeContextCurrent === 'function') ? Boolean(globalThis.__skikoGL.makeContextCurrent(contextId)) : false")

private fun makeGlContextCurrent(contextId: Int): Boolean =
    makeGlContextCurrentJs(contextId).toBoolean()

private fun deleteGlContextJs(contextId: Int): JsBoolean =
    js("(function() { if (!globalThis.__skikoGL || typeof globalThis.__skikoGL.deleteContext !== 'function') { return false; } globalThis.__skikoGL.deleteContext(contextId); return true; })()")

private fun deleteGlContext(contextId: Int): Boolean =
    deleteGlContextJs(contextId).toBoolean()

private fun nativeGlFinish(): Unit =
    js("(globalThis.__skikoGlFinish || (globalThis.__skikoGlFinish = (function() { var nativeGL = globalThis.__skikoNativeGL; if (!nativeGL) { throw new Error('Native GL bindings are unavailable during GPU synchronization'); } var fn = (typeof nativeGL.getGLFunction === 'function') ? nativeGL.getGLFunction('glFinish') : nativeGL.glFinish; if (typeof fn !== 'function') { throw new Error('Native GL glFinish is unavailable during GPU synchronization'); } return function() { fn.call(nativeGL); }; })()))()")

fun wasmGraphicsContext(): GraphicsContext? =
    if (try { isSdlEnv().toBoolean() } catch (t: Throwable) { false }) SdlGraphicsContext() else null
