// READ ME FIRST!
//
// Code in this file is shared between the Android and Desktop JVM targets.
// Kotlin's hierarchical multiplatform projects currently
// don't support sharing code depending on JVM declarations.
//
// You can follow the progress for HMPP JVM & Android intermediate source sets here:
// https://youtrack.jetbrains.com/issue/KT-42466
//
// The workaround used here to access JVM libraries causes IntelliJ IDEA to not
// resolve symbols in this file properly.
//
// Resolution errors in your IDE do not indicate a problem with your setup.

package example.imageviewer.model

import example.imageviewer.core.CommonBitmap

data class Picture(
    val source: String = "",
    val name: String = "",
    val image: CommonBitmap,
    val width: Int = 0,
    val height: Int = 0,
    val id: Int = 0
)

class Miniatures(
    private var list: List<Picture> = emptyList()
) {
    fun get(index: Int): Picture {
        return list[index]
    }

    fun getMiniatures(): List<Picture> {
        return list.toList()
    }

    fun setMiniatures(list: List<Picture>) {
        this.list = list.toList()
    }

    fun size(): Int {
        return list.size
    }

    fun clear() {
        list = emptyList()
    }
}