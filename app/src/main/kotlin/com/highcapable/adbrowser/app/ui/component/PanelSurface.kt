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
 * This file is created by fankes on 2026/4/9.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding

@Composable
fun PanelSurface(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    shape: Shape = RoundedCornerShape(8.dp),
    padding: ComponentPadding = ComponentPadding(0.dp),
    clipContent: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AdbrowserTheme.colors
    val baseModifier = modifier
        .border(1.dp, colors.panelBorder, shape)
        .background(colors.panelBackground, shape)

    val preparedModifier = if (clipContent) baseModifier.clip(shape) else baseModifier

    Box(
        modifier = preparedModifier.padding(padding),
        contentAlignment = contentAlignment,
        content = content
    )
}