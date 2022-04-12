/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.invokeLater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.nio.file.Path

class TodoTempLifeListener : com.intellij.ide.AppLifecycleListener {

  val swingScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

  init {
    swingScope.launch {
      invokeLater {
//        ProjectUtil.openOrImport(Path.of("/Users/dim/IdeaProjects/untitled2"))
        ProjectUtil.openOrImport(Path.of("/Users/dim/Desktop/github/JetBrains/compose-jb/examples/intellij-plugin"))
      }
    }
  }

}
