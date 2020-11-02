package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.codeviewer.util.TextLines
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

fun java.io.File.toProjectFile(): File = object : File {
    override val name: String get() = this@toProjectFile.name

    override val isDirectory: Boolean get() = this@toProjectFile.isDirectory

    override val children: List<File>
        get() = this@toProjectFile
            .listFiles(FilenameFilter { _, name -> !name.startsWith(".")})
            .orEmpty()
            .map { it.toProjectFile() }

    override val hasChildren: Boolean
        get() = isDirectory && listFiles()?.size ?: 0 > 0

    private lateinit var byteBuffer: MappedByteBuffer
    private var byteBufferSize = 0

    override suspend fun readLines(backgroundScope: CoroutineScope): TextLines {
        RandomAccessFile(this@toProjectFile, "r").use { file ->
            byteBuffer = file.channel
                .map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            byteBufferSize = file.length().toInt()
        }

        val lineStartPositions = IntList()
        var size by mutableStateOf(0)

        backgroundScope.launch {
            readLinePositions(lineStartPositions)
            size = lineStartPositions.size
        }

        // Try to preload file.
        backgroundScope.launch {
            byteBuffer.load()
        }

        return object : TextLines {
            override val size get() = size

            override suspend fun get(index: Int): String {
                return withContext(Dispatchers.IO) {
                    val startPosition = lineStartPositions[index]
                    val length =
                        if (index  + 1 < size) lineStartPositions[index + 1] - startPosition else
                            byteBufferSize - startPosition
                    StandardCharsets.UTF_8.decode(byteBuffer.slice(startPosition, length)).toString()
                }
            }
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun java.io.File.readLinePositions(
    starts: IntList
) = withContext(Dispatchers.IO) {
    require(length() <= Int.MAX_VALUE) {
        "Files with size over ${Int.MAX_VALUE} aren't supported"
    }

    val averageLineLength = 200
    starts.clear(length().toInt() / averageLineLength)

    try {
        FileInputStream(this@readLinePositions).use {
            val channel = it.channel
            val ib = channel.map(
                FileChannel.MapMode.READ_ONLY, 0, channel.size()
            )
            var isBeginOfLine = true
            var position = 0L
            while (ib.hasRemaining()) {
                val byte = ib.get()
                if (isBeginOfLine) {
                    starts.add(position.toInt())
                }
                isBeginOfLine = byte.toChar() == '\n'
                position++
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        starts.clear(1)
        starts.add(0)
    }

    starts.compact()
}

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