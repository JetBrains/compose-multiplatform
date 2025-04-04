package benchmarks.complexlazylist.models


val fakeItem2 = object : ICompositionItem {
    override val bgColor: String?
        get() = ""
    override val radius: String?
        get() = "0.5"
    override val alpha: String?
        get() = "0.5"
    override val shadowColor: String?
        get() = ""
    override val textColor: String?
        get() = ""
    override val text: String?
        get() = "yqhwjmjdsk" +
                "fksghksjhgksjh" +
                "gsgjlksgjlk"
}

open class FakeItem : ICompositionModel {
    override val title: String?
        get() = "aghjagj"
    override val subtitle: String?
        get() = "ghjghjghj"
    override val overlyTopLeft: ICompositionItem?
        get() = fakeItem2
    override val overlyView1: ICompositionItem?
        get() = fakeItem2
    override val overlyView2: ICompositionItem?
        get() = fakeItem2
    override val overlyView3: ICompositionItem?
        get() = fakeItem2
    override val overlyTopRight: ICompositionItem?
        get() = fakeItem2
    override val label: ICompositionItem?
        get() = fakeItem2
    override val blockId: String?
        get() = ""
    override val reportInfo: Map<String, Any>?
        get() = TODO("Not yet implemented")
    override val operations: Map<String, Any>?
        get() = TODO("Not yet implemented")
    override val flipInfos: Map<String, Any>?
        get() = TODO("Not yet implemented")
    override val extraData: Map<String, Any>?
        get() = TODO("Not yet implemented")
    override val data: Map<String, Any>?
        get() = TODO("Not yet implemented")
}

fun createFakeItem(): ICompositionModel = FakeItem()

internal fun fetchCompositionModels(useJSON: Boolean, callback: (List<IBaseViewModel>) -> Unit) {
    callback(List(250, { createFakeItem() }))
}
