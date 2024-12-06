package benchmarks.complexlazylist.models

interface IBaseViewModel {
    val blockId: String?
    val reportInfo: Map<String, Any>?
    val operations: Map<String, Any>?
    val flipInfos: Map<String, Any>?
    val extraData: Map<String, Any>?
    val data: Map<String, Any>?
}