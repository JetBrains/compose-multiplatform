/**
 * This package contains all the logic related to lowering a PSI containing R4A Components into IR.
 * The entry-point for this package is ComponentClassLowering, which will generate all supporting synthetics.
 * Each synthetic class of type [ClassName] lives in a file called [ClassName]Generator.
 *
 * Anything beginning with the token `lower` may modify IR
 * Anything beginning with the token `generate` may only produce (return) IR
 */
package org.jetbrains.kotlin.r4a.compiler.lower;
