import com.google.r4a.frames.AbstractRecord
import com.google.r4a.frames.Holder
import com.google.r4a.frames.frameId
import com.google.r4a.frames.readable
import com.google.r4a.frames.writable

class Address(street: String, city: String): Holder<Address.AddressRecord> {
    var street: String
        get() = next.readable().street
        set(value: String) { next.writable(this).street = value }

    var city: String
        get() = next.readable().city
        set(value: String) { next.writable(this).city = value }


    private var next: AddressRecord

    init {
        next = AddressRecord()
        next.street = street
        next.city = city
        next.maxFrame = Int.MAX_VALUE
        next.minFrame = frameId
    }

    class AddressRecord() : AbstractRecord<AddressRecord>() {
        var street: String = ""
        var city: String = ""

        override fun create() = AddressRecord()
        override fun assign(value: AddressRecord) {
            street = value.street
            city = value.city
        }
    }

    override fun prepend(r: AddressRecord) {
        r.next = next
        next = r
    }

    override val first: AddressRecord get() = next
}

