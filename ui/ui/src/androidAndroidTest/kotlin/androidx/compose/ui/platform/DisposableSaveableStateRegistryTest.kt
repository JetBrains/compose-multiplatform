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

package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.os.Binder
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Serializable

@SmallTest
@RunWith(AndroidJUnit4::class)
class DisposableSaveableStateRegistryTest {

    private val ContainerKey = "100"
    private val SaveKey = "key"
    private val SaveValue = 5

    @UiThreadTest
    @Test
    fun simpleSaveAndRestore() {
        val owner1 = TestOwner()
        var registry = DisposableSaveableStateRegistry(ContainerKey, owner1)
        registry.registerProvider(SaveKey) { SaveValue }
        val bundle = owner1.save()

        val owner2 = TestOwner(bundle)
        registry = DisposableSaveableStateRegistry(ContainerKey, owner2)
        val restoredValue = registry.consumeRestored(SaveKey)
        assertEquals(SaveValue, restoredValue)
    }

    @UiThreadTest
    @Test
    fun saveAndRestoreWhenTwoParentsShareTheSameStateArray() {
        val owner1 = TestOwner()
        // This emulates two different AndroidComposeViews used inside the same Activity
        val parentKey1 = "1"
        val value1 = 1
        val parentKey2 = "2"
        val value2 = 2

        // save first view
        val registryToSave1 = DisposableSaveableStateRegistry(parentKey1, owner1)
        registryToSave1.registerProvider(SaveKey) { value1 }

        // save second view
        val registryToSave2 = DisposableSaveableStateRegistry(parentKey2, owner1)
        registryToSave2.registerProvider(SaveKey) { value2 }

        val owner2 = TestOwner(owner1.save())

        // restore first view
        val registryToRestore1 = DisposableSaveableStateRegistry(parentKey1, owner2)
        val restoredValue1 = registryToRestore1.consumeRestored(SaveKey)
        assertEquals(value1, restoredValue1)

        // restore second view
        val registryToRestore2 = DisposableSaveableStateRegistry(parentKey2, owner2)
        val restoredValue2 = registryToRestore2.consumeRestored(SaveKey)
        assertEquals(value2, restoredValue2)
    }

    @UiThreadTest
    @Test
    fun typesSupportedByBaseBundleCanBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        assertTrue(registry.canBeSaved(true))
        assertTrue(registry.canBeSaved(true.asBoxed()))
        assertTrue(registry.canBeSaved(true.asBoxed()))
        assertTrue(registry.canBeSaved(booleanArrayOf(true)))
        assertTrue(registry.canBeSaved(5.toDouble()))
        assertTrue(registry.canBeSaved(5.toDouble().asBoxed()))
        assertTrue(registry.canBeSaved(doubleArrayOf(5.toDouble())))
        assertTrue(registry.canBeSaved(5.toLong()))
        assertTrue(registry.canBeSaved(5.toLong().asBoxed()))
        assertTrue(registry.canBeSaved(longArrayOf(5.toLong())))
        assertTrue(registry.canBeSaved("string"))
        assertTrue(registry.canBeSaved(arrayOf("string")))
    }

    @UiThreadTest
    @Test
    fun typesSupportedByBundleCanBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        assertTrue(registry.canBeSaved(Binder()))
        assertTrue(registry.canBeSaved(Bundle()))
        assertTrue(registry.canBeSaved(5.toByte()))
        assertTrue(registry.canBeSaved(5.toByte().asBoxed()))
        assertTrue(registry.canBeSaved(byteArrayOf(5.toByte())))
        assertTrue(registry.canBeSaved(5.toChar()))
        assertTrue(registry.canBeSaved(5.toChar().asBoxed()))
        assertTrue(registry.canBeSaved(charArrayOf(5.toChar())))
        assertTrue(registry.canBeSaved(5.toFloat()))
        assertTrue(registry.canBeSaved(5.toFloat().asBoxed()))
        assertTrue(registry.canBeSaved(floatArrayOf(5.toFloat())))
        assertTrue(registry.canBeSaved(arrayListOf(5)))
        assertTrue(registry.canBeSaved(CustomParcelable()))
        assertTrue(registry.canBeSaved(arrayOf(CustomParcelable())))
        assertTrue(registry.canBeSaved(arrayListOf(CustomParcelable())))
        assertTrue(registry.canBeSaved(CustomSerializable()))
        assertTrue(registry.canBeSaved(5.toShort()))
        assertTrue(registry.canBeSaved(Size(5, 5)))
        assertTrue(registry.canBeSaved(SizeF(5f, 5f)))
        assertTrue(
            registry.canBeSaved(
                SparseArray<Parcelable>().apply {
                    put(5, CustomParcelable())
                }
            )
        )
        assertTrue(registry.canBeSaved(arrayListOf("String")))
    }

    @UiThreadTest
    @Test
    fun customTypeCantBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        assertFalse(registry.canBeSaved(CustomClass()))
    }

    @UiThreadTest
    @Test
    fun charSequenceCantBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        assertFalse(registry.canBeSaved(CustomCharSequence()))
    }

    @UiThreadTest
    @Test
    fun lambdaCantBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        val lambda: () -> Int = { 1 }
        assertFalse(registry.canBeSaved(lambda))
        val lambdaWithArguments: (String, Int) -> Unit = { _, _ -> }
        assertFalse(registry.canBeSaved(lambdaWithArguments))
        val lambdaWithReceiver: String.() -> Unit = { }
        assertFalse(registry.canBeSaved(lambdaWithReceiver))
    }

    @UiThreadTest
    @Test
    fun customParcelableFunctionCanBeSaved() {
        val registry = DisposableSaveableStateRegistry(ContainerKey, TestOwner())

        val function = CustomParcelableFunction()
        assertTrue(registry.canBeSaved(function))
    }

    private fun Any?.asBoxed(): Any = this!!
}

private class CustomClass

private class CustomSerializable : Serializable

private class CustomCharSequence : CharSequence {
    override val length: Int = 0

    override fun get(index: Int): Char = throw IllegalStateException()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        throw IllegalStateException()
    }
}

@SuppressLint("BanParcelableUsage")
private class CustomParcelable(parcel: Parcel? = null) : Parcelable {
    init {
        parcel?.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<CustomParcelable> {
            override fun createFromParcel(parcel: Parcel) = CustomParcelable(parcel)
            override fun newArray(size: Int) = arrayOfNulls<CustomParcelable?>(size)
        }
    }
}

@SuppressLint("BanParcelableUsage")
private class CustomParcelableFunction(parcel: Parcel? = null) : Parcelable, (String) -> Unit {
    init {
        parcel?.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun invoke(param: String) {}

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<CustomParcelableFunction> {
            override fun createFromParcel(parcel: Parcel) = CustomParcelableFunction(parcel)
            override fun newArray(size: Int) = arrayOfNulls<CustomParcelableFunction?>(size)
        }
    }
}

private class TestOwner(
    restoredBundle: Bundle? = null
) : SavedStateRegistryOwner {

    private val lifecycle = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this).apply {
        performRestore(restoredBundle ?: Bundle())
    }
    init {
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry
    override fun getLifecycle(): Lifecycle = lifecycle

    fun save() = Bundle().apply {
        controller.performSave(this)
    }
}
