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
 * This file is created by fankes on 2026/4/8.
 */
package com.highcapable.adbrowser.frontend.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FileColumnSplitter(
    onDragDelta: (Float) -> Float,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    DraggableResizeHandle(
        orientation = Orientation.Horizontal,
        onDragDelta = onDragDelta,
        onDragStopped = onDragStopped,
        modifier = modifier
            .width(10.dp)
            .height(22.dp),
        indicatorThickness = 2.dp,
        indicatorPadding = Dp.Hairline
    )
}