package org.jetbrains.compose.nativedialogs

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.nfd.NativeFileDialog

fun pickFolder(path: String): String? {
    val buf = PointerBuffer.allocateDirect(100)
    NativeFileDialog.NFD_PickFolder(path, buf)
    val mem = buf.get()
    return if (mem != MemoryUtil.NULL) MemoryUtil.memUTF8(mem) else null
}