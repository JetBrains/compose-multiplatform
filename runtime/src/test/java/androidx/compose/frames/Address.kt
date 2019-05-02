package androidx.compose.frames

class Address(street: String, city: String) : Framed {
    var street: String
        get() = next.readable(this).street
        set(value) { next.writable(this).street = value }

    var city: String
        get() = next.readable(this).city
        set(value) { next.writable(this).city = value }

    private var next: AddressRecord

    init {
        next = AddressRecord()
        next.street = street
        next.city = city
    }

    override fun prependFrameRecord(value: Record) {
        value.next = next
        next = value as AddressRecord
    }

    override val firstFrameRecord: AddressRecord get() = next
}

class AddressRecord : AbstractRecord() {
    var street: String = ""
    var city: String = ""

    override fun create(): Record = AddressRecord()
    override fun assign(value: Record) {
        val other = value as AddressRecord
        street = other.street
        city = other.city
    }
}
