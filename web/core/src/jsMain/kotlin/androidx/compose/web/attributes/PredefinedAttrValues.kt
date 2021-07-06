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
 * https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete
 */
public object AutoComplete {
    /**
     * The browser is not permitted to automatically enter or select a value for this field. It is possible that the document or application provides its own autocomplete feature, or that security concerns require that the field's value not be automatically entered.
     * Note: In most modern browsers, setting autocomplete to "off" will not prevent a password manager from asking the user if they would like to save username and password information, or from automatically filling in those values in a site's login form. See the autocomplete attribute and login fields.
     */
    public const val off: String = "off"

    /**
     * The browser is allowed to automatically complete the input. No guidance is provided as to the type of data expected in the field, so the browser may use its own judgement.
     */
    public const val on: String = "on"

    /**
     *The field expects the value to be a person's full name. Using "name" rather than breaking the name down into its components is generally preferred because it avoids dealing with the wide diversity of human names and how they are structured; however, you can use the following autocomplete values if you do need to break the name down into its components:
     */
    public const val name: String = "name"

    /**
     * The prefix or title, such as "Mrs.", "Mr.", "Miss", "Ms.", "Dr.", or "Mlle.".
     */
    public const val honorificPrefix: String = "honorific-prefix"

    /**
     * The given (or "first") name.
     */
    public const val givenName: String = "given-name"

    /**
     * The middle name.
     */
    public const val additionalName: String = "additional-name"

    /**
     * The family (or "last") name.
     */
    public const val familyName: String = "family-name"

    /**
     * The suffix, such as "Jr.", "B.Sc.", "PhD.", "MBASW", or "IV".
     */
    public const val honorificSuffix: String = "honorific-suffix"

    /**
     * A nickname or handle.
     */
    public const val nickname: String = "nickname"

    /**
     * An email address.
     */
    public const val email: String = "email"

    /**
     * A username or account name.
     */
    public const val username: String = "username"

    /**
     * A new password. When creating a new account or changing passwords, this should be used for an "Enter your new password" or "Confirm new password" field, as opposed to a general "Enter your current password" field that might be present. This may be used by the browser both to avoid accidentally filling in an existing password and to offer assistance in creating a secure password (see also Preventing autofilling with autocomplete="new-password").
     */
    public const val newPassword: String = "new-password"

    /**
     * The user's current password.
     */
    public const val currentPassword: String = "current-password"

    /**
     * A one-time code used for verifying user identity.
     */
    public const val oneTimeCode: String = "one-time-code"

    /**
     * A job title, or the title a person has within an organization, such as "Senior Technical Writer", "President", or "Assistant Troop Leader".
     */
    public const val organizationTitle: String = "organization-title"

    /**
     * A company or organization name, such as "Acme Widget Company" or "Girl Scouts of America".
     */
    public const val organization: String = "organization"

    /**
     * A street address. This can be multiple lines of text, and should fully identify the location of the address within its second administrative level (typically a city or town), but should not include the city name, ZIP or postal code, or country name.
     */
    public const val streetAddress: String = "street-address"

    /**
     * Each individual line of the street address. These should only be present if the "street-address" is not present.
     */
    public const val addressLine1: String = "address-line1"
    public const val addressLine2: String = "address-line2"
    public const val addressLine3: String = "address-line3"

    /**
     * The first administrative level in the address. This is typically the province in which the address is located. In the United States, this would be the state. In Switzerland, the canton. In the United Kingdom, the post town.
     */
    public const val addressLevel1: String = "address-level1"

    /**
     * The second administrative level, in addresses with at least two of them. In countries with two administrative levels, this would typically be the city, town, village, or other locality in which the address is located.
     */
    public const val addressLevel2: String = "address-level2"

    /**
     * The third administrative level, in addresses with at least three administrative levels.
     */
    public const val addressLevel3: String = "address-level3"

    /**
     * The finest-grained administrative level, in addresses which have four levels.
     */
    public const val addressLevel4: String = "address-level4"

    /**
     * A country or territory code.
     */
    public const val country: String = "country"

    /**
     * A country or territory name.
     */
    public const val countryName: String = "country-name"

    /**
     * A postal code (in the United States, this is the ZIP code).
     */
    public const val postalCode: String = "postal-code"

    /**
     * The full name as printed on or associated with a payment instrument such as a credit card. Using a full name field is preferred, typically, over breaking the name into pieces.
     */
    public const val ccName: String = "cc-name"

    /**
     * A given (first) name as given on a payment instrument like a credit card.
     */
    public const val ccGivenName: String = "cc-given-name"

    /**
     * A middle name as given on a payment instrument or credit card.
     */
    public const val ccAdditionalName: String = "cc-additional-name"

    /**
     * A family name, as given on a credit card.
     */
    public const val ccFamilyName: String = "cc-family-name"

    /**
     * A credit card number or other number identifying a payment method, such as an account number.
     */
    public const val ccNumber: String = "cc-number"

    /**
     * A payment method expiration date, typically in the form "MM/YY" or "MM/YYYY".
     */
    public const val ccExp: String = "cc-exp"

    /**
     * The month in which the payment method expires.
     */
    public const val ccExpMonth: String = "cc-exp-month"

    /**
     * The year in which the payment method expires.
     */
    public const val ccExpYear: String = "cc-exp-year"

    /**
     * The security code for the payment instrument; on credit cards, this is the 3-digit verification number on the back of the card.
     */
    public const val ccSecurityCode: String = "cc-csc"

    /**
     * The type of payment instrument (such as "Visa" or "Master Card").
     */
    public const val ccType: String = "cc-type"

    /**
     * The currency in which the transaction is to take place.
     */
    public const val transactionCurrency: String = "transaction-currency"

    /**
     * The amount, given in the currency specified by "transaction-currency", of the transaction, for a payment form.
     */
    public const val transactionAmount: String = "transaction-amount"

    /**
     * A preferred language, given as a valid BCP 47 language tag.
     */
    public const val language: String = "language"

    /**
     * A birth date, as a full date.
     */
    public const val birthdate: String = "bday"

    /**
     * The day of the month of a birth date.
     */
    public const val birthdateDay: String = "bday-day"

    /**
     * The month of the year of a birth date.
     */
    public const val birthdateMonth: String = "bday-month"

    /**
     * The year of a birth date.
     */
    public const val birthdateYear: String = "bday-year"


    /**
     * A gender identity (such as "Female", "Fa'afafine", "Male"), as freeform text without newlines.
     */
    public const val sex: String = "sex"

    /**
     * A full telephone number, including the country code. If you need to break the phone number up into its components, you can use these values for those fields:
     */
    public const val tel: String = "tel"

    /**
     * The country code, such as "1" for the United States, Canada, and other areas in North America and parts of the Caribbean.
     */
    public const val telCountryCode: String = "tel-country-code"

    /**
     * The entire phone number without the country code component, including a country-internal prefix. For the phone number "1-855-555-6502", this field's value would be "855-555-6502".
     */
    public const val telNational: String = "tel-national"

    /**
     * The area code, with any country-internal prefix applied if appropriate.
     */
    public const val telAreaCode: String = "tel-area-code"

    /**
     * The phone number without the country or area code. This can be split further into two parts, for phone numbers which have an exchange number and then a number within the exchange. For the phone number "555-6502", use "tel-local-prefix" for "555" and "tel-local-suffix" for "6502".
     */
    public const val telLocal: String = "tel-local"

    /**
     * A telephone extension code within the phone number, such as a room or suite number in a hotel or an office extension in a company.
     */
    public const val telExtension: String = "tel-extension"

    /**
     * A URL for an instant messaging protocol endpoint, such as "xmpp:username@example.net".
     */
    public const val impp: String = "impp"

    /**
     * A URL, such as a home page or company web site address as appropriate given the context of the other fields in the form.
     */
    public const val url: String = "url"

    /**
     * The URL of an image representing the person, company, or contact information given in the other fields in the form.
     */
    public const val photo: String = "photo"
}
