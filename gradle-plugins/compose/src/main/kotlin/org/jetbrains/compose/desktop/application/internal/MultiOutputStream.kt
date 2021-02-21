package org.jetbrains.compose.desktop.application.internal

import java.io.FilterOutputStream
import java.io.OutputStream

internal class MultiOutputStream(
    mainStream: OutputStream,
    private val secondaryStream: OutputStream
) : FilterOutputStream(mainStream) {
    override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
        secondaryStream.write(b, off, len)
    }

    override fun write(b: ByteArray) {
        super.write(b)
        secondaryStream.write(b)
    }

    override fun write(b: Int) {
        super.write(b)
        secondaryStream.write(b)
    }

    override fun flush() {
        super.flush()
        secondaryStream.flush()
    }

    override fun close() {
        try {
            super.close()
        } finally {
            secondaryStream.close()
        }
    }
}

internal fun OutputStream.alsoOutputTo(secondaryStream: OutputStream): OutputStream =
    MultiOutputStream(this, secondaryStream)