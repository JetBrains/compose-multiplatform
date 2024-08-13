package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import org.jetbrains.codeviewer.util.TextLines
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

fun java.io.File.toProjectFile(): File = object : File {
    override val name: String
        get() = this@toProjectFile.name

    override val isDirectory: Boolean
        get() = this@toProjectFile.isDirectory

    override val children: List<File>
        get() = this@toProjectFile
            .listFiles(FilenameFilter { _, name -> !name.startsWith(".")})
            .orEmpty()
            .map { it.toProjectFile() }

    private val numberOfFiles
        get() = listFiles()?.size ?: 0

    override val hasChildren: Boolean
        get() = isDirectory && numberOfFiles > 0


    override fun readLines(scope: CoroutineScope): TextLines {
        var byteBufferSize: Int
        val byteBuffer = RandomAccessFile(this@toProjectFile, "r").use { file ->
            byteBufferSize = file.length().toInt()
            file.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }

        val lineStartPositions = IntList()
        var size by mutableStateOf(0)

        // In case of big files, update number of lines periodically
        val refreshJob = scope.launch {
            delay(100)
            size = lineStartPositions.size
            while (isActive) {
                delay(1000)
                size = lineStartPositions.size
            }
        }

        // Find indexes where lines starts in background
        scope.launch(Dispatchers.IO) {
            readLinePositions(lineStartPositions)
            refreshJob.cancel()
            size = lineStartPositions.size
        }

        return object : TextLines {
            override val size get() = size

            override fun get(index: Int): String {
                val position = lineRange(index)
                byteBuffer.position(position.first)
                val slice = byteBuffer.slice()
                slice.limit(position.last - position.first)
                return StandardCharsets.UTF_8.decode(slice).toString()
            }

            private fun lineRange(index: Int): IntRange {
                val startPosition = lineStartPositions[index]
                val nextLineIndex = index + 1
                var endPosition = if (nextLineIndex < size) lineStartPositions[nextLineIndex] else byteBufferSize

                // Remove line endings from the range
                while (endPosition > startPosition) {
                    val lastSymbol = byteBuffer[endPosition - 1]
                    when (lastSymbol.toInt().toChar()) {
                        '\n', '\r' -> endPosition--
                        else -> break
                    }
                }
                return startPosition..endPosition
            }
        }
    }
}

// Backport slice from JDK 13
private fun ByteBuffer.slice(index: Int, length: Int): ByteBuffer {
    position(index)
    return slice().limit(length) as ByteBuffer
}

private fun java.io.File.readLinePositions(starts: IntList) {
    require(length() <= Int.MAX_VALUE) {
        "Files with size over ${Int.MAX_VALUE} aren't supported"
    }

    val averageLineLength = 200
    starts.clear(length().toInt() / averageLineLength)

    try {
        for (i in readLinePositions()) {
            starts.add(i)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        starts.clear(1)
        starts.add(0)
    }

    starts.compact()
}

private fun java.io.File.readLinePositions() = sequence {
    require(length() <= Int.MAX_VALUE) {
        "Files with size over ${Int.MAX_VALUE} aren't supported"
    }
    readBuffer {
        yield(position())
        while (hasRemaining()) {
            val byte = get()
            if (byte.isChar('\n')) {
                yield(position())
            }
        }
    }
}

private inline fun java.io.File.readBuffer(block: ByteBuffer.() -> Unit) {
    FileInputStream(this).use { stream ->
        stream.channel.use { channel ->
            channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).block()
        }
    }
}

private fun Byte.isChar(char: Char) = toInt().toChar() == char

/**
 * Compact version of List<Int> (without unboxing Int and using IntArray under the hood)
 */
private class IntList(initialCapacity: Int = 16) {
    @Volatile
    private var array = IntArray(initialCapacity)

    @Volatile
    var size: Int = 0
        private set

    fun clear(capacity: Int) {
        array = IntArray(capacity)
        size = 0
    }

    fun add(value: Int) {
        if (size == array.size) {
            doubleCapacity()
        }
        array[size++] = value
    }

    operator fun get(index: Int) = array[index]

    private fun doubleCapacity() {
        val newArray = IntArray(array.size * 2 + 1)
        System.arraycopy(array, 0, newArray, 0, size)
        array = newArray
    }

    fun compact() {
        array = array.copyOfRange(0, size)
    }
}
