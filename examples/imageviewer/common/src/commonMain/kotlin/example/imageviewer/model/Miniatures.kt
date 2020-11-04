package example.imageviewer.model

expect class Picture

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