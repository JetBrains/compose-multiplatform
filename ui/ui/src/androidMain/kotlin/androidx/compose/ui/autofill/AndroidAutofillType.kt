/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.autofill

import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_DAY
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_MONTH
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_YEAR
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_NUMBER
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_GENDER
import androidx.autofill.HintConstants.AUTOFILL_HINT_NEW_PASSWORD
import androidx.autofill.HintConstants.AUTOFILL_HINT_NEW_USERNAME
import androidx.autofill.HintConstants.AUTOFILL_HINT_PASSWORD
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_PREFIX
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_SUFFIX
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NATIONAL
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NUMBER
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NUMBER_DEVICE
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_SMS_OTP
import androidx.autofill.HintConstants.AUTOFILL_HINT_USERNAME
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillType.AddressAuxiliaryDetails
import androidx.compose.ui.autofill.AutofillType.AddressCountry
import androidx.compose.ui.autofill.AutofillType.AddressLocality
import androidx.compose.ui.autofill.AutofillType.AddressRegion
import androidx.compose.ui.autofill.AutofillType.AddressStreet
import androidx.compose.ui.autofill.AutofillType.BirthDateDay
import androidx.compose.ui.autofill.AutofillType.BirthDateFull
import androidx.compose.ui.autofill.AutofillType.BirthDateMonth
import androidx.compose.ui.autofill.AutofillType.BirthDateYear
import androidx.compose.ui.autofill.AutofillType.CreditCardExpirationDate
import androidx.compose.ui.autofill.AutofillType.CreditCardExpirationDay
import androidx.compose.ui.autofill.AutofillType.CreditCardExpirationMonth
import androidx.compose.ui.autofill.AutofillType.CreditCardExpirationYear
import androidx.compose.ui.autofill.AutofillType.CreditCardNumber
import androidx.compose.ui.autofill.AutofillType.CreditCardSecurityCode
import androidx.compose.ui.autofill.AutofillType.EmailAddress
import androidx.compose.ui.autofill.AutofillType.Gender
import androidx.compose.ui.autofill.AutofillType.NewPassword
import androidx.compose.ui.autofill.AutofillType.NewUsername
import androidx.compose.ui.autofill.AutofillType.Password
import androidx.compose.ui.autofill.AutofillType.PersonFirstName
import androidx.compose.ui.autofill.AutofillType.PersonFullName
import androidx.compose.ui.autofill.AutofillType.PersonLastName
import androidx.compose.ui.autofill.AutofillType.PersonMiddleInitial
import androidx.compose.ui.autofill.AutofillType.PersonMiddleName
import androidx.compose.ui.autofill.AutofillType.PersonNamePrefix
import androidx.compose.ui.autofill.AutofillType.PersonNameSuffix
import androidx.compose.ui.autofill.AutofillType.PhoneCountryCode
import androidx.compose.ui.autofill.AutofillType.PhoneNumber
import androidx.compose.ui.autofill.AutofillType.PhoneNumberDevice
import androidx.compose.ui.autofill.AutofillType.PhoneNumberNational
import androidx.compose.ui.autofill.AutofillType.PostalAddress
import androidx.compose.ui.autofill.AutofillType.PostalCode
import androidx.compose.ui.autofill.AutofillType.PostalCodeExtended
import androidx.compose.ui.autofill.AutofillType.SmsOtpCode
import androidx.compose.ui.autofill.AutofillType.Username

/**
 * Gets the Android specific [AutofillHint][android.view.ViewStructure.setAutofillHints]
 * corresponding to the current [AutofillType].
 */
@ExperimentalComposeUiApi
internal val AutofillType.androidType: String
    get() {
        val androidAutofillType = androidAutofillTypes[this]
        requireNotNull(androidAutofillType, { "Unsupported autofill type" })
        return androidAutofillType
    }

/**
 * Maps each [AutofillType] to one of the  autofill hints in [androidx.autofill.HintConstants]
 */
@ExperimentalComposeUiApi
private val androidAutofillTypes: HashMap<AutofillType, String> = hashMapOf(
    EmailAddress to AUTOFILL_HINT_EMAIL_ADDRESS,
    Username to AUTOFILL_HINT_USERNAME,
    Password to AUTOFILL_HINT_PASSWORD,
    NewUsername to AUTOFILL_HINT_NEW_USERNAME,
    NewPassword to AUTOFILL_HINT_NEW_PASSWORD,
    PostalAddress to AUTOFILL_HINT_POSTAL_ADDRESS,
    PostalCode to AUTOFILL_HINT_POSTAL_CODE,
    CreditCardNumber to AUTOFILL_HINT_CREDIT_CARD_NUMBER,
    CreditCardSecurityCode to AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
    CreditCardExpirationDate to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
    CreditCardExpirationMonth to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
    CreditCardExpirationYear to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
    CreditCardExpirationDay to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY,
    AddressCountry to AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY,
    AddressRegion to AUTOFILL_HINT_POSTAL_ADDRESS_REGION,
    AddressLocality to AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY,
    AddressStreet to AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
    AddressAuxiliaryDetails to AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS,
    PostalCodeExtended to AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE,
    PersonFullName to AUTOFILL_HINT_PERSON_NAME,
    PersonFirstName to AUTOFILL_HINT_PERSON_NAME_GIVEN,
    PersonLastName to AUTOFILL_HINT_PERSON_NAME_FAMILY,
    PersonMiddleName to AUTOFILL_HINT_PERSON_NAME_MIDDLE,
    PersonMiddleInitial to AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL,
    PersonNamePrefix to AUTOFILL_HINT_PERSON_NAME_PREFIX,
    PersonNameSuffix to AUTOFILL_HINT_PERSON_NAME_SUFFIX,
    PhoneNumber to AUTOFILL_HINT_PHONE_NUMBER,
    PhoneNumberDevice to AUTOFILL_HINT_PHONE_NUMBER_DEVICE,
    PhoneCountryCode to AUTOFILL_HINT_PHONE_COUNTRY_CODE,
    PhoneNumberNational to AUTOFILL_HINT_PHONE_NATIONAL,
    Gender to AUTOFILL_HINT_GENDER,
    BirthDateFull to AUTOFILL_HINT_BIRTH_DATE_FULL,
    BirthDateDay to AUTOFILL_HINT_BIRTH_DATE_DAY,
    BirthDateMonth to AUTOFILL_HINT_BIRTH_DATE_MONTH,
    BirthDateYear to AUTOFILL_HINT_BIRTH_DATE_YEAR,
    SmsOtpCode to AUTOFILL_HINT_SMS_OTP
)
