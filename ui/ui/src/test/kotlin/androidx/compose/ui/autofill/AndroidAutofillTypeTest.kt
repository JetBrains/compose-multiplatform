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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class AndroidAutofillTypeTest {

    @Test
    fun emailAddress() {
        assertThat(EmailAddress.androidType).isEqualTo("emailAddress")
    }

    @Test
    fun username() {
        assertThat(Username.androidType).isEqualTo("username")
    }

    @Test
    fun password() {
        assertThat(Password.androidType).isEqualTo("password")
    }

    @Test
    fun newUsername() {
        assertThat(NewUsername.androidType).isEqualTo("newUsername")
    }

    @Test
    fun newPassword() {
        assertThat(NewPassword.androidType).isEqualTo("newPassword")
    }

    @Test
    fun postalAddress() {
        assertThat(PostalAddress.androidType).isEqualTo("postalAddress")
    }

    @Test
    fun postalCode() {
        assertThat(PostalCode.androidType).isEqualTo("postalCode")
    }

    @Test
    fun creditCardNumber() {
        assertThat(CreditCardNumber.androidType).isEqualTo("creditCardNumber")
    }

    @Test
    fun creditCardSecurityCode() {
        assertThat(CreditCardSecurityCode.androidType).isEqualTo("creditCardSecurityCode")
    }

    @Test
    fun creditCardExpirationDate() {
        assertThat(CreditCardExpirationDate.androidType).isEqualTo("creditCardExpirationDate")
    }

    @Test
    fun creditCardExpirationMonth() {
        assertThat(CreditCardExpirationMonth.androidType).isEqualTo("creditCardExpirationMonth")
    }

    @Test
    fun creditCardExpirationYear() {
        assertThat(CreditCardExpirationYear.androidType).isEqualTo("creditCardExpirationYear")
    }

    @Test
    fun creditCardExpirationDay() {
        assertThat(CreditCardExpirationDay.androidType).isEqualTo("creditCardExpirationDay")
    }

    @Test
    fun addressCountry() {
        assertThat(AddressCountry.androidType).isEqualTo("addressCountry")
    }

    @Test
    fun addressRegion() {
        assertThat(AddressRegion.androidType).isEqualTo("addressRegion")
    }

    @Test
    fun addressLocality() {
        assertThat(AddressLocality.androidType).isEqualTo("addressLocality")
    }

    @Test
    fun addressStreet() {
        assertThat(AddressStreet.androidType).isEqualTo("streetAddress")
    }

    @Test
    fun addressAuxiliaryDetails() {
        assertThat(AddressAuxiliaryDetails.androidType).isEqualTo("extendedAddress")
    }

    @Test
    fun postalCodeExtended() {
        assertThat(PostalCodeExtended.androidType).isEqualTo("extendedPostalCode")
    }

    @Test
    fun personFullName() {
        assertThat(PersonFullName.androidType).isEqualTo("personName")
    }

    @Test
    fun personFirstName() {
        assertThat(PersonFirstName.androidType).isEqualTo("personGivenName")
    }

    @Test
    fun personLastName() {
        assertThat(PersonLastName.androidType).isEqualTo("personFamilyName")
    }

    @Test
    fun personMiddleName() {
        assertThat(PersonMiddleName.androidType).isEqualTo("personMiddleName")
    }

    @Test
    fun personMiddleInitial() {
        assertThat(PersonMiddleInitial.androidType).isEqualTo("personMiddleInitial")
    }

    @Test
    fun personNamePrefix() {
        assertThat(PersonNamePrefix.androidType).isEqualTo("personNamePrefix")
    }

    @Test
    fun personNameSuffix() {
        assertThat(PersonNameSuffix.androidType).isEqualTo("personNameSuffix")
    }

    @Test
    fun phoneNumber() {
        assertThat(PhoneNumber.androidType).isEqualTo("phoneNumber")
    }

    @Test
    fun phoneNumberDevice() {
        assertThat(PhoneNumberDevice.androidType).isEqualTo("phoneNumberDevice")
    }

    @Test
    fun phoneCountryCode() {
        assertThat(PhoneCountryCode.androidType).isEqualTo("phoneCountryCode")
    }

    @Test
    fun phoneNumberNational() {
        assertThat(PhoneNumberNational.androidType).isEqualTo("phoneNational")
    }

    @Test
    fun gender() {
        assertThat(Gender.androidType).isEqualTo("gender")
    }

    @Test
    fun birthDateFull() {
        assertThat(BirthDateFull.androidType).isEqualTo("birthDateFull")
    }

    @Test
    fun birthDateDay() {
        assertThat(BirthDateDay.androidType).isEqualTo("birthDateDay")
    }

    @Test
    fun birthDateMonth() {
        assertThat(BirthDateMonth.androidType).isEqualTo("birthDateMonth")
    }

    @Test
    fun birthDateYear() {
        assertThat(BirthDateYear.androidType).isEqualTo("birthDateYear")
    }

    @Test
    fun smsOTPCode() {
        assertThat(SmsOtpCode.androidType).isEqualTo("smsOTPCode")
    }
}
