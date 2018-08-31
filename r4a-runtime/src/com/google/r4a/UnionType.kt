/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class UnionType(vararg val types: KClass<*>)

