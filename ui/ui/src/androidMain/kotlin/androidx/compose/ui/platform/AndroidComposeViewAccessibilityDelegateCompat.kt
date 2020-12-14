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

import android.content.Context
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
import android.view.accessibility.AccessibilityNodeProvider
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.collection.ArraySet
import androidx.collection.SparseArrayCompat
import androidx.compose.ui.R
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.CustomActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.findChildById
import androidx.compose.ui.semantics.getAllSemanticsNodesToMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.length
import androidx.compose.ui.text.platform.toAccessibilitySpannableString
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

private fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
    var currentParent = this.parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

@OptIn(InternalTextApi::class)
internal class AndroidComposeViewAccessibilityDelegateCompat(val view: AndroidComposeView) :
    AccessibilityDelegateCompat() {
    companion object {
        /** Virtual node identifier value for invalid nodes. */
        const val InvalidId = Integer.MIN_VALUE
        const val ClassName = "android.view.View"
        const val LogTag = "AccessibilityDelegate"
        /**
         * Intent size limitations prevent sending over a megabyte of data. Limit
         * text length to 100K characters - 200KB.
         */
        const val ParcelSafeTextLength = 100000
        /**
         * The undefined cursor position.
         */
        const val AccessibilityCursorPositionUndefined = -1
        // 20 is taken from AbsSeekbar.java.
        const val AccessibilitySliderStepsCount = 20
        /**
         * Delay before dispatching a recurring accessibility event in milliseconds.
         * This delay guarantees that a recurring event will be send at most once
         * during the [SendRecurringAccessibilityEventsIntervalMillis] time
         * frame.
         */
        const val SendRecurringAccessibilityEventsIntervalMillis: Long = 100
        private val AccessibilityActionsResourceIds = intArrayOf(
            R.id.accessibility_custom_action_0,
            R.id.accessibility_custom_action_1,
            R.id.accessibility_custom_action_2,
            R.id.accessibility_custom_action_3,
            R.id.accessibility_custom_action_4,
            R.id.accessibility_custom_action_5,
            R.id.accessibility_custom_action_6,
            R.id.accessibility_custom_action_7,
            R.id.accessibility_custom_action_8,
            R.id.accessibility_custom_action_9,
            R.id.accessibility_custom_action_10,
            R.id.accessibility_custom_action_11,
            R.id.accessibility_custom_action_12,
            R.id.accessibility_custom_action_13,
            R.id.accessibility_custom_action_14,
            R.id.accessibility_custom_action_15,
            R.id.accessibility_custom_action_16,
            R.id.accessibility_custom_action_17,
            R.id.accessibility_custom_action_18,
            R.id.accessibility_custom_action_19,
            R.id.accessibility_custom_action_20,
            R.id.accessibility_custom_action_21,
            R.id.accessibility_custom_action_22,
            R.id.accessibility_custom_action_23,
            R.id.accessibility_custom_action_24,
            R.id.accessibility_custom_action_25,
            R.id.accessibility_custom_action_26,
            R.id.accessibility_custom_action_27,
            R.id.accessibility_custom_action_28,
            R.id.accessibility_custom_action_29,
            R.id.accessibility_custom_action_30,
            R.id.accessibility_custom_action_31
        )
    }
    /** Virtual view id for the currently hovered logical item. */
    private var hoveredVirtualViewId = InvalidId
    private val accessibilityManager: AccessibilityManager =
        view.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    internal var accessibilityForceEnabledForTesting = false
    private val isAccessibilityEnabled
        get() = accessibilityForceEnabledForTesting ||
            accessibilityManager.isEnabled &&
            accessibilityManager.isTouchExplorationEnabled
    private val handler = Handler(Looper.getMainLooper())
    private var nodeProvider: AccessibilityNodeProviderCompat =
        AccessibilityNodeProviderCompat(MyNodeProvider())
    private var focusedVirtualViewId = InvalidId
    // For actionIdToId and labelToActionId, the keys are the virtualViewIds. The value of
    // actionIdToLabel holds assigned custom action id to custom action label mapping. The
    // value of labelToActionId holds custom action label to assigned custom action id mapping.
    private var actionIdToLabel = SparseArrayCompat<SparseArrayCompat<CharSequence>>()
    private var labelToActionId = SparseArrayCompat<Map<CharSequence, Int>>()
    private var accessibilityCursorPosition = AccessibilityCursorPositionUndefined
    private val subtreeChangedLayoutNodes = ArraySet<LayoutNode>()
    private val boundsUpdateChannel = Channel<Unit>(Channel.CONFLATED)

    @VisibleForTesting
    internal class SemanticsNodeCopy(
        semanticsNode: SemanticsNode
    ) {
        val config = semanticsNode.config
        val children: MutableSet<Int> = mutableSetOf()

        init {
            semanticsNode.children.fastForEach { child ->
                children.add(child.id)
            }
        }
    }

    @VisibleForTesting
    internal var semanticsNodes: MutableMap<Int, SemanticsNodeCopy> = mutableMapOf()
    private var semanticsRoot = SemanticsNodeCopy(view.semanticsOwner.rootSemanticsNode)
    private var checkingForSemanticsChanges = false

    init {
        // Remove callbacks that rely on view being attached to a window when we become detached.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {}
            override fun onViewDetachedFromWindow(view: View) {
                handler.removeCallbacks(semanticsChangeChecker)
            }
        })
    }

    private fun createNodeInfo(virtualViewId: Int): AccessibilityNodeInfo {
        val info: AccessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.obtain()
        val semanticsNode: SemanticsNode?
        if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            info.setSource(view)
            semanticsNode = view.semanticsOwner.rootSemanticsNode
            info.setParent(ViewCompat.getParentForAccessibility(view) as? View)
        } else {
            semanticsNode = view.semanticsOwner.rootSemanticsNode.findChildById(virtualViewId)
            if (semanticsNode == null) {
                // throw IllegalStateException("Semantics node $virtualViewId is not attached")
                return info.unwrap()
            }
            info.setSource(view, semanticsNode.id)
            if (semanticsNode.parent != null) {
                var parentId = semanticsNode.parent!!.id
                if (parentId == view.semanticsOwner.rootSemanticsNode.id) {
                    parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                }
                info.setParent(view, parentId)
            } else {
                throw IllegalStateException("semanticsNode $virtualViewId has null parent")
            }
        }

        populateAccessibilityNodeInfoProperties(virtualViewId, info, semanticsNode)

        return info.unwrap()
    }

    @VisibleForTesting
    @OptIn(ExperimentalComposeUiApi::class)
    fun populateAccessibilityNodeInfoProperties(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        semanticsNode: SemanticsNode
    ) {
        info.className = ClassName
        semanticsNode.config.getOrNull(SemanticsProperties.Role)?.let {
            when (it) {
                Role.Button -> info.className = "android.widget.Button"
                Role.Checkbox -> info.className = "android.widget.CheckBox"
                Role.Switch -> info.className = "android.widget.Switch"
                Role.RadioButton -> info.className = "android.widget.RadioButton"
                Role.Tab -> info.roleDescription = AccessibilityRoleDescriptions.Tab
            }
        }
        info.packageName = view.context.packageName
        try {
            info.setBoundsInScreen(
                android.graphics.Rect(
                    semanticsNode.globalBounds.left.toInt(),
                    semanticsNode.globalBounds.top.toInt(),
                    semanticsNode.globalBounds.right.toInt(),
                    semanticsNode.globalBounds.bottom.toInt()
                )
            )
        } catch (e: IllegalStateException) {
            // We may get "Asking for measurement result of unmeasured layout modifier" error.
            // TODO(b/153198816): check whether we still get this exception when R is in.
            info.setBoundsInScreen(android.graphics.Rect())
        }

        for (child in semanticsNode.children) {
            info.addChild(view, child.id)
        }

        // Manage internal accessibility focus state.
        if (focusedVirtualViewId == virtualViewId) {
            info.isAccessibilityFocused = true
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_CLEAR_ACCESSIBILITY_FOCUS
            )
        } else {
            info.isAccessibilityFocused = false
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat
                    .ACTION_ACCESSIBILITY_FOCUS
            )
        }

        // TODO: we need a AnnotedString to CharSequence conversion function
        info.text = trimToSize(
            semanticsNode.config.getOrNull(SemanticsProperties.Text)
                ?.toAccessibilitySpannableString(density = view.density, view.fontLoader),
            ParcelSafeTextLength
        )
        info.stateDescription =
            semanticsNode.config.getOrNull(SemanticsProperties.StateDescription)
        info.contentDescription =
            semanticsNode.config.getOrNull(SemanticsProperties.ContentDescription)
        semanticsNode.config.getOrNull(SemanticsProperties.Heading)?.let {
            info.isHeading = true
        }
        // Note editable is not added to semantics properties api.
        info.isEditable = semanticsNode.config.contains(SemanticsActions.SetText)
        info.isEnabled = semanticsNode.enabled()
        info.isFocusable = semanticsNode.config.contains(SemanticsProperties.Focused)
        if (info.isFocusable) {
            info.isFocused = semanticsNode.config[SemanticsProperties.Focused]
        }
        info.isVisibleToUser =
            (semanticsNode.config.getOrNull(SemanticsProperties.InvisibleToUser) == null)
        info.isClickable = false
        semanticsNode.config.getOrNull(SemanticsActions.OnClick)?.let {
            info.isClickable = true
            if (semanticsNode.enabled()) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        it.label
                    )
                )
            }
        }
        info.isLongClickable = false
        semanticsNode.config.getOrNull(SemanticsActions.OnLongClick)?.let {
            info.isLongClickable = true
            if (semanticsNode.enabled()) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                        it.label
                    )
                )
            }
        }

        semanticsNode.config.getOrNull(SemanticsActions.SetText)?.let {
            info.className = "android.widget.EditText"
        }
        // The config will contain this action only if there is a text selection at the moment.
        semanticsNode.config.getOrNull(SemanticsActions.CopyText)?.let {
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_COPY,
                    it.label
                )
            )
        }
        if (semanticsNode.enabled()) {
            semanticsNode.config.getOrNull(SemanticsActions.SetText)?.let {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
                        it.label
                    )
                )
            }
            semanticsNode.config.getOrNull(SemanticsActions.SetSelection)?.let {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                        it.label
                    )
                )
            }

            // The config will contain this action only if there is a text selection at the moment.
            semanticsNode.config.getOrNull(SemanticsActions.CutText)?.let {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CUT,
                        it.label
                    )
                )
            }

            // The config will contain the action anyway, therefore we check the clipboard text to
            // decide whether to add the action to the node or not.
            semanticsNode.config.getOrNull(SemanticsActions.PasteText)?.let {
                if (info.isFocused && view.clipboardManager.hasText()) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_PASTE,
                            it.label
                        )
                    )
                }
            }
        }

        val text = getIterableTextForAccessibility(semanticsNode)
        if (!text.isNullOrEmpty()) {
            info.setTextSelection(
                getAccessibilitySelectionStart(semanticsNode),
                getAccessibilitySelectionEnd(semanticsNode)
            )
            if (semanticsNode.config.getOrNull(SemanticsActions.SetSelection) == null ||
                !semanticsNode.enabled()
            ) {
                // Note this is the default set selection action provided by the framework.
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SET_SELECTION)
            }
            info.addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
            info.addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)
            info.movementGranularities =
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
            // We only traverse the text when contentDescription is not set.
            if (info.contentDescription.isNullOrEmpty() &&
                semanticsNode.config.contains(SemanticsActions.GetTextLayoutResult)
            ) {
                info.movementGranularities = info.movementGranularities or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !info.text.isNullOrEmpty() &&
            semanticsNode.config.contains(SemanticsActions.GetTextLayoutResult)
        ) {
            info.unwrap().availableExtraData = listOf(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        }

        val rangeInfo =
            semanticsNode.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        if (rangeInfo != null) {
            if (semanticsNode.config.contains(SemanticsActions.SetProgress)) {
                info.className = "android.widget.SeekBar"
            } else {
                info.className = "android.widget.ProgressBar"
            }
            info.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
                rangeInfo.range.start,
                rangeInfo.range.endInclusive,
                rangeInfo.current
            )
            if (semanticsNode.config.contains(SemanticsActions.SetProgress) &&
                semanticsNode.enabled()
            ) {
                if (rangeInfo.current <
                    rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                )
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD
                    )
                if (rangeInfo.current >
                    rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                )
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD
                    )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Api24Impl.addSetProgressAction(info, semanticsNode)
        }

        val xScrollState =
            semanticsNode.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        val scrollAction = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)
        if (xScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work.
            info.className = "android.widget.HorizontalScrollView"
            if (xScrollState.maxValue > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled() && xScrollState.value < xScrollState.maxValue) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD
                )
                if (!xScrollState.reverseScrolling) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                    )
                } else {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT
                    )
                }
            }
            if (semanticsNode.enabled() && xScrollState.value > 0f) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD
                )
                if (!xScrollState.reverseScrolling) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT
                    )
                } else {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                    )
                }
            }
        }
        val yScrollState =
            semanticsNode.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (yScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work.
            info.className = "android.widget.ScrollView"
            if (yScrollState.maxValue > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled() && yScrollState.value < yScrollState.maxValue) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD
                )
                if (!yScrollState.reverseScrolling) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN
                    )
                } else {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP
                    )
                }
            }
            if (semanticsNode.enabled() && yScrollState.value > 0f) {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD
                )
                if (!yScrollState.reverseScrolling) {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP
                    )
                } else {
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN
                    )
                }
            }
        }

        if (semanticsNode.enabled()) {
            semanticsNode.config.getOrNull(SemanticsActions.Dismiss)?.let {
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_DISMISS,
                        it.label
                    )
                )
            }

            if (semanticsNode.config.contains(CustomActions)) {
                val customActions = semanticsNode.config[CustomActions]
                if (customActions.size >= AccessibilityActionsResourceIds.size) {
                    throw IllegalStateException(
                        "Can't have more than " +
                            "${AccessibilityActionsResourceIds.size} custom actions for one widget"
                    )
                }
                val currentActionIdToLabel = SparseArrayCompat<CharSequence>()
                val currentLabelToActionId = mutableMapOf<CharSequence, Int>()
                // If this virtual node had custom action id assignment before, we try to keep the id
                // unchanged for the same action (identified by action label). This way, we can
                // minimize the influence of custom action change between custom actions are
                // presented to the user and actually performed.
                if (labelToActionId.containsKey(virtualViewId)) {
                    val oldLabelToActionId = labelToActionId[virtualViewId]
                    val availableIds = AccessibilityActionsResourceIds.toMutableList()
                    val unassignedActions = mutableListOf<CustomAccessibilityAction>()
                    for (action in customActions) {
                        if (oldLabelToActionId!!.contains(action.label)) {
                            val actionId = oldLabelToActionId[action.label]
                            currentActionIdToLabel.put(actionId!!, action.label)
                            currentLabelToActionId[action.label] = actionId
                            availableIds.remove(actionId)
                            info.addAction(
                                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    actionId, action.label
                                )
                            )
                        } else {
                            unassignedActions.add(action)
                        }
                    }
                    for ((index, action) in unassignedActions.withIndex()) {
                        val actionId = availableIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                actionId, action.label
                            )
                        )
                    }
                } else {
                    for ((index, action) in customActions.withIndex()) {
                        val actionId = AccessibilityActionsResourceIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                actionId, action.label
                            )
                        )
                    }
                }
                actionIdToLabel.put(virtualViewId, currentActionIdToLabel)
                labelToActionId.put(virtualViewId, currentLabelToActionId)
            }
        }
    }

    /**
     * Returns whether this virtual view is accessibility focused.
     *
     * @return True if the view is accessibility focused.
     */
    private fun isAccessibilityFocused(virtualViewId: Int): Boolean {
        return (focusedVirtualViewId == virtualViewId)
    }

    /**
     * Attempts to give accessibility focus to a virtual view.
     * <p>
     * A virtual view will not actually take focus if
     * {@link AccessibilityManager#isEnabled()} returns false,
     * {@link AccessibilityManager#isTouchExplorationEnabled()} returns false,
     * or the view already has accessibility focus.
     *
     * @param virtualViewId The id of the virtual view on which to place
     *            accessibility focus.
     * @return Whether this virtual view actually took accessibility focus.
     */
    private fun requestAccessibilityFocus(virtualViewId: Int): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }
        // TODO: Check virtual view visibility.
        if (!isAccessibilityFocused(virtualViewId)) {
            // Clear focus from the previously focused view, if applicable.
            if (focusedVirtualViewId != InvalidId) {
                sendEventForVirtualView(
                    focusedVirtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
                    null,
                    null
                )
            }

            // Set focus on the new view.
            focusedVirtualViewId = virtualViewId

            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
                null,
                null
            )
            return true
        }
        return false
    }

    /**
     * Populates an event of the specified type with information about an item
     * and attempts to send it up through the view hierarchy.
     * <p>
     * You should call this method after performing a user action that normally
     * fires an accessibility event, such as clicking on an item.
     *
     * <pre>public performItemClick(T item) {
     *   ...
     *   sendEventForVirtualView(item.id, AccessibilityEvent.TYPE_VIEW_CLICKED)
     * }
     * </pre>
     *
     * @param virtualViewId The virtual view id for which to send an event.
     * @param eventType The type of event to send.
     * @param contentChangeType The contentChangeType of this event.
     * @param contentDescription Content description of this event.
     * @return true if the event was sent successfully.
     */
    private fun sendEventForVirtualView(
        virtualViewId: Int,
        eventType: Int,
        contentChangeType: Int? = null,
        contentDescription: CharSequence? = null
    ): Boolean {
        if (virtualViewId == InvalidId || !isAccessibilityEnabled) {
            return false
        }

        val parent: ViewParent = view.parent

        val event: AccessibilityEvent = createEvent(virtualViewId, eventType)
        if (contentChangeType != null) {
            event.contentChangeTypes = contentChangeType
        }
        if (contentDescription != null) {
            event.contentDescription = contentDescription
        }

        return parent.requestSendAccessibilityEvent(view, event)
    }

    /**
     * Send an accessibility event.
     *
     * @param event The accessibility event to send.
     * @return true if the event was sent successfully.
     */
    private fun sendEvent(event: AccessibilityEvent): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }

        return view.parent.requestSendAccessibilityEvent(view, event)
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} populated with
     * information about the specified item.
     *
     * @param virtualViewId The virtual view id for the item for which to
     *            construct an event.
     * @param eventType The type of event to construct.
     * @return An {@link AccessibilityEvent} populated with information about
     *         the specified item.
     */
    @VisibleForTesting
    internal fun createEvent(virtualViewId: Int, eventType: Int): AccessibilityEvent {
        val event: AccessibilityEvent = AccessibilityEvent.obtain(eventType)
        event.isEnabled = true
        event.className = ClassName

        // Don't allow the client to override these properties.
        event.packageName = view.context.packageName
        event.setSource(view, virtualViewId)

        return event
    }

    /**
     * Attempts to clear accessibility focus from a virtual view.
     *
     * @param virtualViewId The id of the virtual view from which to clear
     *            accessibility focus.
     * @return Whether this virtual view actually cleared accessibility focus.
     */
    private fun clearAccessibilityFocus(virtualViewId: Int): Boolean {
        if (isAccessibilityFocused(virtualViewId)) {
            focusedVirtualViewId = InvalidId
            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
                null,
                null
            )
            return true
        }
        return false
    }

    private fun performActionHelper(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        val node: SemanticsNode =
            if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
                view.semanticsOwner.rootSemanticsNode
            } else {
                view.semanticsOwner.rootSemanticsNode.findChildById(virtualViewId) ?: return false
            }

        // Actions can be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS ->
                return requestAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS ->
                return clearAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
            AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY -> {
                if (arguments != null) {
                    val granularity = arguments.getInt(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
                    )
                    val extendSelection = arguments.getBoolean(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
                    )
                    return traverseAtGranularity(
                        node, granularity,
                        action == AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        extendSelection
                    )
                }
                return false
            }
            AccessibilityNodeInfoCompat.ACTION_SET_SELECTION -> {
                val start = arguments?.getInt(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, -1
                ) ?: -1
                val end = arguments?.getInt(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, -1
                ) ?: -1
                // Note: This is a little different from current android framework implementation.
                val success = setAccessibilitySelection(node, start, end, false)
                // Text selection changed event already updates the cache. so this may not be
                // necessary.
                if (success) {
                    sendEventForVirtualView(
                        semanticsNodeIdToAccessibilityVirtualNodeId(node.id),
                        AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                    )
                }
                return success
            }
            AccessibilityNodeInfoCompat.ACTION_COPY -> {
                return node.config.getOrNull(SemanticsActions.CopyText)?.let {
                    it.action()
                } ?: false
            }
        }

        if (!node.enabled()) {
            return false
        }

        // Actions can't be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                return if (node.config.contains(SemanticsActions.OnClick)) {
                    node.config[SemanticsActions.OnClick].action()
                } else {
                    false
                }
            }
            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                return if (node.config.contains(SemanticsActions.OnLongClick)) {
                    node.config[SemanticsActions.OnLongClick].action()
                } else {
                    false
                }
            }
            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD,
            android.R.id.accessibilityActionScrollDown,
            android.R.id.accessibilityActionScrollUp,
            android.R.id.accessibilityActionScrollRight,
            android.R.id.accessibilityActionScrollLeft -> {
                if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD ||
                    action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
                ) {
                    val rangeInfo =
                        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
                    val setProgressAction = node.config.getOrNull(SemanticsActions.SetProgress)
                    if (rangeInfo != null && setProgressAction != null) {
                        val max = rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                        val min = rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                        var increment = if (rangeInfo.steps > 0) {
                            (max - min) / (rangeInfo.steps + 1)
                        } else {
                            (max - min) / AccessibilitySliderStepsCount
                        }
                        if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                            increment = -increment
                        }
                        return setProgressAction.action(rangeInfo.current + increment)
                    }
                }

                val scrollAction = node.config.getOrNull(SemanticsActions.ScrollBy) ?: return false
                val xScrollState =
                    node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
                if (xScrollState != null) {
                    if ((
                        (
                            !xScrollState.reverseScrolling &&
                                action == android.R.id.accessibilityActionScrollRight
                            ) ||
                            (
                                xScrollState.reverseScrolling &&
                                    action == android.R.id.accessibilityActionScrollLeft
                                ) ||
                            (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
                        ) &&
                        xScrollState.value < xScrollState.maxValue
                    ) {
                        return scrollAction.action(
                            node.globalBounds.right - node.globalBounds.left,
                            0f
                        )
                    }
                    if ((
                        (
                            xScrollState.reverseScrolling &&
                                action == android.R.id.accessibilityActionScrollRight
                            ) ||
                            (
                                !xScrollState.reverseScrolling &&
                                    action == android.R.id.accessibilityActionScrollLeft
                                ) ||
                            (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
                        ) &&
                        xScrollState.value > 0
                    ) {
                        return scrollAction.action(
                            -(node.globalBounds.right - node.globalBounds.left),
                            0f
                        )
                    }
                }
                val yScrollState =
                    node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                if (yScrollState != null) {
                    if ((
                        (
                            !yScrollState.reverseScrolling &&
                                action == android.R.id.accessibilityActionScrollDown
                            ) ||
                            (
                                yScrollState.reverseScrolling &&
                                    action == android.R.id.accessibilityActionScrollUp
                                ) ||
                            (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
                        ) &&
                        yScrollState.value < yScrollState.maxValue
                    ) {
                        return scrollAction.action(
                            0f,
                            node.globalBounds.bottom - node.globalBounds.top
                        )
                    }
                    if ((
                        (
                            yScrollState.reverseScrolling &&
                                action == android.R.id.accessibilityActionScrollDown
                            ) ||
                            (
                                !yScrollState.reverseScrolling &&
                                    action == android.R.id.accessibilityActionScrollUp
                                ) ||
                            (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
                        ) &&
                        yScrollState.value > 0
                    ) {
                        return scrollAction.action(
                            0f,
                            -(node.globalBounds.bottom - node.globalBounds.top)
                        )
                    }
                }
                return false
            }
            android.R.id.accessibilityActionSetProgress -> {
                if (arguments == null || !arguments.containsKey(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE
                    )
                ) {
                    return false
                }
                return if (node.config.contains(SemanticsActions.SetProgress)) {
                    node.config[SemanticsActions.SetProgress].action(
                        arguments.getFloat(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE
                        )
                    )
                } else {
                    false
                }
            }
            AccessibilityNodeInfoCompat.ACTION_SET_TEXT -> {
                val text = arguments?.getString(
                    AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE
                )
                return node.config.getOrNull(SemanticsActions.SetText)?.let {
                    it.action(AnnotatedString(text ?: ""))
                } ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_PASTE -> {
                return node.config.getOrNull(SemanticsActions.PasteText)?.let {
                    it.action()
                } ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_CUT -> {
                return node.config.getOrNull(SemanticsActions.CutText)?.let {
                    it.action()
                } ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_DISMISS -> {
                return node.config.getOrNull(SemanticsActions.Dismiss)?.let {
                    it.action()
                } ?: false
            }
            // TODO: handling for other system actions
            else -> {
                val label = actionIdToLabel[virtualViewId]?.get(action) ?: return false
                val customActions = node.config.getOrNull(CustomActions) ?: return false
                for (customAction in customActions) {
                    if (customAction.label == label) {
                        return customAction.action()
                    }
                }
                return false
            }
        }
    }

    private fun addExtraDataToAccessibilityNodeInfoHelper(
        virtualViewId: Int,
        info: AccessibilityNodeInfo,
        extraDataKey: String,
        arguments: Bundle?
    ) {
        val node: SemanticsNode =
            if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
                view.semanticsOwner.rootSemanticsNode
            } else {
                view.semanticsOwner.rootSemanticsNode.findChildById(virtualViewId) ?: return
            }
        // TODO(b/157474582): This only works for single text/text field
        if (node.config.contains(SemanticsProperties.Text) &&
            node.config.contains(SemanticsActions.GetTextLayoutResult) &&
            arguments != null && extraDataKey == EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
        ) {
            val positionInfoStartIndex = arguments.getInt(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, -1
            )
            val positionInfoLength = arguments.getInt(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, -1
            )
            if ((positionInfoLength <= 0) || (positionInfoStartIndex < 0) ||
                (positionInfoStartIndex >= node.config[SemanticsProperties.Text].length)
            ) {
                Log.e(LogTag, "Invalid arguments for accessibility character locations")
                return
            }
            val textLayoutResults = mutableListOf<TextLayoutResult>()
            // Note now it only works for single Text/TextField until we fix the merging issue.
            val textLayoutResult: TextLayoutResult
            if (node.config[SemanticsActions.GetTextLayoutResult].action(textLayoutResults)) {
                textLayoutResult = textLayoutResults[0]
            } else {
                return
            }
            val boundingRects = mutableListOf<RectF?>()
            val textNode: SemanticsNode? = node.findNonEmptyTextChild()
            for (i in 0 until positionInfoLength) {
                val bounds = textLayoutResult.getBoundingBox(positionInfoStartIndex + i)
                val screenBounds: Rect?
                // Only the visible/partial visible locations are used.
                if (textNode != null) {
                    screenBounds = toScreenCoords(textNode, bounds)
                } else {
                    screenBounds = bounds
                }
                if (screenBounds == null) {
                    boundingRects.add(null)
                } else {
                    boundingRects.add(
                        RectF(
                            screenBounds.left,
                            screenBounds.top,
                            screenBounds.right,
                            screenBounds.bottom
                        )
                    )
                }
            }
            info.extras.putParcelableArray(extraDataKey, boundingRects.toTypedArray())
        }
    }

    private fun toScreenCoords(textNode: SemanticsNode, bounds: Rect): Rect? {
        val screenBounds = bounds.translate(textNode.globalPosition)
        val globalBounds = textNode.globalBounds
        if (screenBounds.overlaps(globalBounds)) {
            return screenBounds.intersect(globalBounds)
        }
        return null
    }

    // TODO: this only works for single text/text field.
    private fun SemanticsNode.findNonEmptyTextChild(): SemanticsNode? {
        if (this.unmergedConfig.contains(SemanticsProperties.Text) &&
            this.unmergedConfig[SemanticsProperties.Text].length != 0
        ) {
            return this
        }
        unmergedChildren().fastForEach {
            val result = it.findNonEmptyTextChild()
            if (result != null) return result
        }
        return null
    }

    /**
     * Dispatches hover {@link android.view.MotionEvent}s to the virtual view hierarchy when
     * the Explore by Touch feature is enabled.
     * <p>
     * This method should be called by overriding
     * {@link View#dispatchHoverEvent}:
     *
     * <pre>&#64;Override
     * public boolean dispatchHoverEvent(MotionEvent event) {
     *   if (mHelper.dispatchHoverEvent(this, event) {
     *     return true;
     *   }
     *   return super.dispatchHoverEvent(event);
     * }
     * </pre>
     *
     * @param event The hover event to dispatch to the virtual view hierarchy.
     * @return Whether the hover event was handled.
     */
    fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (!isAccessibilityEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                val virtualViewId: Int = getVirtualViewAt(event.getX(), event.getY())
                updateHoveredVirtualView(virtualViewId)
                return (virtualViewId != InvalidId)
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                if (hoveredVirtualViewId != InvalidId) {
                    updateHoveredVirtualView(InvalidId)
                    return true
                }
                return false
            }
            else -> {
                return false
            }
        }
    }

    @VisibleForTesting
    internal fun getVirtualViewAt(x: Float, y: Float): Int {
        val node = view.semanticsOwner.rootSemanticsNode
        val id = findVirtualViewAt(
            x + node.globalBounds.left,
            y + node.globalBounds.top, node
        )
        if (id == node.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
    }

    // TODO(b/151729467): compose accessibility getVirtualViewAt needs to be more efficient
    private fun findVirtualViewAt(x: Float, y: Float, node: SemanticsNode): Int {
        val children = node.children
        for (i in children.size - 1 downTo 0) {
            val id = findVirtualViewAt(x, y, children[i])
            if (id != InvalidId) {
                return id
            }
        }

        if (node.globalBounds.left < x && node.globalBounds.right > x && node
            .globalBounds.top < y && node.globalBounds.bottom > y
        ) {
            return node.id
        }

        return InvalidId
    }

    /**
     * Sets the currently hovered item, sending hover accessibility events as
     * necessary to maintain the correct state.
     *
     * @param virtualViewId The virtual view id for the item currently being
     *            hovered, or {@link #InvalidId} if no item is hovered within
     *            the parent view.
     */
    private fun updateHoveredVirtualView(virtualViewId: Int) {
        if (hoveredVirtualViewId == virtualViewId) {
            return
        }

        val previousVirtualViewId: Int = hoveredVirtualViewId
        hoveredVirtualViewId = virtualViewId

        /*
        Stay consistent with framework behavior by sending ENTER/EXIT pairs
        in reverse order. This is accurate as of API 18.
        */
        sendEventForVirtualView(
            virtualViewId,
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
            null,
            null
        )
        sendEventForVirtualView(
            previousVirtualViewId,
            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT,
            null,
            null
        )
    }

    override fun getAccessibilityNodeProvider(host: View?): AccessibilityNodeProviderCompat {
        return nodeProvider
    }

    /**
     * Trims the text to [size] length. Returns the string as it is if the length is
     * smaller than [size]. If chars at [size] - 1 and [size] is a surrogate
     * pair, returns a CharSequence of length [size] - 1.
     *
     * @param size length of the result, should be greater than 0
     */
    private fun <T : CharSequence> trimToSize(text: T?, @IntRange(from = 1) size: Int): T? {
        require(size > 0)
        var len = size
        if (text.isNullOrEmpty() || text.length <= size) return text
        if (Character.isHighSurrogate(text[size - 1]) && Character.isLowSurrogate(text[size])) {
            len = size - 1
        }
        @Suppress("UNCHECKED_CAST")
        return text.subSequence(0, len) as T
    }

    // TODO (in a separate cl): Called when the SemanticsNode with id semanticsNodeId disappears.
    // fun clearNode(semanticsNodeId: Int) { // clear the actionIdToId and labelToActionId nodes }

    private val semanticsChangeChecker = Runnable {
        checkForSemanticsChanges()
        checkingForSemanticsChanges = false
    }

    internal fun onSemanticsChange() {
        if (isAccessibilityEnabled && !checkingForSemanticsChanges) {
            checkingForSemanticsChanges = true
            handler.post(semanticsChangeChecker)
        }
    }

    /**
     * This suspend function loops for the entire lifetime of the Compose instance: it consumes
     * recent layout changes and sends events to the accessibility framework in batches separated
     * by a 100ms delay.
     */
    suspend fun boundsUpdatesEventLoop() {
        try {
            val subtreeChangedSemanticsNodesIds = ArraySet<Int>()
            for (notification in boundsUpdateChannel) {
                if (isAccessibilityEnabled) {
                    for (i in subtreeChangedLayoutNodes.indices) {
                        sendSubtreeChangeAccessibilityEvents(
                            subtreeChangedLayoutNodes.valueAt(i)!!,
                            subtreeChangedSemanticsNodesIds
                        )
                    }
                    subtreeChangedSemanticsNodesIds.clear()
                }
                subtreeChangedLayoutNodes.clear()
                delay(SendRecurringAccessibilityEventsIntervalMillis)
            }
        } finally {
            subtreeChangedLayoutNodes.clear()
        }
    }

    internal fun onLayoutChange(layoutNode: LayoutNode) {
        if (!isAccessibilityEnabled) {
            return
        }

        // The layout change of a LayoutNode will also affect its children, so even if it doesn't
        // have semantics attached, we should process it.
        notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode)
    }

    private fun notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode: LayoutNode) {
        if (subtreeChangedLayoutNodes.add(layoutNode)) {
            boundsUpdateChannel.offer(Unit)
        }
    }

    private fun sendSubtreeChangeAccessibilityEvents(
        layoutNode: LayoutNode,
        subtreeChangedSemanticsNodesIds: ArraySet<Int>
    ) {
        // The node may be no longer available while we were waiting so check
        // again.
        if (!layoutNode.isAttached) {
            return
        }
        // When we finally send the event, make sure it is an accessibility-focusable node.
        var semanticsWrapper = layoutNode.outerSemantics
            ?: layoutNode.findClosestParentNode { it.outerSemantics != null }
                ?.outerSemantics ?: return
        if (!semanticsWrapper.collapsedSemanticsConfiguration().isMergingSemanticsOfDescendants) {
            layoutNode.findClosestParentNode {
                it.outerSemantics
                    ?.collapsedSemanticsConfiguration()
                    ?.isMergingSemanticsOfDescendants == true
            }?.outerSemantics?.let { semanticsWrapper = it }
        }
        val id = semanticsWrapper.modifier.id
        if (!subtreeChangedSemanticsNodesIds.add(id)) {
            return
        }

        sendEvent(
            createEvent(
                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ).also { it.contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE }
        )
    }

    private fun checkForSemanticsChanges() {
        val newSemanticsNodes = view.semanticsOwner.getAllSemanticsNodesToMap()

        // Structural change
        sendSemanticsStructureChangeEvents(view.semanticsOwner.rootSemanticsNode, semanticsRoot)

        // Property change
        sendSemanticsPropertyChangeEvents(newSemanticsNodes)

        // Update the cache
        semanticsNodes.clear()
        for (entry in newSemanticsNodes.entries) {
            semanticsNodes[entry.key] = SemanticsNodeCopy(entry.value)
        }
        semanticsRoot = SemanticsNodeCopy(view.semanticsOwner.rootSemanticsNode)
    }

    @VisibleForTesting
    internal fun sendSemanticsPropertyChangeEvents(newSemanticsNodes: Map<Int, SemanticsNode>) {
        for (id in newSemanticsNodes.keys) {
            // We do doing this search because the new configuration is set as a whole, so we
            // can't indicate which property is changed when setting the new configuration.
            val oldNode = semanticsNodes[id] ?: continue
            val newNode = newSemanticsNodes[id]
            var propertyChanged = false
            for (entry in newNode!!.config) {
                if (entry.value == oldNode.config.getOrNull(entry.key)) {
                    continue
                }
                when (entry.key) {
                    SemanticsProperties.StateDescription ->
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        )
                    SemanticsProperties.ContentDescription ->
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION,
                            entry.value as CharSequence
                        )
                    SemanticsProperties.Text -> {
                        // TODO(b/160184953) Add test for SemanticsProperty Text change event
                        if (newNode.config.contains(SemanticsActions.SetText)) {
                            val oldText = (
                                oldNode.config.getOrElse(
                                    SemanticsProperties.Text
                                ) { AnnotatedString("") }
                                ).text
                            val newText = (
                                newNode.config.getOrElse(
                                    SemanticsProperties.Text
                                ) { AnnotatedString("") }
                                ).text
                            var startCount = 0
                            // endCount records how many characters are the same from the end.
                            var endCount = 0
                            val oldTextLen = oldText.length
                            val newTextLen = newText.length
                            val minLength = oldTextLen.coerceAtMost(newTextLen)
                            while (startCount < minLength) {
                                if (oldText[startCount] != newText[startCount]) {
                                    break
                                }
                                startCount++
                            }
                            // abcdabcd vs
                            //     abcd
                            while (endCount < minLength - startCount) {
                                if (oldText[oldTextLen - 1 - endCount] !=
                                    newText[newTextLen - 1 - endCount]
                                ) {
                                    break
                                }
                                endCount++
                            }
                            val removedCount = oldTextLen - endCount - startCount
                            val addedCount = newTextLen - endCount - startCount
                            val textChangeEvent = createEvent(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                            )
                            textChangeEvent.fromIndex = startCount
                            textChangeEvent.removedCount = removedCount
                            textChangeEvent.addedCount = addedCount
                            textChangeEvent.beforeText = oldText
                            textChangeEvent.text.add(trimToSize(newText, ParcelSafeTextLength))
                            sendEvent(textChangeEvent)
                        } else {
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT,
                                null
                            )
                        }
                    }
                    // do we need to overwrite TextRange equals?
                    SemanticsProperties.TextSelectionRange -> {
                        val newText = (
                            newNode.config.getOrElse(
                                SemanticsProperties.Text
                            ) { AnnotatedString("") }
                            ).text
                        val event = createEvent(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
                        )
                        val textRange = newNode.config[SemanticsProperties.TextSelectionRange]
                        event.fromIndex = textRange.start
                        event.toIndex = textRange.end
                        event.itemCount = newText.length
                        event.text.add(trimToSize(newText, ParcelSafeTextLength))
                        sendEvent(event)
                    }
                    SemanticsProperties.HorizontalScrollAxisRange,
                    SemanticsProperties.VerticalScrollAxisRange -> {
                        // TODO(yingleiw): Add throttling for scroll/state events.
                        val newXState = newNode.config.getOrNull(
                            SemanticsProperties.HorizontalScrollAxisRange
                        )
                        val oldXState = oldNode.config.getOrNull(
                            SemanticsProperties.HorizontalScrollAxisRange
                        )
                        val newYState = newNode.config.getOrNull(
                            SemanticsProperties.VerticalScrollAxisRange
                        )
                        val oldYState = oldNode.config.getOrNull(
                            SemanticsProperties.VerticalScrollAxisRange
                        )
                        notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                        val deltaX = if (newXState != null && oldXState != null) {
                            newXState.value - oldXState.value
                        } else {
                            0f
                        }
                        val deltaY = if (newYState != null && oldYState != null) {
                            newYState.value - oldYState.value
                        } else {
                            0f
                        }
                        if (deltaX != 0f || deltaY != 0f) {
                            val event = createEvent(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_VIEW_SCROLLED
                            )
                            if (newXState != null) {
                                event.scrollX = newXState.value.toInt()
                                event.maxScrollX = newXState.maxValue.toInt()
                            }
                            if (newYState != null) {
                                event.scrollY = newYState.value.toInt()
                                event.maxScrollY = newYState.maxValue.toInt()
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                Api28Impl.setScrollEventDelta(event, deltaX.toInt(), deltaY.toInt())
                            }
                            sendEvent(event)
                        }
                    }
                    SemanticsProperties.Focused -> {
                        if (entry.value as Boolean) {
                            sendEvent(
                                createEvent(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                                    AccessibilityEvent.TYPE_VIEW_FOCUSED
                                )
                            )
                        }
                        // In View.java this window event is sent for unfocused view. But we send
                        // it for focused too so that TalkBack invalidates its cache. Otherwise
                        // PasteText edit option is not displayed properly on some OS versions.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                        )
                    }
                    // TODO(b/151840490) send the correct events for certain properties, like pane
                    //  title, view selected.
                    else -> {
                        propertyChanged = true
                    }
                }
            }

            if (!propertyChanged) {
                propertyChanged = newNode.propertiesDeleted(oldNode)
            }
            if (propertyChanged) {
                // TODO(b/176105563): throttle the window content change events and merge different
                //  sub types. We can use the subtreeChangedLayoutNodes with sub types.
                sendEventForVirtualView(
                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
                )
            }
        }
    }

    private fun sendSemanticsStructureChangeEvents(
        newNode: SemanticsNode,
        oldNode: SemanticsNodeCopy
    ) {
        val newChildren: MutableSet<Int> = mutableSetOf()

        // If any child is added, clear the subtree rooted at this node and return.
        newNode.children.fastForEach { child ->
            if (!oldNode.children.contains(child.id)) {
                notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                return
            }
            newChildren.add(child.id)
        }

        // If any child is deleted, clear the subtree rooted at this node and return.
        for (child in oldNode.children) {
            if (!newChildren.contains(child)) {
                notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                return
            }
        }

        newNode.children.fastForEach { child ->
            sendSemanticsStructureChangeEvents(child, semanticsNodes[child.id]!!)
        }
    }

    private fun semanticsNodeIdToAccessibilityVirtualNodeId(id: Int): Int {
        if (id == view.semanticsOwner.rootSemanticsNode.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
    }

    private fun traverseAtGranularity(
        node: SemanticsNode,
        granularity: Int,
        forward: Boolean,
        extendSelection: Boolean
    ): Boolean {
        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return false
        }
        val iterator = getIteratorForGranularity(node, granularity) ?: return false
        var current = getAccessibilitySelectionEnd(node)
        if (current == AccessibilityCursorPositionUndefined) {
            current = if (forward) 0 else text.length
        }
        val range = (if (forward) iterator.following(current) else iterator.preceding(current))
            ?: return false
        val segmentStart = range[0]
        val segmentEnd = range[1]
        var selectionStart: Int
        val selectionEnd: Int
        if (extendSelection && isAccessibilitySelectionExtendable(node)) {
            selectionStart = getAccessibilitySelectionStart(node)
            if (selectionStart == AccessibilityCursorPositionUndefined) {
                selectionStart = if (forward) segmentStart else segmentEnd
            }
            selectionEnd = if (forward) segmentEnd else segmentStart
        } else {
            selectionStart = if (forward) segmentEnd else segmentStart
            selectionEnd = selectionStart
        }
        setAccessibilitySelection(node, selectionStart, selectionEnd, true)
        val action =
            if (forward)
                AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            else AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
        sendViewTextTraversedAtGranularityEvent(node, action, granularity, segmentStart, segmentEnd)
        return true
    }

    private fun sendViewTextTraversedAtGranularityEvent(
        node: SemanticsNode,
        action: Int,
        granularity: Int,
        fromIndex: Int,
        toIndex: Int
    ) {
        val event = createEvent(
            node.id,
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
        )
        event.fromIndex = fromIndex
        event.toIndex = toIndex
        event.action = action
        event.movementGranularity = granularity
        event.text.add(getIterableTextForAccessibility(node))
        sendEvent(event)
    }

    private fun setAccessibilitySelection(
        node: SemanticsNode,
        start: Int,
        end: Int,
        traversalMode: Boolean
    ): Boolean {
        // Any widget which has custom action_set_selection needs to provide cursor
        // positions, so events will be sent when cursor position change.
        // When the node is disabled, only the default/virtual set selection can performed.
        if (node.config.contains(SemanticsActions.SetSelection) && node.enabled()) {
            // Hide all selection controllers used for adjusting selection
            // since we are doing so explicitly by other means and these
            // controllers interact with how selection behaves. From TextView.java.
            return node.config[SemanticsActions.SetSelection].action(
                start,
                end,
                traversalMode
            )
        }
        if (start == end && end == accessibilityCursorPosition) {
            return false
        }
        if (getIterableTextForAccessibility(node) == null) {
            return false
        }
        accessibilityCursorPosition = if (start >= 0 && start == end &&
            end <= getIterableTextForAccessibility(node)!!.length
        ) {
            start
        } else {
            AccessibilityCursorPositionUndefined
        }
        sendEventForVirtualView(node.id, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED)
        return true
    }

    private fun getAccessibilitySelectionStart(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (!node.config.contains(SemanticsProperties.ContentDescription) &&
            node.config.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.config[SemanticsProperties.TextSelectionRange].start
        }
        return accessibilityCursorPosition
    }

    private fun getAccessibilitySelectionEnd(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (!node.config.contains(SemanticsProperties.ContentDescription) &&
            node.config.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.config[SemanticsProperties.TextSelectionRange].end
        }
        return accessibilityCursorPosition
    }

    private fun isAccessibilitySelectionExtendable(node: SemanticsNode): Boolean {
        // Currently only TextField is extendable. Static text may become extendable later.
        return !node.config.contains(SemanticsProperties.ContentDescription) &&
            node.config.contains(SemanticsProperties.Text)
    }

    private fun getIteratorForGranularity(
        node: SemanticsNode?,
        granularity: Int
    ): AccessibilityIterators.TextSegmentIterator? {
        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return null
        }
        // TODO(b/160190186) Make sure locale is right in AccessibilityIterators.
        val iterator: AccessibilityIterators.AbstractTextSegmentIterator
        @Suppress("DEPRECATION")
        when (granularity) {
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER -> {
                iterator = AccessibilityIterators.CharacterTextSegmentIterator.getInstance(
                    view.context.resources.configuration.locale
                )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD -> {
                iterator = AccessibilityIterators.WordTextSegmentIterator.getInstance(
                    view.context.resources.configuration.locale
                )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH -> {
                iterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance()
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE,
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE -> {
                // Line and page granularity are only for static text or text field.
                if (node == null || !node.config.contains(SemanticsProperties.Text) ||
                    !node.config.contains(SemanticsActions.GetTextLayoutResult)
                ) {
                    return null
                }
                // TODO(b/157474582): Note now it only works for single Text/TextField until we
                //  fix the merging issue.
                val textLayoutResults = mutableListOf<TextLayoutResult>()
                val textLayoutResult: TextLayoutResult
                if (node.config[SemanticsActions.GetTextLayoutResult].action(textLayoutResults)) {
                    textLayoutResult = textLayoutResults[0]
                } else {
                    return null
                }
                if (granularity == AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE) {
                    iterator = AccessibilityIterators.LineTextSegmentIterator.getInstance()
                    iterator.initialize(text, textLayoutResult)
                } else {
                    iterator = AccessibilityIterators.PageTextSegmentIterator.getInstance()
                    // TODO: the node should be text/textfield node instead of the current node.
                    iterator.initialize(text, textLayoutResult, node)
                }
            }
            else -> return null
        }
        return iterator
    }

    /**
     * Gets the text reported for accessibility purposes.
     *
     * @return The accessibility text.
     */
    private fun getIterableTextForAccessibility(node: SemanticsNode?): String? {
        if (node == null) {
            return null
        }
        // Note in android framework, TextView set this to its text. This is changed to
        // prioritize content description, even for Text.
        if (node.config.contains(SemanticsProperties.ContentDescription)) {
            return node.config[SemanticsProperties.ContentDescription]
        }
        if (node.config.contains(SemanticsProperties.Text)) {
            return node.config[SemanticsProperties.Text].text
        }
        return null
    }

    // TODO(b/160820721): use AccessibilityNodeProviderCompat instead of AccessibilityNodeProvider
    inner class MyNodeProvider : AccessibilityNodeProvider() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int):
            AccessibilityNodeInfo? {
                return createNodeInfo(virtualViewId)
            }

        override fun performAction(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            return performActionHelper(virtualViewId, action, arguments)
        }

        override fun addExtraDataToAccessibilityNodeInfo(
            virtualViewId: Int,
            info: AccessibilityNodeInfo,
            extraDataKey: String,
            arguments: Bundle?
        ) {
            addExtraDataToAccessibilityNodeInfoHelper(virtualViewId, info, extraDataKey, arguments)
        }
    }

    private class Api24Impl {
        @RequiresApi(Build.VERSION_CODES.N)
        companion object {
            fun addSetProgressAction(
                info: AccessibilityNodeInfoCompat,
                semanticsNode: SemanticsNode
            ) {
                if (semanticsNode.enabled()) {
                    semanticsNode.config.getOrNull(SemanticsActions.SetProgress)?.let {
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                android.R.id.accessibilityActionSetProgress,
                                it.label
                            )
                        )
                    }
                }
            }
        }
    }

    private class Api28Impl {
        @RequiresApi(Build.VERSION_CODES.P)
        companion object {
            fun setScrollEventDelta(event: AccessibilityEvent, deltaX: Int, deltaY: Int) {
                event.scrollDeltaX = deltaX
                event.scrollDeltaY = deltaY
            }
        }
    }
}

private fun SemanticsNode.enabled() = (config.getOrNull(SemanticsProperties.Disabled) == null)

private fun SemanticsNode.propertiesDeleted(
    oldNode: AndroidComposeViewAccessibilityDelegateCompat.SemanticsNodeCopy
): Boolean {
    for (entry in oldNode.config) {
        if (!config.contains(entry.key)) {
            return true
        }
    }
    return false
}
