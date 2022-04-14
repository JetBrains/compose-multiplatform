/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.googlefonts

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Resources
import androidx.annotation.WorkerThread
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.FontResourcesParserCompat
import java.util.Arrays

@SuppressLint("ListIterator") // this is not a hot code path, nor is it optimized
@OptIn(ExperimentalTextApi::class)
@WorkerThread
internal fun GoogleFont.Provider.checkAvailable(
    packageManager: PackageManager,
    resources: Resources
): Boolean {
    // check package is available (false return paths)
    @Suppress("DEPRECATION")
    val providerInfo = packageManager.resolveContentProvider(providerAuthority, 0) ?: return false
    if (providerInfo.packageName != providerPackage) return false

    // now check signatures (true or except after this)
    val signatures = packageManager.getSignatures(providerInfo.packageName)
    val sortedSignatures = signatures.sortedWith(ByteArrayComparator)
    val allExpectedCerts = loadCertsIfNeeded(resources)
    val certsMatched = allExpectedCerts.any { certList ->
        val expected = certList?.sortedWith(ByteArrayComparator)
        if (expected?.size != sortedSignatures.size) return@any false
        for (i in expected.indices) {
            if (!Arrays.equals(expected[i], sortedSignatures[i])) return@any false
        }
        true
    }
    return if (certsMatched) {
        true
    } else {
        throwFormattedCertsMissError(signatures)
    }
}

@SuppressLint("ListIterator") // not a hot code path, not optimized
private fun throwFormattedCertsMissError(signatures: List<ByteArray>): Nothing {
    val fullDescription = signatures.joinToString(
        ",",
        prefix = "listOf(listOf(",
        postfix = "))"
    ) { repr(it) }
    throw IllegalStateException(
        "Provided signatures did not match. Actual signatures of package are:\n\n$fullDescription"
    )
}

private fun repr(b: ByteArray): String {
    return b.joinToString(",", prefix = "byteArrayOf(", postfix = ")")
}

@OptIn(ExperimentalTextApi::class)
private fun GoogleFont.Provider.loadCertsIfNeeded(resources: Resources): List<List<ByteArray?>?> {
    if (certificates != null) {
        return certificates
    }

    return FontResourcesParserCompat.readCerts(resources, certificatesRes)
}

private fun PackageManager.getSignatures(packageName: String): List<ByteArray> {
    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    val packageInfo: PackageInfo = getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    @Suppress("DEPRECATION")
    return convertToByteArrayList(packageInfo.signatures)
}

private val ByteArrayComparator = Comparator { l: ByteArray, r: ByteArray ->
    if (l.size != r.size) {
        return@Comparator l.size - r.size
    }
    var i = 0
    while (i < l.size) {
        if (l[i] != r[i]) {
            return@Comparator l[i] - r[i]
        }
        ++i
    }
    0
}

private fun convertToByteArrayList(signatures: Array<Signature>): List<ByteArray> {
    val shaList: MutableList<ByteArray> = ArrayList()
    for (signature in signatures) {
        shaList.add(signature.toByteArray())
    }
    return shaList
}
