/*
 * Adbrowser - A modern cross-platform Android file manager powered by ADB.
 * Copyright (C) 2019 HighCapable
 * https://github.com/BetterAndroid/Adbrowser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2026/4/5.
 */
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FixedWidthHorizontalSplitLayout(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    firstPaneWidth: Dp,
    onFirstPaneWidthChange: (Dp) -> Unit,
    onFirstPaneWidthChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    firstPaneMinWidth: Dp = 240.dp,
    secondPaneMinWidth: Dp = 560.dp,
    dividerWidth: Dp = 10.dp
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val minFirstPanePx = with(density) { firstPaneMinWidth.toPx() }
        val minSecondPanePx = with(density) { secondPaneMinWidth.toPx() }
        val dividerWidthPx = with(density) { dividerWidth.toPx() }

        // The first pane width is clamped against the current total width so window resizing
        // cannot push either side below its minimum size.
        val maxFirstPanePx = (totalWidthPx - dividerWidthPx - minSecondPanePx).coerceAtLeast(minFirstPanePx)
        val firstPaneWidthPx = with(density) { firstPaneWidth.toPx() }.coerceIn(minFirstPanePx, maxFirstPanePx)

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(with(density) { firstPaneWidthPx.toDp() })
                    .fillMaxHeight()
            ) { first() }
            DraggableResizeHandle(
                orientation = Orientation.Horizontal,
                onDragDelta = { deltaPx ->
                    val updatedPx = (firstPaneWidthPx + deltaPx)
                        .coerceIn(minFirstPanePx, maxFirstPanePx)
                    onFirstPaneWidthChange(with(density) { updatedPx.toDp() })

                    // Return the actually consumed delta so the handle can preserve any leftover
                    // overflow when dragging against a min/max boundary.
                    updatedPx - firstPaneWidthPx
                },
                onDragStopped = onFirstPaneWidthChangeFinished,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dividerWidth)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .widthIn(min = secondPaneMinWidth)
            ) { second() }
        }
    }
}