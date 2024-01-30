data class UnstableDataClassWithPrivateVar(private var i: Int) {

    fun inc() { i++ }
    fun getI() = i
}


data class StableDataClassWithPrivateVal(private val i: Int)