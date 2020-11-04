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


class Miniatures(
    private var list: MutableList<Picture> = ArrayList()
) {
    fun get(index: Int): Picture {
        return list[index]
    }

    fun getMiniatures(): List<Picture> {
        return ArrayList(list)
    }

    fun setMiniatures(list: List<Picture>) {
        this.list = ArrayList(list)
    }

    fun size(): Int {
        return list.size
    }

    fun clear() {
        list = ArrayList()
    }
}