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


    override fun readLines(scope: CoroutineScope): TextLines {
        var byteBufferSize: Int
        val byteBuffer = RandomAccessFile(this@toProjectFile, "r").use { file ->
            byteBufferSize = file.length().toInt()
            file.channel
                .map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }

        val lineStartPositions = IntList()

        var size by mutableStateOf(0)

        val refreshJob = scope.launch {
            delay(100)
            size = lineStartPositions.size
            while (true) {
                delay(1000)
                size = lineStartPositions.size
            }
        }

        scope.launch(Dispatchers.IO) {
            readLinePositions(lineStartPositions)
            refreshJob.cancel()
            size = lineStartPositions.size
        }

        return object : TextLines {
            override val size get() = size

            override fun get(index: Int): String {
                val startPosition = lineStartPositions[index]
                val length = if (index + 1 < size) lineStartPositions[index + 1] - startPosition else
                    byteBufferSize - startPosition
                // Only JDK since 13 has slice() method we need, so do ugly for now.
                byteBuffer.position(startPosition)
                val slice = byteBuffer.slice()
                slice.limit(length)
                return StandardCharsets.UTF_8.decode(slice).toString()
            }
        }
    }
}

private fun java.io.File.readLinePositions(
    starts: IntList
) {
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
            channel.close()
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