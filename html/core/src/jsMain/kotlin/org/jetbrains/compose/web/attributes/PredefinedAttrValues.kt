package org.jetbrains.compose.web.attributes

import org.w3c.dom.events.Event

sealed class InputType<T>(val typeStr: String) {

    object Button : InputTypeWithUnitValue("button")
    object Checkbox : InputTypeCheckedValue("checkbox")
    object Color : InputTypeWithStringValue("color")
    object Date : InputTypeWithStringValue("date")
    object DateTimeLocal : InputTypeWithStringValue("datetime-local")
    object Email : InputTypeWithStringValue("email")
    object File : InputTypeWithStringValue("file")
    object Hidden : InputTypeWithStringValue("hidden")
    object Month : InputTypeWithStringValue("month")
    object Number : InputTypeNumberValue("number")
    object Password : InputTypeWithStringValue("password")
    object Radio : InputTypeCheckedValue("radio")
    object Range : InputTypeNumberValue("range")
    object Search : InputTypeWithStringValue("search")
    object Submit : InputTypeWithUnitValue("submit")
    object Tel : InputTypeWithStringValue("tel")
    object Text : InputTypeWithStringValue("text")
    object Time : InputTypeWithStringValue("time")
    object Url : InputTypeWithStringValue("url")
    object Week : InputTypeWithStringValue("week")

    open class InputTypeWithStringValue(name: String) : InputType<String>(name) {
        override fun inputValue(event: Event) = Week.valueAsString(event)
    }

    open class InputTypeWithUnitValue(name: String) : InputType<Unit>(name) {
        override fun inputValue(event: Event) = Unit
    }

    open class InputTypeCheckedValue(name: String) : InputType<Boolean>(name) {
        override fun inputValue(event: Event): Boolean {
            return event.target?.asDynamic()?.checked?.unsafeCast<Boolean>() ?: false
        }
    }

    open class InputTypeNumberValue(name: String) : InputType<kotlin.Number?>(name) {
        override fun inputValue(event: Event): kotlin.Number? {
            return event.target?.asDynamic()?.valueAsNumber ?: null
        }
    }

    abstract fun inputValue(event: Event): T

    protected fun valueAsString(event: Event): String {
        return event.target?.asDynamic()?.value?.unsafeCast<String>() ?: ""
    }

    companion object {
        internal fun fromString(type: String): InputType<*> {
            return when (type) {
                "button" -> Button
                "checkbox" -> Checkbox
                "color" -> Color
                "date" -> Date
                "datetime-local" -> DateTimeLocal
                "email" -> Email
                "file" -> File
                "hidden" -> Hidden
                "month" -> Month
                "number" -> Number
                "password" -> Password
                "radio" -> Radio
                "range" -> Range
                "search" -> Search
                "submit" -> Submit
                "tel" -> Tel
                "text" -> Text
                "time" -> Time
                "url" -> Url
                "week" -> Week
                else -> error("fromString got unknown type - $type")
            }
        }
    }
}

sealed class DirType(val dirStr: String) {
    object Ltr : DirType("ltr")
    object Rtl : DirType("rtl")
    object Auto : DirType("auto")
}

sealed class ATarget(val targetStr: String) {
    object Blank : ATarget("_blank")
    object Parent : ATarget("_parent")
    object Self : ATarget("_self")
    object Top : ATarget("_top")
}

sealed class ARel(val relStr: String) {
    object Alternate : ARel("alternate")
    object Author : ARel("author")
    object Bookmark : ARel("bookmark")
    object External : ARel("external")
    object Help : ARel("help")
    object License : ARel("license")
    object Next : ARel("next")
    object First : ARel("first")
    object Prev : ARel("prev")
    object Last : ARel("last")
    object NoFollow : ARel("nofollow")
    object NoOpener : ARel("noopener")
    object NoReferrer : ARel("noreferrer")
    object Opener : ARel("opener")
    object Search : ARel("search")
    object Tag : ARel("tag")

    class CustomARel(value: String) : ARel(value)
}

enum class Draggable(val str: String) {
    True("true"), False("false"), Auto("auto");
}

enum class ButtonType(val str: String) {
    Button("button"), Reset("reset"), Submit("submit")
}

sealed class ButtonFormTarget(val targetStr: String) {
    object Blank : ButtonFormTarget("_blank")
    object Parent : ButtonFormTarget("_parent")
    object Self : ButtonFormTarget("_self")
    object Top : ButtonFormTarget("_top")
}

enum class ButtonFormMethod(val methodStr: String) {
    Get("get"), Post("post")
}

enum class ButtonFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class FormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class FormTarget(val targetStr: String) {
    object Blank : FormTarget("_blank")
    object Parent : FormTarget("_parent")
    object Self : FormTarget("_self")
    object Top : FormTarget("_top")
}

enum class InputFormEncType(val typeStr: String) {
    MultipartFormData("multipart/form-data"),
    ApplicationXWwwFormUrlEncoded("application/x-www-form-urlencoded"),
    TextPlain("text/plain")
}

enum class InputFormMethod(val methodStr: String) {
    Get("get"),
    Post("post"),
    Dialog("dialog")
}

sealed class InputFormTarget(val targetStr: String) {
    object Blank : InputFormTarget("_blank")
    object Parent : InputFormTarget("_parent")
    object Self : InputFormTarget("_self")
    object Top : InputFormTarget("_top")
}

enum class TextAreaWrap(val str: String) {
    Hard("hard"),
    Soft("soft"),
    Off("off")
}

enum class Scope(val str: String) {
    Row("row"),
    Rowgroup("rowgroup"),
    Col("col"),
    Colgroup("colgroup")
}

/**
 * see https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode
 */
enum class InputMode(val str: String) {
    None("none"),
    Text("text"), // default
    Decimal("decimal"),
    Numeric("numeric"),
    Tel("tel"),
    Search("search"),
    Email("email"),
    Url("url"),
    ;
}


/**
 * https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete
 */
@Suppress("Unused", "NOTHING_TO_INLINE", "NESTED_CLASS_IN_EXTERNAL_INTERFACE", "INLINE_EXTERNAL_DECLARATION", "WRONG_BODY_OF_EXTERNAL_DECLARATION", "NESTED_EXTERNAL_DECLARATION", "ClassName")
interface AutoComplete {
     companion object {
        /**
         * The browser is not permitted to automatically enter or select a value for this field. It is possible that the document or application provides its own autocomplete feature, or that security concerns require that the field's value not be automatically entered.
         * Note: In most modern browsers, setting autocomplete to "off" will not prevent a password manager from asking the user if they would like to save username and password information, or from automatically filling in those values in a site's login form. See the autocomplete attribute and login fields.
         */
        inline val off get() = AutoComplete("off")

        /**
         * The browser is allowed to automatically complete the input. No guidance is provided as to the type of data expected in the field, so the browser may use its own judgement.
         */
        inline val on get() = AutoComplete("on")

        /**
         *The field expects the value to be a person's full name. Using "name" rather than breaking the name down into its components is generally preferred because it avoids dealing with the wide diversity of human names and how they are structured; however, you can use the following autocomplete values if you do need to break the name down into its components:
         */
        inline val name get() = AutoComplete("name")

        /**
         * The prefix or title, such as "Mrs.", "Mr.", "Miss", "Ms.", "Dr.", or "Mlle.".
         */
        inline val honorificPrefix get() = AutoComplete("honorific-prefix")

        /**
         * The given (or "first") name.
         */
        inline val givenName get() = AutoComplete("given-name")

        /**
         * The middle name.
         */
        inline val additionalName get() = AutoComplete("additional-name")

        /**
         * The family (or "last") name.
         */
        inline val familyName get() = AutoComplete("family-name")

        /**
         * The suffix, such as "Jr.", "B.Sc.", "PhD.", "MBASW", or "IV".
         */
        inline val honorificSuffix get() = AutoComplete("honorific-suffix")

        /**
         * A nickname or handle.
         */
        inline val nickname get() = AutoComplete("nickname")

        /**
         * An email address.
         */
        inline val email get() = AutoComplete("email")

        /**
         * A username or account name.
         */
        inline val username get() = AutoComplete("username")

        /**
         * A new password. When creating a new account or changing passwords, this should be used for an "Enter your new password" or "Confirm new password" field, as opposed to a general "Enter your current password" field that might be present. This may be used by the browser both to avoid accidentally filling in an existing password and to offer assistance in creating a secure password (see also Preventing autofilling with autocomplete="new-password").
         */
        inline val newPassword get() = AutoComplete("new-password")

        /**
         * The user's current password.
         */
        inline val currentPassword get() = AutoComplete("current-password")

        /**
         * A one-time code used for verifying user identity.
         */
        inline val oneTimeCode get() = AutoComplete("one-time-code")

        /**
         * A job title, or the title a person has within an organization, such as "Senior Technical Writer", "President", or "Assistant Troop Leader".
         */
        inline val organizationTitle get() = AutoComplete("organization-title")

        /**
         * A company or organization name, such as "Acme Widget Company" or "Girl Scouts of America".
         */
        inline val organization get() = AutoComplete("organization")

        /**
         * A street address. This can be multiple lines of text, and should fully identify the location of the address within its second administrative level (typically a city or town), but should not include the city name, ZIP or postal code, or country name.
         */
        inline val streetAddress get() = AutoComplete("street-address")

        /**
         * Each individual line of the street address. These should only be present if the "street-address" is not present.
         */
        inline val addressLine1 get() = AutoComplete("address-line1")
        inline val addressLine2 get() = AutoComplete("address-line2")
        inline val addressLine3 get() = AutoComplete("address-line3")

        /**
         * The first administrative level in the address. This is typically the province in which the address is located. In the United States, this would be the state. In Switzerland, the canton. In the United Kingdom, the post town.
         */
        inline val addressLevel1 get() = AutoComplete("address-level1")

        /**
         * The second administrative level, in addresses with at least two of them. In countries with two administrative levels, this would typically be the city, town, village, or other locality in which the address is located.
         */
        inline val addressLevel2 get() = AutoComplete("address-level2")

        /**
         * The third administrative level, in addresses with at least three administrative levels.
         */
        inline val addressLevel3 get() = AutoComplete("address-level3")

        /**
         * The finest-grained administrative level, in addresses which have four levels.
         */
        inline val addressLevel4 get() = AutoComplete("address-level4")

        /**
         * A country or territory code.
         */
        inline val country get() = AutoComplete("country")

        /**
         * A country or territory name.
         */
        inline val countryName get() = AutoComplete("country-name")

        /**
         * A postal code (in the United States, this is the ZIP code).
         */
        inline val postalCode get() = AutoComplete("postal-code")

        /**
         * The full name as printed on or associated with a payment instrument such as a credit card. Using a full name field is preferred, typically, over breaking the name into pieces.
         */
        inline val ccName get() = AutoComplete("cc-name")

        /**
         * A given (first) name as given on a payment instrument like a credit card.
         */
        inline val ccGivenName get() = AutoComplete("cc-given-name")

        /**
         * A middle name as given on a payment instrument or credit card.
         */
        inline val ccAdditionalName get() = AutoComplete("cc-additional-name")

        /**
         * A family name, as given on a credit card.
         */
        inline val ccFamilyName get() = AutoComplete("cc-family-name")

        /**
         * A credit card number or other number identifying a payment method, such as an account number.
         */
        inline val ccNumber get() = AutoComplete("cc-number")

        /**
         * A payment method expiration date, typically in the form "MM/YY" or "MM/YYYY".
         */
        inline val ccExp get() = AutoComplete("cc-exp")

        /**
         * The month in which the payment method expires.
         */
        inline val ccExpMonth get() = AutoComplete("cc-exp-month")

        /**
         * The year in which the payment method expires.
         */
        inline val ccExpYear get() = AutoComplete("cc-exp-year")

        /**
         * The security code for the payment instrument; on credit cards, this is the 3-digit verification number on the back of the card.
         */
        inline val ccSecurityCode get() = AutoComplete("cc-csc")

        /**
         * The type of payment instrument (such as "Visa" or "Master Card").
         */
        inline val ccType get() = AutoComplete("cc-type")

        /**
         * The currency in which the transaction is to take place.
         */
        inline val transactionCurrency get() = AutoComplete("transaction-currency")

        /**
         * The amount, given in the currency specified by "transaction-currency", of the transaction, for a payment form.
         */
        inline val transactionAmount get() = AutoComplete("transaction-amount")

        /**
         * A preferred language, given as a valid BCP 47 language tag.
         */
        inline val language get() = AutoComplete("language")

        /**
         * A birth date, as a full date.
         */
        inline val birthdate get() = AutoComplete("bday")

        /**
         * The day of the month of a birth date.
         */
        inline val birthdateDay get() = AutoComplete("bday-day")

        /**
         * The month of the year of a birth date.
         */
        inline val birthdateMonth get() = AutoComplete("bday-month")

        /**
         * The year of a birth date.
         */
        inline val birthdateYear get() = AutoComplete("bday-year")


        /**
         * A gender identity (such as "Female", "Fa'afafine", "Male"), as freeform text without newlines.
         */
        inline val sex get() = AutoComplete("sex")

        /**
         * A full telephone number, including the country code. If you need to break the phone number up into its components, you can use these values for those fields:
         */
        inline val tel get() = AutoComplete("tel")

        /**
         * The country code, such as "1" for the United States, Canada, and other areas in North America and parts of the Caribbean.
         */
        inline val telCountryCode get() = AutoComplete("tel-country-code")

        /**
         * The entire phone number without the country code component, including a country-internal prefix. For the phone number "1-855-555-6502", this field's value would be "855-555-6502".
         */
        inline val telNational get() = AutoComplete("tel-national")

        /**
         * The area code, with any country-internal prefix applied if appropriate.
         */
        inline val telAreaCode get() = AutoComplete("tel-area-code")

        /**
         * The phone number without the country or area code. This can be split further into two parts, for phone numbers which have an exchange number and then a number within the exchange. For the phone number "555-6502", use "tel-local-prefix" for "555" and "tel-local-suffix" for "6502".
         */
        inline val telLocal get() = AutoComplete("tel-local")

        /**
         * A telephone extension code within the phone number, such as a room or suite number in a hotel or an office extension in a company.
         */
        inline val telExtension get() = AutoComplete("tel-extension")

        /**
         * A URL for an instant messaging protocol endpoint, such as "xmpp:username@example.net".
         */
        inline val impp get() = AutoComplete("impp")

        /**
         * A URL, such as a home page or company web site address as appropriate given the context of the other fields in the form.
         */
        inline val url get() = AutoComplete("url")

        /**
         * The URL of an image representing the person, company, or contact information given in the other fields in the form.
         */
        inline val photo get() = AutoComplete("photo")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun AutoComplete(value: String) = value.unsafeCast<AutoComplete>()
