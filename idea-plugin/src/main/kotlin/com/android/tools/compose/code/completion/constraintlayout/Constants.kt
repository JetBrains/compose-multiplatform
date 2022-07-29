/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.compose.code.completion.constraintlayout

internal object KeyWords {
  /**
   * Name of the property within a MotionScene that contains several ConstraintSet declarations.
   */
  const val ConstraintSets = "ConstraintSets"

  /**
   * Name of the property within a MotionScene that contains several Transition declarations.
   */
  const val Transitions = "Transitions"

  /**
   * Name of the property used to indicate that the containing ConstraintSet inherits its constraints from the ConstraintSet given by the
   * `Extends` property value.
   */
  const val Extends = "Extends"

  /**
   * Reserved ID for the containing layout. Typically referenced in constraint blocks.
   */
  const val ParentId = "parent"

  /**
   * Name of the Visibility property in a constraint block.
   */
  const val Visibility = "visibility"

  /**
   * Name of the Clear property in a constraint block.
   *
   * Populated by an array of options to clear inherited parameters from [Extends].
   *
   * @see ClearOption
   */
  const val Clear = "clear"
}

/**
 * Common interface to simplify handling multiple enum Classes.
 *
 * [keyWord] is the case-sensitive string used in the syntax.
 */
internal interface ConstraintLayoutKeyWord {
  val keyWord: String
}

//region Constrain KeyWords
/**
 * The classic anchors used to constrain a widget.
 */
internal enum class StandardAnchor(override val keyWord: String) : ConstraintLayoutKeyWord {
  Start("start"),
  Left("left"),
  End("end"),
  Right("right"),
  Top("top"),
  Bottom("bottom"),
  Baseline("baseline");

  companion object {
    fun isVertical(keyWord: String) = verticalAnchors.any { it.keyWord == keyWord }

    fun isHorizontal(keyWord: String) = horizontalAnchors.any { it.keyWord == keyWord }

    val horizontalAnchors: List<StandardAnchor> = listOf(Start, End, Left, Right)

    val verticalAnchors: List<StandardAnchor> = listOf(Top, Bottom, Baseline)
  }
}

/**
 * Non-typical anchors.
 *
 * These implicitly apply multiple [StandardAnchor]s.
 */
internal enum class SpecialAnchor(override val keyWord: String) : ConstraintLayoutKeyWord {
  Center("center"),
  CenterH("centerHorizontally"),
  CenterV("centerVertically")
}

/**
 * Supported keywords to define the dimension of a widget.
 */
internal enum class Dimension(override val keyWord: String) : ConstraintLayoutKeyWord {
  Width("width"),
  Height("height")
}

/**
 * Keywords to apply rendering time transformations to a widget.
 */
internal enum class RenderTransform(override val keyWord: String) : ConstraintLayoutKeyWord {
  Alpha("alpha"),
  ScaleX("scaleX"),
  ScaleY("scaleY"),
  RotationX("rotationX"),
  RotationY("rotationY"),
  RotationZ("rotationZ"),
  TranslationX("translationX"),
  TranslationY("translationY"),
  TranslationZ("translationZ"),
}
//endregion

internal enum class DimBehavior(override val keyWord: String) : ConstraintLayoutKeyWord {
  Spread("spread"),
  Wrap("wrap"),
  PreferWrap("preferWrap"),
  MatchParent("parent")
}

internal enum class VisibilityMode(override val keyWord: String): ConstraintLayoutKeyWord {
  Visible("visible"),
  Invisible("invisible"),
  Gone("gone")
}

internal enum class ClearOption(override val keyWord: String): ConstraintLayoutKeyWord {
  Constraints("constraints"),
  Dimensions("dimensions"),
  Transforms("transforms")
}

internal enum class TransitionField(override val keyWord: String): ConstraintLayoutKeyWord {
  From("from"),
  To("to"),
  PathArc("pathMotionArc"),
  KeyFrames("KeyFrames"),
  OnSwipe("onSwipe")
}

internal enum class OnSwipeField(override val keyWord: String): ConstraintLayoutKeyWord {
  AnchorId("anchor"),
  Direction("direction"),
  Side("side"),
  Mode("mode")
}

internal enum class OnSwipeSide(override val keyWord: String): ConstraintLayoutKeyWord {
  Top("top"),
  Left("left"),
  Right("right"),
  Bottom("bottom"),
  Middle("middle"),
  Start("start"),
  End("end")
}

internal enum class OnSwipeDirection(override val keyWord: String): ConstraintLayoutKeyWord {
  Up("up"),
  Down("down"),
  Left("left"),
  Right("right"),
  Start("start"),
  End("end"),
  Clockwise("clockwise"),
  AntiClockwise("anticlockwise")
}

internal enum class OnSwipeMode(override val keyWord: String): ConstraintLayoutKeyWord {
  Velocity("velocity"),
  Spring("spring")
}

internal enum class KeyFrameField(override val keyWord: String): ConstraintLayoutKeyWord {
  Positions("KeyPositions"),
  Attributes("KeyAttributes"),
  Cycles("KeyCycles")
}

/**
 * Common fields used by any of [KeyFrameField].
 */
internal enum class KeyFrameChildCommonField(override val keyWord: String): ConstraintLayoutKeyWord {
  TargetId("target"),
  Frames("frames"),
  Easing("transitionEasing"),
  Fit("curveFit"),
}

internal enum class KeyPositionField(override val keyWord: String): ConstraintLayoutKeyWord {
  PercentX("percentX"),
  PercentY("percentY"),
  PercentWidth("percentWidth"),
  PercentHeight("percentHeight"),
  PathArc("pathMotionArc"),
  Type("type")
}

internal enum class KeyCycleField(override val keyWord: String): ConstraintLayoutKeyWord {
  Period("period"),
  Offset("offset"),
  Phase("phase")
}