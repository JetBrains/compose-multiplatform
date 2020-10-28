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