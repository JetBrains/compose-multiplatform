import kotlin.jvm.JvmInline

@JvmInline
value class VCAllPublic(val value: Int)

@JvmInline
value class VCNestedVCAllPublic(val value: VCAllPublic)

@JvmInline
value class VCInternalVal(internal val value: Int)

@JvmInline
value class VCPrivateVal(private val value: Int)

@JvmInline
value class VCNestedVCPrivateVal(val value: VCPrivateVal)

@JvmInline
value class VCInternalCtor internal constructor(val value: Int) {
    companion object {
        val V1 = VCInternalCtor(1)
        val V2 = VCInternalCtor(2)
    }
}

@JvmInline
value class VCInternalAll internal constructor(internal val value: Int) {
    companion object {
        val V1 = VCInternalAll(111)
        val V2 = VCInternalAll(222)
    }
}

@JvmInline
value class VCPrivateCtor private constructor(val value: Int) {
    companion object {
        val V1 = VCPrivateCtor(1111)
        val V2 = VCPrivateCtor(2222)
    }
}

@JvmInline
value class VCPrivateCtorInternalVal private constructor(internal val value: Int) {
    companion object {
        val V1 = VCPrivateCtorInternalVal(101)
        val V2 = VCPrivateCtorInternalVal(202)
    }
}

@JvmInline
value class VCPrivateAll private constructor(private val value: Int) {
    companion object {
        val V1 = VCPrivateAll(1001)
        val V2 = VCPrivateAll(2002)
    }
}

@JvmInline
value class VCPrivateAllNonPrimitive private constructor(private val value: String) {
    companion object {
        val V1 = VCPrivateAllNonPrimitive("V1")
        val V2 = VCPrivateAllNonPrimitive("V2")
    }
}

data class DCCopyAvailable(val value: Int)
