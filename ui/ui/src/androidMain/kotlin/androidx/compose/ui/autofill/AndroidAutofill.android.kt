/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Build
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.util.fastMap

/**
 * Autofill implementation for Android.
 *
 * @param view The parent compose view.
 * @param autofillTree The autofill tree. This will be replaced by a semantic tree (b/138604305).
 */
@ExperimentalComposeUiApi
@RequiresApi(Build.VERSION_CODES.O)
internal class AndroidAutofill(val view: View, val autofillTree: AutofillTree) : Autofill {

    val autofillManager = view.context.getSystemService(AutofillManager::class.java)
        ?: error("Autofill service could not be located.")

    init { view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES }

    override fun requestAutofillForNode(autofillNode: AutofillNode) {
        // TODO(b/138731416): Find out what happens when notifyViewEntered() is called multiple times
        // before calling notifyViewExited().
        autofillManager.notifyViewEntered(
            view,
            autofillNode.id,
            autofillNode.boundingBox?.toAndroidRect()
                ?: error("requestAutofill called before onChildPositioned()")
        )
    }

    override fun cancelAutofillForNode(autofillNode: AutofillNode) {
        autofillManager.notifyViewExited(view, autofillNode.id)
    }
}

/**
 * Populates the view structure with autofill information.
 *
 * @param root A reference to the view structure of the parent compose view.
 *
 * This function populates the view structure using the information in the { AutofillTree }.
 */
@ExperimentalComposeUiApi
@RequiresApi(Build.VERSION_CODES.O)
internal fun AndroidAutofill.populateViewStructure(root: ViewStructure) {

    // Add child nodes. The function returns the index to the first item.
    var index = AutofillApi23Helper.addChildCount(root, autofillTree.children.count())

    for ((id, autofillNode) in autofillTree.children) {
        AutofillApi23Helper.newChild(root, index)?.also { child ->
            AutofillApi26Helper.setAutofillId(
                child,
                AutofillApi26Helper.getAutofillId(root)!!,
                id
            )
            AutofillApi23Helper.setId(child, id, view.context.packageName, null, null)
            AutofillApi26Helper.setAutofillType(child, View.AUTOFILL_TYPE_TEXT)
            AutofillApi26Helper.setAutofillHints(
                child,
                autofillNode.autofillTypes.fastMap { it.androidType }.toTypedArray()
            )

            if (autofillNode.boundingBox == null) {
                // Do we need an exception here? warning? silently ignore? If the boundingbox is
                // null, the autofill overlay will not be shown.
                Log.w(
                    "Autofill Warning",
                    """Bounding box not set.
                        Did you call perform autofillTree before the component was positioned? """
                )
            }
            autofillNode.boundingBox?.toAndroidRect()?.run {
                AutofillApi23Helper.setDimens(child, left, top, 0, 0, width(), height())
            }
        }
        index++
    }
}

/**
 * Triggers onFill() in response to a request from the autofill framework.
 */
@ExperimentalComposeUiApi
@RequiresApi(Build.VERSION_CODES.O)
internal fun AndroidAutofill.performAutofill(values: SparseArray<AutofillValue>) {
    for (index in 0 until values.size()) {
        val itemId = values.keyAt(index)
        val value = values[itemId]
        when {
            AutofillApi26Helper.isText(value) -> autofillTree.performAutofill(
                itemId,
                AutofillApi26Helper.textValue(value).toString()
            )
            AutofillApi26Helper.isDate(value) ->
                TODO("b/138604541: Add onFill() callback for date")
            AutofillApi26Helper.isList(value) ->
                TODO("b/138604541: Add onFill() callback for list")
            AutofillApi26Helper.isToggle(value) ->
                TODO("b/138604541:  Add onFill() callback for toggle")
        }
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(26)
internal object AutofillApi26Helper {
    @RequiresApi(26)
    @DoNotInline
    fun setAutofillId(structure: ViewStructure, parent: AutofillId, virtualId: Int) =
        structure.setAutofillId(parent, virtualId)

    @RequiresApi(26)
    @DoNotInline
    fun getAutofillId(structure: ViewStructure) = structure.autofillId

    @RequiresApi(26)
    @DoNotInline
    fun setAutofillType(structure: ViewStructure, type: Int) = structure.setAutofillType(type)

    @RequiresApi(26)
    @DoNotInline
    fun setAutofillHints(structure: ViewStructure, hints: Array<String>) =
        structure.setAutofillHints(hints)

    @RequiresApi(26)
    @DoNotInline
    fun isText(value: AutofillValue) = value.isText

    @RequiresApi(26)
    @DoNotInline
    fun isDate(value: AutofillValue) = value.isDate

    @RequiresApi(26)
    @DoNotInline
    fun isList(value: AutofillValue) = value.isList

    @RequiresApi(26)
    @DoNotInline
    fun isToggle(value: AutofillValue) = value.isToggle

    @RequiresApi(26)
    @DoNotInline
    fun textValue(value: AutofillValue): CharSequence = value.textValue
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(23)
internal object AutofillApi23Helper {
    @RequiresApi(23)
    @DoNotInline
    fun newChild(structure: ViewStructure, index: Int): ViewStructure? =
        structure.newChild(index)

    @RequiresApi(23)
    @DoNotInline
    fun addChildCount(structure: ViewStructure, num: Int) =
        structure.addChildCount(num)

    @RequiresApi(23)
    @DoNotInline
    fun setId(
        structure: ViewStructure,
        id: Int,
        packageName: String?,
        typeName: String?,
        entryName: String?
    ) = structure.setId(id, packageName, typeName, entryName)

    @RequiresApi(23)
    @DoNotInline
    fun setDimens(
        structure: ViewStructure,
        left: Int,
        top: Int,
        scrollX: Int,
        scrollY: Int,
        width: Int,
        height: Int
    ) = structure.setDimens(left, top, scrollX, scrollY, width, height)
}