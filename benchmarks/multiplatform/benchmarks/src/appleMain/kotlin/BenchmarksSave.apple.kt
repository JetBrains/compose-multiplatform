/*
 * Copyright 2020-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

actual fun saveBenchmarkStats(name: String, stats: BenchmarkStats) = saveBenchmarkStatsOnDisk(name, stats)
