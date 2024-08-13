package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.*

fun DisplayStyle.Companion.values() = listOf(
    DisplayStyle.Block,
    DisplayStyle.Inline,
    DisplayStyle.InlineBlock,
    DisplayStyle.Flex,
    DisplayStyle.LegacyInlineFlex,
    DisplayStyle.Grid,
    DisplayStyle.LegacyInlineGrid,
    DisplayStyle.FlowRoot,
    DisplayStyle.None,
    DisplayStyle.Contents,
    DisplayStyle.Table,
    DisplayStyle.TableRow,
    DisplayStyle.ListItem,
    DisplayStyle.Inherit,
    DisplayStyle.Initial,
    DisplayStyle.Unset
)

fun FlexDirection.Companion.values() = listOf(
    FlexDirection.Row,
    FlexDirection.RowReverse,
    FlexDirection.Column,
    FlexDirection.ColumnReverse
)

fun FlexWrap.Companion.values() = listOf(
    FlexWrap.Wrap,
    FlexWrap.Nowrap,
    FlexWrap.WrapReverse
)

fun JustifyContent.Companion.values() = listOf(
    JustifyContent.Center,
    JustifyContent.Start,
    JustifyContent.End,
    JustifyContent.FlexStart,
    JustifyContent.FlexEnd,
    JustifyContent.Left,
    JustifyContent.Right,
    JustifyContent.Normal,
    JustifyContent.SpaceBetween,
    JustifyContent.SpaceAround,
    JustifyContent.SpaceEvenly,
    JustifyContent.Stretch,
    JustifyContent.Inherit,
    JustifyContent.Initial,
    JustifyContent.Unset,
    JustifyContent.SafeCenter,
    JustifyContent.UnsafeCenter
)

fun AlignSelf.Companion.values() = listOf(
    AlignSelf.Auto,
    AlignSelf.Normal,
    AlignSelf.Center,
    AlignSelf.Start,
    AlignSelf.End,
    AlignSelf.SelfStart,
    AlignSelf.SelfEnd,
    AlignSelf.FlexStart,
    AlignSelf.FlexEnd,
    AlignSelf.Baseline,
    AlignSelf.Stretch,
    AlignSelf.SafeCenter,
    AlignSelf.UnsafeCenter,
    AlignSelf.Inherit,
    AlignSelf.Initial,
    AlignSelf.Unset
)

fun AlignItems.Companion.values() = listOf(
    AlignItems.Normal,
    AlignItems.Stretch,
    AlignItems.Center,
    AlignItems.Start,
    AlignItems.End,
    AlignItems.FlexStart,
    AlignItems.FlexEnd,
    AlignItems.Baseline,
    AlignItems.SafeCenter,
    AlignItems.UnsafeCenter,
    AlignItems.Inherit,
    AlignItems.Initial,
    AlignItems.Unset
)

fun AlignContent.Companion.values() = listOf(
    AlignContent.Center,
    AlignContent.Start,
    AlignContent.End,
    AlignContent.FlexStart,
    AlignContent.FlexEnd,
    AlignContent.Baseline,
    AlignContent.SafeCenter,
    AlignContent.UnsafeCenter,
    AlignContent.SpaceBetween,
    AlignContent.SpaceAround,
    AlignContent.SpaceEvenly,
    AlignContent.Stretch,
    AlignContent.Inherit,
    AlignContent.Initial,
    AlignContent.Unset
)

fun Position.Companion.values() = listOf(
    Position.Static,
    Position.Relative,
    Position.Absolute,
    Position.Sticky,
    Position.Fixed
)

fun VisibilityStyle.Companion.values() = listOf(
    VisibilityStyle.Visible,
    VisibilityStyle.Hidden,
    VisibilityStyle.Collapse,
    VisibilityStyle.Inherit,
    VisibilityStyle.Initial,
    VisibilityStyle.Revert,
    VisibilityStyle.RevertLayer,
    VisibilityStyle.Unset
)
