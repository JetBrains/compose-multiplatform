/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

// Keep generated code and compiler work within these limits.
// When a limit is reached, leave the remaining work to runtime rendering.

// Constant evaluation: bound compiler time and memory.
internal const val MaxEvaluatedStringLength = 1_000_000
// Expression and alias recursion.
internal const val MaxFoldDepth = 64
// Work per expression.
internal const val MaxFoldSteps = 2_048
// IR interpreter stack and work per operation.
internal const val MaxInterpreterStackDepth = 64
internal const val MaxInterpreterCommands = 2_048

// Static HTML: bound fragment size and expansion.
internal const val MaxEncodedFragmentLength = 1_000_000
internal const val MaxExpandedHtmlCalls = 8_000
// Subtree height, cached per node.
internal const val MaxHtmlNestingDepth = 64
// Traverse deeper to find lowerable children under rejected roots.
internal const val MaxEncodingRecursion = 4 * MaxHtmlNestingDepth
internal const val MaxStaticRepeatCount = 10_000

// Attributes: keep callbacks below the JVM's 64 KiB method limit.
internal const val MaxPreparedAttributeOperations = 128
internal const val MaxMixedAttributeOperations = 128
// Duplicate checks grow quadratically.
internal const val MaxMixedClassTokens = 32
// Cap fields and constant-pool use.
internal const val MaxPreparedAttributesPerFile = 256

// Chunks fit the JVM's 65,535-byte modified UTF-8 constant limit.
internal const val MaxHtmlChunkLength = 8_000
// Bound method size and constant-pool use.
internal const val MaxGeneratedChunksPerFunction = 128
internal const val MaxGeneratedChunksPerFile = 1_024
// Cap cached data and string initialization code.
internal const val MaxCachedStringLengthPerFile = 4_000_000
