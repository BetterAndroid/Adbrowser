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
 * This file is created by fankes on 2026/4/11.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.frontend.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import java.awt.Cursor

@Composable
fun DraggableResizeHandle(
    orientation: Orientation,
    onDragDelta: (Float) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorThickness: Dp = 3.dp,
    indicatorPadding: Dp = 6.dp
) {
    val colors = AdbrowserTheme.colors
    val density = LocalDensity.current

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var dragged by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val dragState = rememberDraggableState { deltaPx ->
        onDragDelta(with(density) { deltaPx.toDp().value })
    }

    val indicatorColor = when {
        pressed || dragged -> colors.primaryAccentPressed
        hovered -> colors.panelBorder
        else -> Color.Transparent
    }
    val pointerIcon = remember(orientation) {
        PointerIcon(
            Cursor.getPredefinedCursor(
                if (orientation == Orientation.Horizontal)
                    Cursor.E_RESIZE_CURSOR
                else Cursor.N_RESIZE_CURSOR
            )
        )
    }

    Box(
        modifier = modifier
            .pointerHoverIcon(pointerIcon)
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interactionSource = interactionSource)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .draggable(
                orientation = orientation,
                state = dragState,
                onDragStarted = { dragged = true },
                onDragStopped = {
                    dragged = false
                    onDragStopped()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Indicator(
            orientation = orientation,
            color = indicatorColor,
            thickness = indicatorThickness,
            padding = indicatorPadding
        )
    }
}

@Composable
private fun BoxScope.Indicator(
    orientation: Orientation,
    color: Color,
    thickness: Dp,
    padding: Dp
) {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .then(
                if (orientation == Orientation.Horizontal)
                    Modifier
                        .width(thickness)
                        .fillMaxHeight()
                        .padding(vertical = padding)
                else Modifier
                    .height(thickness)
                    .fillMaxWidth()
                    .padding(horizontal = padding)
            )
            .background(color, RoundedCornerShape(999.dp))
    )
}