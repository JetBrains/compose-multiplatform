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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.lint.checks.ApiLookup
import com.android.tools.lint.checks.ApiLookup.equivalentName
import com.android.tools.lint.checks.DesugaredMethodLookup
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Desugaring
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.getLongAttribute
import com.android.tools.lint.detector.api.VersionChecks
import com.android.tools.lint.detector.api.VersionChecks.Companion.REQUIRES_API_ANNOTATION
import com.android.tools.lint.detector.api.getInternalMethodName
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import kotlin.math.min
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UInstanceExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.isNullLiteral
import org.jetbrains.uast.util.isConstructorCall
import org.jetbrains.uast.util.isMethodCall

/**
 * This check detects references to platform APIs that are likely to cause class verification
 * failures.
 * <p>
 * Specifically, this check looks for references to APIs that were added prior to the library's
 * minSdkVersion and therefore may not exist on the run-time classpath. If the class verifier
 * detects such a reference, e.g. while verifying a class containing the reference, it will abort
 * verification. This will prevent the class from being optimized, resulting in potentially severe
 * performance losses.
 * <p>
 * See Chromium's excellent guide to Class Verification Failures for more information:
 * https://chromium.googlesource.com/chromium/src/+/HEAD/build/android/docs/class_verification_failures.md
 */
class ClassVerificationFailureDetector : Detector(), SourceCodeScanner {
    private var apiDatabase: ApiLookup? = null

    /**
     * Copied from ApiDetector.kt
     */
    override fun beforeCheckRootProject(context: Context) {
        if (apiDatabase == null) {
            apiDatabase = ApiLookup.get(context.client, context.project.buildTarget)
            // We can't look up the minimum API required by the project here:
            // The manifest file hasn't been processed yet in the -before- project hook.
            // For now it's initialized lazily in getMinSdk(Context), but the
            // lint infrastructure should be fixed to parse manifest file up front.
        }
    }

    /**
     * Copied from ApiDetector.kt
     */
    private fun getMinSdk(context: Context): Int {
        val project = if (context.isGlobalAnalysis())
            context.mainProject else context.project
        return if (!project.isAndroidProject) {
            // Don't flag API checks in non-Android projects
            Integer.MAX_VALUE
        } else {
            project.minSdkVersion.featureLevel
        }
    }

    /**
     * Copied from ApiDetector.kt
     */
    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (apiDatabase == null || context.isTestSource && !context.driver.checkTestSources) {
            return null
        }
        val project = if (context.isGlobalAnalysis())
            context.mainProject else context.project
        return if (project.isAndroidProject) {
            ApiVisitor(context)
        } else {
            null
        }
    }

    /**
     * Modified from ApiDetector.kt
     *
     * Changes:
     * - Only `UCallExpression` is returned in the list
     */
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    /**
     * Modified from ApiDetector.kt
     *
     * Changes:
     * - Only the `visitCall` method has been copied over
     */
    private inner class ApiVisitor(private val context: JavaContext) : UElementHandler() {

        /**
         * Modified from ApiDetector.kt
         *
         * Changes:
         * - Replaced the `method == null` conditional block with elvis operator `return`
         */
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve() ?: return

            visitCall(method, node, node)
        }

        /**
         * Modified from ApiDetector.kt
         *
         * Changes:
         * - Removed cast checking on parameter list
         * - Removed PsiCompiledElement check
         * - Removed special-casing of Support Library
         * - Removed SimpleDateFormat and Animator checks
         * - Removed check for suppression
         * - Removed signature generation
         * - Removed unused values (signature, desugaring, fqcn)
         * - Replaced `report` call with a call to `visitNewApiCall`
         */
        private fun visitCall(
            method: PsiMethod,
            call: UCallExpression,
            reference: UElement
        ) {
            val apiDatabase = apiDatabase ?: return
            val containingClass = method.containingClass ?: return

            // Change: Removed cast checking on parameter list

            // Change: Removed PsiCompiledElement check

            val evaluator = context.evaluator
            val owner = evaluator.getQualifiedName(containingClass)
                ?: return // Couldn't resolve type

            // Change: Removed special-casing of Support library

            if (!apiDatabase.containsClass(owner)) {
                return
            }

            val name = getInternalMethodName(method)
            val desc = evaluator.getMethodDescription(
                method,
                includeName = false,
                includeReturn = false
            ) // Couldn't compute description of method for some reason; probably
                // failure to resolve parameter types
                ?: return

            // Change: Removed SimpleDateFormat and Animator checks
            @Suppress("DEPRECATION") // b/262915628
            var api = apiDatabase.getMethodVersion(owner, name, desc)
            if (api == -1) {
                return
            }
            val minSdk = getMinSdk(context)
            if (api <= minSdk) {
                return
            }

            val receiver = if (call.isMethodCall()) {
                call.receiver
            } else {
                null
            }

            // The lint API database contains two optimizations:
            // First, all members that were available in API 1 are omitted from the database,
            // since that saves about half of the size of the database, and for API check
            // purposes, we don't need to distinguish between "doesn't exist" and "available
            // in all versions".

            // Second, all inherited members were inlined into each class, so that it doesn't
            // have to do a repeated search up the inheritance chain.
            //
            // Unfortunately, in this custom PSI detector, we look up the real resolved method,
            // which can sometimes have a different minimum API.
            //
            // For example, SQLiteDatabase had a close() method from API 1. Therefore, calling
            // SQLiteDatabase is supported in all versions. However, it extends SQLiteClosable,
            // which in API 16 added "implements Closable". In this detector, if we have the
            // following code:
            //     void test(SQLiteDatabase db) { db.close }
            // here the call expression will be the close method on type SQLiteClosable. And
            // that will result in an API requirement of API 16, since the close method it now
            // resolves to is in API 16.
            //
            // To work around this, we can now look up the type of the call expression ("db"
            // in the above, but it could have been more complicated), and if that's a
            // different type than the type of the method, we look up *that* method from
            // lint's database instead. Furthermore, it's possible for that method to return
            // "-1" and we can't tell if that means "doesn't exist" or "present in API 1", we
            // then check the package prefix to see whether we know it's an API method whose
            // members should all have been inlined.
            if (call.isMethodCall()) {
                if (receiver != null &&
                    receiver !is UThisExpression &&
                    receiver !is PsiSuperExpression
                ) {
                    val receiverType = receiver.getExpressionType()
                    if (receiverType is PsiClassType) {
                        val containingType = context.evaluator.getClassType(containingClass)
                        val inheritanceChain =
                            getInheritanceChain(receiverType, containingType)
                        if (inheritanceChain != null) {
                            for (type in inheritanceChain) {
                                val expressionOwner = evaluator.getQualifiedName(type)
                                if (expressionOwner != null && expressionOwner != owner) {
                                    @Suppress("DEPRECATION") // b/262915628
                                    val specificApi = apiDatabase.getMethodVersion(
                                        expressionOwner, name, desc
                                    )
                                    if (specificApi == -1) {
                                        if (apiDatabase.isRelevantOwner(expressionOwner)) {
                                            return
                                        }
                                    } else if (specificApi <= minSdk) {
                                        return
                                    } else {
                                        // For example, for Bundle#getString(String,String) the API level
                                        // is 12, whereas for BaseBundle#getString(String,String) the API
                                        // level is 21. If the code specified a Bundle instead of
                                        // a BaseBundle, reported the Bundle level in the error message
                                        // instead.
                                        if (specificApi < api) {
                                            api = specificApi
                                        }
                                        api = min(specificApi, api)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Unqualified call; need to search in our super hierarchy
                    var cls: PsiClass? = null
                    val receiverType = call.receiverType
                    if (receiverType is PsiClassType) {
                        cls = receiverType.resolve()
                    }

                    if (receiver is UThisExpression || receiver is USuperExpression) {
                        val pte = receiver as UInstanceExpression
                        val resolved = pte.resolve()
                        if (resolved is PsiClass) {
                            cls = resolved
                        }
                    }

                    while (cls != null) {
                        if (cls is PsiAnonymousClass) {
                            // If it's an unqualified call in an anonymous class, we need to
                            // rely on the resolve method to find out whether the method is
                            // picked up from the anonymous class chain or any outer classes
                            var found = false
                            val anonymousBaseType = cls.baseClassType
                            val anonymousBase = anonymousBaseType.resolve()
                            if (anonymousBase != null && anonymousBase.isInheritor(
                                    containingClass,
                                    true
                                )
                            ) {
                                cls = anonymousBase
                                found = true
                            } else {
                                val surroundingBaseType =
                                    PsiTreeUtil.getParentOfType(cls, PsiClass::class.java, true)
                                if (surroundingBaseType != null && surroundingBaseType.isInheritor(
                                        containingClass,
                                        true
                                    )
                                ) {
                                    cls = surroundingBaseType
                                    found = true
                                }
                            }
                            if (!found) {
                                break
                            }
                        }
                        val expressionOwner = evaluator.getQualifiedName(cls)
                        if (expressionOwner == null || equivalentName(
                                expressionOwner,
                                "java/lang/Object"
                            )
                        ) {
                            break
                        }
                        @Suppress("DEPRECATION") // b/262915628
                        val specificApi =
                            apiDatabase.getMethodVersion(expressionOwner, name, desc)
                        if (specificApi == -1) {
                            if (apiDatabase.isRelevantOwner(expressionOwner)) {
                                break
                            }
                        } else if (specificApi <= minSdk) {
                            return
                        } else {
                            if (specificApi < api) {
                                api = specificApi
                            }
                            api = min(specificApi, api)
                            break
                        }
                        cls = cls.superClass
                    }
                }
            }

            // Change: Removed check for suppression

            if (receiver != null || call.isMethodCall()) {
                var target: PsiClass? = null
                if (!method.isConstructor) {
                    if (receiver != null) {
                        val type = receiver.getExpressionType()
                        if (type is PsiClassType) {
                            target = type.resolve()
                        }
                    } else {
                        target = call.getContainingUClass()?.javaPsi
                    }
                }

                // Look to see if there's a possible local receiver
                if (target != null) {
                    val methods = target.findMethodsBySignature(method, true)
                    if (methods.size > 1) {
                        for (m in methods) {
                            //noinspection LintImplPsiEquals
                            if (method != m) {
                                val provider = m.containingClass
                                if (provider != null) {
                                    val methodOwner = evaluator.getQualifiedName(provider)
                                    if (methodOwner != null) {
                                        @Suppress("DEPRECATION") // b/262915628
                                        val methodApi = apiDatabase.getMethodVersion(
                                            methodOwner, name, desc
                                        )
                                        if (methodApi == -1 || methodApi <= minSdk) {
                                            // Yes, we found another call that doesn't have an API requirement
                                            return
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If you're simply calling super.X from method X, even if method X is in a higher
                // API level than the minSdk, we're generally safe; that method should only be
                // called by the framework on the right API levels. (There is a danger of somebody
                // calling that method locally in other contexts, but this is hopefully unlikely.)
                if (receiver is USuperExpression) {
                    val containingMethod = call.getContainingUMethod()?.javaPsi
                    if (containingMethod != null &&
                        name == containingMethod.name &&
                        evaluator.areSignaturesEqual(method, containingMethod) &&
                        // We specifically exclude constructors from this check, because we
                        // do want to flag constructors requiring the new API level; it's
                        // highly likely that the constructor is called by local code so
                        // you should specifically investigate this as a developer
                        !method.isConstructor
                    ) {
                        return
                    }
                }
            }

            // Builtin R8 desugaring, such as rewriting compare calls (see b/36390874)
            if (owner.startsWith("java.") &&
                DesugaredMethodLookup.isDesugared(owner, name, desc, context.sourceSetType)) {
                return
            }

            // These methods are not included in the R8 backported list so handle them manually
            // the way R8 seems to
            if (api == 19 && owner == "java.lang.Throwable" &&
                (name == "addSuppressed" && desc == "(Ljava.lang.Throwable;)" ||
                    name == "getSuppressed" && desc == "()")
            ) {
                if (context.project.isDesugaring(Desugaring.TRY_WITH_RESOURCES)) {
                    return
                }
            }

            // Change: Removed signature generation

            val nameIdentifier = call.methodIdentifier
            val location = if (call.isConstructorCall() && call.classReference != null) {
                context.getRangeLocation(call, 0, call.classReference!!, 0)
            } else if (nameIdentifier != null) {
                context.getLocation(nameIdentifier)
            } else {
                context.getLocation(reference)
            }

            // Change: Replaced `report` call with a call to `visitNewApiCall`
            visitNewApiCall(call, method, api, reference, location)
        }

        fun visitNewApiCall(
            call: UCallExpression?,
            method: PsiMethod,
            api: Int,
            reference: UElement,
            location: Location
        ) {
            call ?: return
            var classUnderInspection: UClass? = call.getContainingUClass() ?: return

            // Walk up class hierarchy to find if there is any RequiresApi annotation that fulfills
            // the API requirements
            while (classUnderInspection != null) {
                val potentialRequiresApiVersion = getRequiresApiFromAnnotations(
                    classUnderInspection.javaPsi
                )

                if (potentialRequiresApiVersion >= api) {
                    return
                }

                classUnderInspection = classUnderInspection.getContainingUClass()
            }

            // call.getContainingUClass()!! refers to the direct parent class of this method
            val containingClassName = call.getContainingUClass()!!.qualifiedName.toString()
            val lintFix = createLintFix(method, call, api)
            val incident = Incident(context)
                .fix(lintFix)
                .issue(ISSUE)
                .location(location)
                .message("This call references a method added in API level $api; however, the " +
                    "containing class $containingClassName is reachable from earlier API " +
                    "levels and will fail run-time class verification.")
                .scope(reference)

            context.report(incident)
        }

        /**
         * Attempts to create a [LintFix] for the call to specified method.
         *
         * @return a lint fix, or `null` if no fix could be created
         */
        private fun createLintFix(
            method: PsiMethod,
            call: UCallExpression,
            api: Int
        ): LintFix? {
            val callPsi = call.sourcePsi ?: return null
            if (isKotlin(callPsi)) {
                // We only support Java right now.
                return null
            }

            // The host class should never be null if we're looking at Java code.
            val callContainingClass = call.getContainingUClass() ?: return null

            val (wrapperMethodName, wrapperMethodParams, methodForInsertion) =
                generateWrapperMethod(
                    method,
                    // Find what type the result of this call is used as
                    getExpectedTypeByParent(callPsi)
                ) ?: return null

            val (wrapperClassName, insertionPoint, insertionSource) = generateInsertionSource(
                api,
                callContainingClass,
                wrapperMethodName,
                wrapperMethodParams,
                methodForInsertion
            )

            val replacementCall = generateWrapperCall(
                method,
                call.receiver,
                call.valueArguments,
                wrapperClassName,
                wrapperMethodName,
                wrapperMethodParams
            )

            val fix = fix()
                .name("Extract to static inner class")
                .composite()
                .add(fix()
                    .replace()
                    .range(context.getLocation(call))
                    .with(replacementCall)
                    .shortenNames()
                    .build()
                )

            if (insertionPoint != null) {
                fix.add(fix()
                    .replace()
                    .range(insertionPoint)
                    .beginning()
                    .with(insertionSource)
                    .shortenNames()
                    .build()
                )
            }

            return fix.build()
        }

        /**
         * Find what type the parent of the element is expecting the element to be.
         */
        private fun getExpectedTypeByParent(element: PsiElement): PsiType? {
            val expectedType = PsiTypesUtil.getExpectedTypeByParent(element)
            if (expectedType != null) return expectedType

            // PsiTypesUtil didn't know the expected type, but it doesn't handle the case when the
            // value is a parameter to a method call. See if that's what this is.
            val (parent, childOfParent) = getParentSkipParens(element)
            if (parent is PsiExpressionList) {
                val grandparent = PsiUtil.skipParenthesizedExprUp(parent.parent)
                if (grandparent is PsiMethodCallExpression) {
                    val paramIndex = parent.expressions.indexOf(childOfParent)
                    if (paramIndex < 0) return null
                    val method = grandparent.resolveMethod() ?: return null
                    return method.parameterList.getParameter(paramIndex)?.type
                }
            }
            // Not a parameter, still don't know the expected type (or there isn't one).
            return null
        }

        /**
         * Return the first parent of the element which isn't a PsiParenthesizedExpression, and also
         * return the direct child element of that parent.
         */
        private fun getParentSkipParens(element: PsiElement): Pair<PsiElement, PsiElement> {
            val parent = element.parent
            return if (parent is PsiParenthesizedExpression) {
                getParentSkipParens(parent)
            } else {
                Pair(parent, element)
            }
        }

        /**
         * Generates source code for a wrapper method and class (where applicable) and calculates
         * the insertion point. If the wrapper class already exists, returns source code for the
         * method body only with an insertion point at the end of the existing wrapper class body.
         * If the wrapper class and method both already exists, just returns the name of the
         * wrapper class.
         *
         * Source code follows the general format:
         *
         * ```java
         * @RequiresApi(21)
         * static class Api21Impl {
         *   private Api21Impl() {}
         *   // Method body here.
         * }
         * ```
         *
         * @param api API level at which the platform method can be safely called
         * @param callContainingClass Class containing the call to the platform method
         * @param wrapperMethodName The name of the wrapper method, used to check if the wrapper
         * method already exists
         * @param wrapperMethodParams List of the types of the wrapper method's parameters, used to
         * check if the wrapper method already exists
         * @param wrapperMethodBody Source code for the wrapper method
         * @return Triple containing (1) the name of the static wrapper class, (2) the insertion
         * point for the generated source code (or null if the wrapper method already exists), and
         * (3) generated source code for a static wrapper method, including a static wrapper class
         * if necessary (or null if the wrapper method already exists)
         */
        private fun generateInsertionSource(
            api: Int,
            callContainingClass: UClass,
            wrapperMethodName: String,
            wrapperMethodParams: List<PsiType>,
            wrapperMethodBody: String,
        ): Triple<String, Location?, String?> {
            val wrapperClassName = "Api${api}Impl"
            val implInsertionPoint: Location?
            val implForInsertion: String?

            val existingWrapperClass = callContainingClass.innerClasses.find { innerClass ->
                innerClass.name == wrapperClassName
            }

            if (existingWrapperClass == null) {
                implInsertionPoint = context.getLocation(callContainingClass.lastChild)
                implForInsertion = """
                @androidx.annotation.RequiresApi($api)
                static class $wrapperClassName {
                    private $wrapperClassName() {
                        // This class is not instantiable.
                    }
                    $wrapperMethodBody
                }

                """.trimIndent()
            } else {
                val existingWrapperMethod = existingWrapperClass.methods.find { method ->
                    method.name == wrapperMethodName &&
                        wrapperMethodParams == getParameterTypes(method)
                }
                if (existingWrapperMethod == null) {
                    implInsertionPoint = context.getLocation(existingWrapperClass.lastChild)
                    // Add a newline to force the `}`s for the class and method onto different lines
                    implForInsertion = wrapperMethodBody.trimIndent() + "\n"
                } else {
                    implInsertionPoint = null
                    implForInsertion = null
                }
            }

            return Triple(
                wrapperClassName,
                implInsertionPoint,
                implForInsertion
            )
        }

        /**
         * Generates source code for a call to the generated wrapper method.
         *
         * Source code follows the general format:
         *
         * ```
         * WrapperClassName.wrapperMethodName(receiverVar, argumentVar)
         * ```
         *
         * @param method Platform method which is being called
         * @param callReceiver Receiver of the call to the platform method
         * @param callValueArguments Arguments of the call to the platform method
         * @param wrapperClassName Name of the generated wrapper class
         * @param wrapperMethodName Name of the generated wrapper method
         * @param wrapperMethodParams Param types of the wrapper method
         * @return Source code for a call to the static wrapper method
         */
        private fun generateWrapperCall(
            method: PsiMethod,
            callReceiver: UExpression?,
            callValueArguments: List<UExpression>,
            wrapperClassName: String,
            wrapperMethodName: String,
            wrapperMethodParams: List<PsiType>
        ): String {
            val callReceiverStr = when {
                // Static method
                context.evaluator.isStatic(method) ->
                    null
                // Constructor
                method.isConstructor ->
                    null
                // If there is no call receiver, and the method isn't a constructor or static,
                // it must be a call to an instance method using `this` implicitly.
                callReceiver == null ->
                    "this"
                // Use the original call receiver string (removing extra parens), casting if needed
                else ->
                    createArgumentString(unwrapExpression(callReceiver), wrapperMethodParams[0])
            }

            val callValues = if (callValueArguments.isNotEmpty()) {
                // The first element in the wrapperMethodParams is the receiver, so drop that.
                // Also drop the last parameter, because it is special-cased later.
                val paramTypesWithoutReceiverAndFinal = wrapperMethodParams.drop(1).dropLast(1)
                // For varargs methods, what we care about for the last type is the type of each
                // vararg, not the containing type.
                val finalParamType = if (method.isVarArgs) {
                    (wrapperMethodParams.last() as PsiEllipsisType).componentType
                } else {
                    wrapperMethodParams.last()
                }

                callValueArguments.mapIndexed { argIndex, arg ->
                    // The number of args might be greater than the number of param types due to
                    // varargs, repeat the final param type for all args after exhausting the
                    // paramTypesWithoutReceiverAndFinal list.
                    val expectedType = if (argIndex < paramTypesWithoutReceiverAndFinal.size) {
                        paramTypesWithoutReceiverAndFinal[argIndex]
                    } else {
                        finalParamType
                    }
                    createArgumentString(arg, expectedType)
                }.joinToString(separator = ", ")
            } else {
                null
            }

            val replacementArgs = listOfNotNull(callReceiverStr, callValues).joinToString(", ")

            return "$wrapperClassName.$wrapperMethodName($replacementArgs)"
        }

        /**
         * Creates the string representation of an argument in the wrapper call. If the type of the
         * arg is not identical to the parameter type of the wrapper method, casts to that type.
         */
        private fun createArgumentString(arg: UExpression, expectedType: PsiType): String {
            val argType = arg.getExpressionType()
            val expectedTypeText = expectedType.canonicalText
            // If the arg is the expected type, use as normal, otherwise, cast to expected type.
            // Uses text-base equality instead of directly comparing types because certain types
            // (eq. java.lang.Class<T>) are not necessarily equal to instances of the same type.
            // There isn't really a point in casting if the arg is null.
            return if (argType?.equalsToText(expectedTypeText) == true || arg.isNullLiteral()) {
                arg.asSourceString()
            } else {
                "($expectedTypeText) ${arg.asSourceString()}"
            }
        }

        /**
         * Remove parentheses from the expression (unwrap the expression until it is no longer a
         * UParenthesizedExpression).
         */
        private fun unwrapExpression(expr: UExpression): UExpression {
            var unwrappedExpr = expr
            while (unwrappedExpr is UParenthesizedExpression) {
                unwrappedExpr = unwrappedExpr.expression
            }
            return unwrappedExpr
        }

        /**
         * Generates source code for a wrapper method, or `null` if we don't know how to do that.
         * Currently, this method is capable of handling method and constructor calls from Java
         * source code.
         *
         * Source code follows the general format:
         *
         * ```
         * @DoNotInline
         * static ReturnType methodName(HostType hostType, ParamType paramType) {
         *   return hostType.methodName(paramType);
         * }
         * ```
         *
         * @param method Platform method which is being called
         * @return Pair containing (1) the name of the static wrapper method and (2) generated
         * source code for a static wrapper around the platform method
         */
        private fun generateWrapperMethod(
            method: PsiMethod,
            expectedReturnType: PsiType?
        ): Triple<String, List<PsiType>, String>? {
            val evaluator = context.evaluator
            val isStatic = evaluator.isStatic(method)
            val isConstructor = method.isConstructor

            // None of these should be null if we're looking at Java code.
            val containingClass = method.containingClass ?: return null
            // When referencing the type, use the fully qualified type in case it isn't imported
            // (shortenTypes will simplify if it is). For the variable name, use just the class name
            val hostType = containingClass.qualifiedName ?: return null
            val hostClassName = containingClass.name ?: return null
            val hostVar = hostClassName[0].lowercaseChar() + hostClassName.substring(1)

            val hostParam = if (isStatic || isConstructor) { null } else { "$hostType $hostVar" }

            val typeParamsStr = if (method.typeParameters.isNotEmpty()) {
                "<${method.typeParameters.joinToString(", ") { param -> "${param.name}" }}> "
            } else {
                ""
            }

            val typedParams = method.parameters.map { param ->
                "${(param.type as? PsiType)?.canonicalText} ${param.name}"
            }
            val typedParamsStr = (listOfNotNull(hostParam) + typedParams).joinToString(", ")

            val paramTypes = listOf(PsiTypesUtil.getClassType(containingClass)) +
                getParameterTypes(method)

            val namedParamsStr = method.parameters.joinToString(separator = ", ") { param ->
                "${param.name}"
            }

            val methodName: String
            var wrapperMethodName: String
            var returnTypeStr: String
            val receiverStr: String

            if (isConstructor) {
                methodName = hostType
                wrapperMethodName = "create$hostClassName"
                returnTypeStr = hostType
                receiverStr = "new "
            } else {
                methodName = method.name
                wrapperMethodName = methodName
                // PsiMethod.returnType is only supposed to be null if the method is a constructor,
                // so something has gone wrong if it's null here.
                returnTypeStr = method.returnType?.canonicalText ?: return null
                receiverStr = if (isStatic) "$hostType." else "$hostVar."
            }

            // If the parent is expecting a different return type, use that type.
            // This is to prevent a CVF on an implicit cast from a type that isn't available at the
            // caller's API level to a type that is, which becomes a cast from Object.
            // The real return type should be the same or a subtype of what the parent is expecting.
            // Also changes the method name to avoid conflicts if the same method with a different
            // expected return type is needed elsewhere.
            if (expectedReturnType != null && expectedReturnType.canonicalText != returnTypeStr) {
                returnTypeStr = expectedReturnType.canonicalText
                wrapperMethodName += "Returns${expectedReturnType.presentableText}"
            } else if (expectedReturnType == null && returnTypeStr != "void" &&
                !classAvailableAtMinSdk(returnTypeStr)) {
                // This method returns a value of a type that isn't available at the min SDK.
                // The expected return type is null either because the returned value isn't used or
                // getExpectedTypeByParent didn't know how it is used. In case it is used and is
                // actually expected to be a different type, don't suggest an autofix to prevent an
                // invalid implicit cast.
                return null
            }

            val returnStmtStr = if ("void" == returnTypeStr) "" else "return "

            return Triple(
                wrapperMethodName,
                paramTypes,
                """
                    @androidx.annotation.DoNotInline
                    static $typeParamsStr$returnTypeStr $wrapperMethodName($typedParamsStr) {
                        $returnStmtStr$receiverStr$methodName($namedParamsStr);
                    }
                """
            )
        }

        /**
         * Returns a list of the method's parameter types.
         */
        private fun getParameterTypes(method: PsiMethod): List<PsiType> =
            method.parameterList.parameters.map { it.type }

        /**
         * Check if the specified class is available at the min SDK.
         */
        private fun classAvailableAtMinSdk(className: String): Boolean {
            val apiDatabase = apiDatabase ?: return false
            val minSdk = getMinSdk(context)
            @Suppress("DEPRECATION") // b/262915628
            val version = apiDatabase.getClassVersion(className)
            return version <= minSdk
        }

        private fun getInheritanceChain(
            derivedClass: PsiClassType,
            baseClass: PsiClassType?
        ): List<PsiClassType>? {
            if (derivedClass == baseClass) {
                return emptyList()
            }
            val chain = getInheritanceChain(derivedClass, baseClass, HashSet(), 0)
            chain?.reverse()
            return chain
        }

        private fun getInheritanceChain(
            derivedClass: PsiClassType,
            baseClass: PsiClassType?,
            visited: HashSet<PsiType>,
            depth: Int
        ): MutableList<PsiClassType>? {
            if (derivedClass == baseClass) {
                return ArrayList(depth)
            }
            for (type in derivedClass.superTypes) {
                if (visited.add(type) && type is PsiClassType) {
                    val chain = getInheritanceChain(type, baseClass, visited, depth + 1)
                    if (chain != null) {
                        chain.add(derivedClass)
                        return chain
                    }
                }
            }
            return null
        }

        private fun getRequiresApiFromAnnotations(modifierListOwner: PsiModifierListOwner): Int {
            for (annotation in context.evaluator.getAnnotations(modifierListOwner)) {
                val qualifiedName = annotation.qualifiedName
                if (REQUIRES_API_ANNOTATION.isEquals(qualifiedName)) {
                    var api = getLongAttribute(
                        context, annotation,
                        ATTR_VALUE, NO_API_REQUIREMENT.toLong()
                    ).toInt()
                    if (api <= 1) {
                        // @RequiresApi has two aliasing attributes: api and value
                        api = getLongAttribute(
                            context, annotation, "api", NO_API_REQUIREMENT.toLong()).toInt()
                    }
                    return api
                } else if (qualifiedName == null) {
                    // Work around UAST type resolution problems
                    // Work around bugs in UAST type resolution for file annotations:
                    // parse the source string instead.
                    val psiAnnotation = annotation.javaPsi
                    if (psiAnnotation == null || psiAnnotation is PsiCompiledElement) {
                        continue
                    }
                    val text = psiAnnotation.text
                    if (text.contains("RequiresApi(")) {
                        val start = text.indexOf('(')
                        val end = text.indexOf(')', start + 1)
                        if (end != -1) {
                            var name = text.substring(start + 1, end)
                            // Strip off attribute name and qualifiers, e.g.
                            //   @RequiresApi(api = Build.VERSION.O) -> O
                            var index = name.indexOf('=')
                            if (index != -1) {
                                name = name.substring(index + 1).trim()
                            }
                            index = name.indexOf('.')
                            if (index != -1) {
                                name = name.substring(index + 1)
                            }
                            if (!name.isEmpty()) {
                                if (name[0].isDigit()) {
                                    val api = Integer.parseInt(name)
                                    if (api > 0) {
                                        return api
                                    }
                                } else {
                                    return VersionChecks.codeNameToApi(name)
                                }
                            }
                        }
                    }
                }
            }
            return NO_API_REQUIREMENT
        }
    }

    companion object {
        const val NO_API_REQUIREMENT = -1
        val ISSUE = Issue.create(
            "ClassVerificationFailure",
            "Even in cases where references to new APIs are gated on SDK_INT " +
                "checks, run-time class verification will still fail on references to APIs that " +
                "may not be available at run time, including platform APIs introduced after a " +
                "library's minSdkVersion.",
            """
                References to APIs added after a library's minSdkVersion -- regardless of
                any surrounding version checks -- will fail run-time class verification if
                the API does not exist on the device, leading to reduced run-time performance.

                To prevent class verification failures, references to new APIs must be moved to
                methods within inner classes that are only initialized inside of an appropriate
                SDK check. These methods must be paired with the @DoNotInline annotation.

                For more details and an example, see go/androidx-api-guidelines#compat-sdk.
            """,
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(ClassVerificationFailureDetector::class.java, Scope.JAVA_FILE_SCOPE)
        ).setAndroidSpecific(true)
    }
}
