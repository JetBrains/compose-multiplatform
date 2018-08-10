package com.google.r4a.mock

// <linear>
//  <text text="Name: ${contact.name}" />
//  <text text="email: ${contact.email" />
// </linear>
fun ViewComposition.contact(contact: Contact) {
    linear {
        text(value = "Name: ${contact.name}")
        text(value = "email: ${contact.email}")
    }
}

fun ViewValidator.contact(contact: Contact) {
    linear {
        text(value = "Name: ${contact.name}")
        text(value = "email: ${contact.email}")
    }
}

// <linear>
//   <repeat of=contacts>
//    <selectBox selected=(it == selected)>
//      <contact contact=it />
//    <selectBox>
//   </repeat>
// </linear>
fun ViewComposition.contacts(contacts: Collection<Contact>, selected: Contact?) {
    linear {
        repeat(of = contacts) {
            selectBox(it == selected) {
                contact(it)
            }
        }
    }
}

fun ViewValidator.contacts(contacts: Collection<Contact>, selected: Contact?) {
    linear {
        repeat(of = contacts) {
            selectBox(it == selected) {
                contact(it)
            }
        }
    }
}

// <linear>
//   <linear>
//     <text value="Filter:" />
//     <edit value=model.filter />
//   </linear>
//   <linear>
//     <text value="Contacts:" />
//     <contacts contacts=model.filtered selected=model.selected />
//   </linear>
// </linear>
fun ViewComposition.selectContact(model: ContactModel) {
    linear {
        linear {
            text(value = "Filter:")
            edit(value = model.filter)
        }

        linear {
            text(value = "Contacts:")
            contacts(model.filtered, model.selected)
        }
    }
}

fun ViewValidator.selectContact(model: ContactModel) {
    linear {
        linear {
            text(value = "Filter:")
            edit(value = model.filter)
        }

        linear {
            text(value = "Contacts:")
            contacts(model.filtered, model.selected)
        }
    }
}
