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

package androidx.compose.ui.tooling.preview

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.TransitionModel
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Providers
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.AmbientFontLoader
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.tooling.Group
import androidx.compose.ui.tooling.Inspectable
import androidx.compose.ui.tooling.SlotTableRecord
import androidx.compose.ui.tooling.SourceLocation
import androidx.compose.ui.tooling.asTree
import androidx.compose.ui.tooling.preview.animation.PreviewAnimationClock
import androidx.compose.ui.unit.IntBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner

const val TOOLS_NS_URI = "http://schemas.android.com/tools"

/**
 * Class containing the minimum information needed by the Preview to map components to the
 * source code and render boundaries.
 *
 * @suppress
 */
data class ViewInfo(
    val fileName: String,
    val lineNumber: Int,
    val bounds: IntBounds,
    val location: SourceLocation?,
    val children: List<ViewInfo>
) {
    fun hasBounds(): Boolean = bounds.bottom != 0 && bounds.right != 0

    fun allChildren(): List<ViewInfo> =
        children + children.flatMap { it.allChildren() }

    override fun toString(): String =
        """($fileName:$lineNumber,
            |bounds=(top=${bounds.top}, left=${bounds.left},
            |location=${location?.let { "(${it.offset}L${it.length}" } ?: "<none>"}
            |bottom=${bounds.bottom}, right=${bounds.right}),
            |childrenCount=${children.size})""".trimMargin()
}

/**
 * View adapter that renders a `@Composable`. The `@Composable` is found by
 * reading the `tools:composableName` attribute that contains the FQN. Additional attributes can
 * be used to customize the behaviour of this view:
 *  - `tools:parameterProviderClass`: FQN of the [PreviewParameterProvider] to be instantiated by
 *  the [ComposeViewAdapter] that will be used as source for the `@Composable` parameters.
 *  - `tools:parameterProviderIndex`: The index within the [PreviewParameterProvider] of the
 *  value to be used in this particular instance.
 *  - `tools:paintBounds`: If true, the component boundaries will be painted. This is only meant
 *  for debugging purposes.
 *  - `tools:printViewInfos`: If true, the [ComposeViewAdapter] will log the tree of [ViewInfo]
 *  to logcat for debugging.
 *  - `tools:animationClockStartTime`: When set, the [AnimationClockAmbient] will provide a
 *  [PreviewAnimationClock] using this value as start time. The clock will control the animations
 *  in the [ComposeViewAdapter] context.
 *
 * @suppress
 */
@Suppress("unused")
internal class ComposeViewAdapter : FrameLayout {
    private val TAG = "ComposeViewAdapter"

    /**
     * [ComposeView] that will contain the [Composable] to preview.
     */
    private val composeView = ComposeView(context)

    /**
     * When enabled, generate and cache [ViewInfo] tree that can be inspected by the Preview
     * to map components to source code.
     */
    private var debugViewInfos = false

    /**
     * When enabled, paint the boundaries generated by layout nodes.
     */
    private var debugPaintBounds = false
    internal var viewInfos: List<ViewInfo> = emptyList()
    private val slotTableRecord = SlotTableRecord.create()

    /**
     * Simple function name of the Composable being previewed.
     */
    private var composableName = ""

    /**
     * Whether the current Composable has animations.
     */
    private var hasAnimations = false

    /**
     * Saved exception from the last composition. Since we can not handle the exception during the
     * composition, we save it and throw it during onLayout, this allows Studio to catch it and
     * display it to the user.
     */
    private val delayedException = AtomicReference<Throwable?>(null)

    /**
     * The [Composable] to be rendered in the preview. It is initialized when this adapter
     * is initialized.
     */
    private var previewComposition: @Composable () -> Unit = emptyContent()

    // Note: the call to emptyContent() below instead of a literal {} works around
    // https://youtrack.jetbrains.com/issue/KT-17467, which causes the compiler to emit classes
    // named `content` and `Content` (from the Content method's composable update scope)
    // which causes compilation problems on case-insensitive filesystems.
    @Suppress("RemoveExplicitTypeArguments")
    private val content = mutableStateOf<@Composable () -> Unit>(emptyContent())

    /**
     * When true, the composition will be immediately invalidated after being drawn. This will
     * force it to be recomposed on the next render. This is useful for live literals so the
     * whole composition happens again on the next render.
     */
    private var forceCompositionInvalidation = false

    /**
     * Callback invoked when onDraw has been called.
     */
    private var onDraw = {}

    private val debugBoundsPaint = Paint().apply {
        pathEffect = DashPathEffect(floatArrayOf(5f, 10f, 15f, 20f), 0f)
        style = Paint.Style.STROKE
        color = Color.Red.toArgb()
    }

    private var composition: Composition? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun walkTable(viewInfo: ViewInfo, indent: Int = 0) {
        Log.d(TAG, ("|  ".repeat(indent)) + "|-$viewInfo")
        viewInfo.children.forEach { walkTable(it, indent + 1) }
    }

    private val Group.fileName: String
        get() = location?.sourceFile ?: ""

    private val Group.lineNumber: Int
        get() = location?.lineNumber ?: -1

    /**
     * Returns true if this [Group] has no source position information
     */
    private fun Group.hasNullSourcePosition(): Boolean =
        fileName.isEmpty() && lineNumber == -1

    /**
     * Returns true if this [Group] has no source position information and no children
     */
    private fun Group.isNullGroup(): Boolean =
        hasNullSourcePosition() && children.isEmpty()

    private fun Group.toViewInfo(): ViewInfo {
        if (children.size == 1 && hasNullSourcePosition()) {
            // There is no useful information in this intermediate node, remove.
            return children.single().toViewInfo()
        }

        val childrenViewInfo = children
            .filter { !it.isNullGroup() }
            .map { it.toViewInfo() }

        // TODO: Use group names instead of indexing once it's supported
        return ViewInfo(
            location?.sourceFile ?: "",
            location?.lineNumber ?: -1,
            box,
            location,
            childrenViewInfo
        )
    }

    /**
     * Processes the recorded slot table and re-generates the [viewInfos] attribute.
     */
    private fun processViewInfos() {
        viewInfos = slotTableRecord.store.map { it.asTree() }.map { it.toViewInfo() }.toList()

        if (debugViewInfos) {
            viewInfos.forEach {
                walkTable(it)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        delayedException.getAndSet(null)?.let { exception ->
            // There was a pending exception. Throw it here since Studio will catch it and show
            // it to the user.
            throw exception
        }

        processViewInfos()
        if (composableName.isNotEmpty()) {
            // TODO(b/160126628): support other APIs, e.g. animate
            findAndSubscribeTransitions()
        }
    }

    /**
     * Finds all the transition animations defined in the Compose tree where the root is the
     * `@Composable` being previewed. We only return animations defined in the user code, i.e.
     * the ones we've got source information for.
     */
    @OptIn(InternalAnimationApi::class)
    private fun findAndSubscribeTransitions() {
        val slotTrees = slotTableRecord.store.map { it.asTree() }
        val observers = mutableSetOf<AnimationClockObserver>()
        // Check all the slot tables, since some animations might not be present in the same
        // table as the one containing the `@Composable` being previewed, e.g. when they're
        // defined using sub-composition.
        slotTrees.forEach { tree ->
            observers.addAll(
                tree.findAll {
                    // Find `transition` calls in the user code, i.e. when source location is known
                    it.name == "transition" && it.location != null
                }.mapNotNull {
                    val rememberCall =
                        it.firstOrNull { it.name == "remember" } ?: return@mapNotNull null
                    val transitionModel = rememberCall.data.firstOrNull { data ->
                        data is TransitionModel<*>
                    } as? TransitionModel<*>
                    transitionModel?.anim?.animationClockObserver
                }
            )
        }
        hasAnimations = observers.isNotEmpty()
        // Subscribe all the observers found to the `PreviewAnimationClock`
        if (::clock.isInitialized) {
            observers.forEach { clock.subscribe(it) }
        }
    }

    private fun Group.firstOrNull(predicate: (Group) -> Boolean): Group? {
        return findGroupsThatMatchPredicate(this, predicate, true).firstOrNull()
    }

    private fun Group.findAll(predicate: (Group) -> Boolean): List<Group> {
        return findGroupsThatMatchPredicate(this, predicate)
    }

    /**
     * Search [Group]s that match a given [predicate], starting from a given [root]. An optional
     * boolean parameter can be set if we're interested in a single occurrence. If it's set, we
     * return early after finding the first matching [Group].
     */
    private fun findGroupsThatMatchPredicate(
        root: Group,
        predicate: (Group) -> Boolean,
        findOnlyFirst: Boolean = false
    ): List<Group> {
        val result = mutableListOf<Group>()
        val stack = mutableListOf(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (predicate(current)) {
                if (findOnlyFirst) {
                    return listOf(current)
                }
                result.add(current)
            }
            stack.addAll(current.children)
        }
        return result
    }

    private fun invalidateComposition() {
        // Invalidate the full composition by setting it to empty and back to the actual value
        content.value = emptyContent()
        content.value = previewComposition
        // Invalidate the state of the view so it gets redrawn
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        if (forceCompositionInvalidation) invalidateComposition()

        onDraw()
        if (!debugPaintBounds) {
            return
        }

        viewInfos
            .flatMap { listOf(it) + it.allChildren() }
            .forEach {
                if (it.hasBounds()) {
                    canvas?.apply {
                        val pxBounds = android.graphics.Rect(
                            it.bounds.left,
                            it.bounds.top,
                            it.bounds.right,
                            it.bounds.bottom
                        )
                        drawRect(pxBounds, debugBoundsPaint)
                    }
                }
            }
    }

    /**
     * Clock that controls the animations defined in the context of this [ComposeViewAdapter].
     *
     * @suppress
     */
    @VisibleForTesting
    internal lateinit var clock: PreviewAnimationClock

    /**
     * Wraps a given [Preview] method an does any necessary setup.
     */
    @Composable
    private fun WrapPreview(content: @Composable () -> Unit) {
        // We need to replace the FontResourceLoader to avoid using ResourcesCompat.
        // ResourcesCompat can not load fonts within Layoutlib and, since Layoutlib always runs
        // the latest version, we do not need it.
        Providers(AmbientFontLoader provides LayoutlibFontResourceLoader(context)) {
            Inspectable(slotTableRecord, content)
        }
    }

    /**
     * Initializes the adapter and populates it with the given [Preview] composable.
     * @param className name of the class containing the preview function
     * @param methodName `@Preview` method name
     * @param parameterProvider [Class] for the [PreviewParameterProvider] to be used as
     * parameter input for this call. If null, no parameters will be passed to the composable.
     * @param parameterProviderIndex when [parameterProvider] is not null, this index will
     * reference the element in the [Sequence] to be used as parameter.
     * @param debugPaintBounds if true, the view will paint the boundaries around the layout
     * elements.
     * @param debugViewInfos if true, it will generate the [ViewInfo] structures and will log it.
     * @param animationClockStartTime if positive, the [AnimationClockAmbient] will provide
     * [clock] instead of the default clock, setting this value as the clock's initial time.
     * @param forceCompositionInvalidation if true, the composition will be invalidated on every
     * draw, forcing it to recompose on next render.
     * @param onCommit callback invoked after every commit of the preview composable.
     * @param onDraw callback invoked after every draw of the adapter. Only for test use.
     */
    @VisibleForTesting
    internal fun init(
        className: String,
        methodName: String,
        parameterProvider: Class<out PreviewParameterProvider<*>>? = null,
        parameterProviderIndex: Int = 0,
        debugPaintBounds: Boolean = false,
        debugViewInfos: Boolean = false,
        animationClockStartTime: Long = -1,
        forceCompositionInvalidation: Boolean = false,
        onCommit: () -> Unit = {},
        onDraw: () -> Unit = {}
    ) {
        this.debugPaintBounds = debugPaintBounds
        this.debugViewInfos = debugViewInfos
        this.composableName = methodName
        this.forceCompositionInvalidation = forceCompositionInvalidation
        this.onDraw = onDraw

        previewComposition = @Composable {
            onCommit {
                onCommit()
            }

            WrapPreview {
                val composer = currentComposer
                // We need to delay the reflection instantiation of the class until we are in the
                // composable to ensure all the right initialization has happened and the Composable
                // class loads correctly.
                val composable = {
                    try {
                        invokeComposableViaReflection(
                            className,
                            methodName,
                            composer,
                            *getPreviewProviderParameters(parameterProvider, parameterProviderIndex)
                        )
                    } catch (t: Throwable) {
                        // If there is an exception, store it for later but do not catch it so
                        // compose can handle it and dispose correctly.
                        var exception: Throwable = t
                        // Find the root cause and use that for the delayedException.
                        while (exception is ReflectiveOperationException) {
                            exception = exception.cause ?: break
                        }
                        delayedException.set(exception)
                        throw t
                    }
                }
                if (animationClockStartTime >= 0) {
                    // Provide a custom clock when animation inspection is enabled, i.e. when a
                    // valid `animationClockStartTime` is passed. This clock will control the
                    // animations defined in this `ComposeViewAdapter` from Android Studio.
                    clock = PreviewAnimationClock(animationClockStartTime) {
                        // Invalidate the descendants of this ComposeViewAdapter's only grandchild
                        // (an AndroidOwner) when setting the clock time to make sure the Compose
                        // Preview will animate when the states are read inside the draw scope.
                        val composeView = getChildAt(0) as ComposeView
                        (composeView.getChildAt(0) as? ViewRootForTest)
                            ?.invalidateDescendants()
                    }
                    Providers(AmbientAnimationClock provides clock) {
                        composable()
                    }
                } else {
                    composable()
                }
            }
        }
        composeView.setContent(previewComposition)
        invalidate()
    }

    /**
     * Disposes the Compose elements allocated during [init]
     */
    internal fun dispose() {
        composeView.disposeComposition()
        if (::clock.isInitialized) {
            clock.dispose()
        }
    }

    /**
     *  Returns whether this `@Composable` has animations. This allows Android Studio to decide if
     *  the Animation Inspector icon should be displayed for this preview. The reason for using a
     *  method instead of the property directly is we use Java reflection to call it from Android
     *  Studio, and to find the property we'd need to filter the method names using `contains`
     *  instead of `equals`.
     *
     *  @suppress
     */
    fun hasAnimations() = hasAnimations

    private fun init(attrs: AttributeSet) {
        // ComposeView and lifecycle initialization
        ViewTreeLifecycleOwner.set(this, FakeSavedStateRegistryOwnerOwner)
        ViewTreeSavedStateRegistryOwner.set(this, FakeSavedStateRegistryOwnerOwner)
        ViewTreeViewModelStoreOwner.set(this, FakeViewModelStoreOwner)
        addView(composeView)

        val composableName = attrs.getAttributeValue(TOOLS_NS_URI, "composableName") ?: return
        val className = composableName.substringBeforeLast('.')
        val methodName = composableName.substringAfterLast('.')
        val parameterProviderIndex = attrs.getAttributeIntValue(
            TOOLS_NS_URI,
            "parameterProviderIndex", 0
        )
        val parameterProviderClass = attrs.getAttributeValue(TOOLS_NS_URI, "parameterProviderClass")
            ?.asPreviewProviderClass()

        val animationClockStartTime = try {
            attrs.getAttributeValue(TOOLS_NS_URI, "animationClockStartTime").toLong()
        } catch (e: Exception) {
            -1L
        }

        val forceCompositionInvalidation = attrs.getAttributeBooleanValue(
            TOOLS_NS_URI,
            "forceCompositionInvalidation", false
        )

        init(
            className = className,
            methodName = methodName,
            parameterProvider = parameterProviderClass,
            parameterProviderIndex = parameterProviderIndex,
            debugPaintBounds = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "paintBounds",
                debugPaintBounds
            ),
            debugViewInfos = attrs.getAttributeBooleanValue(
                TOOLS_NS_URI,
                "printViewInfos",
                debugViewInfos
            ),
            animationClockStartTime = animationClockStartTime,
            forceCompositionInvalidation = forceCompositionInvalidation
        )
    }

    private val FakeSavedStateRegistryOwnerOwner = object : SavedStateRegistryOwner {
        private val lifecycle = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this).apply {
            performRestore(Bundle())
        }

        init {
            lifecycle.currentState = Lifecycle.State.RESUMED
        }

        override fun getSavedStateRegistry(): SavedStateRegistry = controller.savedStateRegistry
        override fun getLifecycle(): Lifecycle = lifecycle
    }

    private val FakeViewModelStoreOwner = ViewModelStoreOwner {
        throw IllegalStateException("ViewModels creation is not supported in Preview")
    }
}
