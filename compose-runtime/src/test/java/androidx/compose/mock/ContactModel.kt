package androidx.compose.mock

class ContactModel(
    var filter: String = "",
    val contacts: MutableList<Contact>,
    var selected: Contact? = null
) {
    val filtered get() = contacts.filter { it.name.contains(filter) }

    fun add(contact: Contact, after: Contact? = null) {
        if (after == null) {
            contacts.add(contact)
        } else {
            contacts.add(find(after) + 1, contact)
        }
    }

    fun move(contact: Contact, after: Contact?) {
        if (after == null) {
            contacts.removeAt(find(contact))
            contacts.add(0, contact)
        } else {
            contacts.removeAt(find(contact))
            contacts.add(find(after) + 1, contact)
        }
    }

    private fun find(contact: Contact): Int {
        val index = contacts.indexOf(contact)
        if (index < 0) error("Contact $contact not found")
        return index
    }
}
