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
 * This file is created by fankes on 2026/4/15.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.app.ui.interaction.onPressRelease
import com.highcapable.adbrowser.app.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.app.ui.interaction.onSelectionPrimaryPress
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.LogEntryItem
import com.highcapable.adbrowser.core.logging.LogLevel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.colorPalette

@Composable
fun LogEntryRow(
    entry: LogEntryItem,
    selected: Boolean,
    timeWidth: Dp,
    levelWidth: Dp,
    categoryWidth: Dp,
    messageWidth: Dp,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val colors = AdbrowserTheme.colors

    val foreground = if (selected) Color.White else rememberLogLevelColor(entry.level)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSecondaryPress(pass = PointerEventPass.Initial, onSecondaryPress = onSecondaryClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(6.dp))
                .hoverable(interactionSource = interactionSource)
                .onSelectionPrimaryPress(onPressedChange = { pressed = it }) { modifiers ->
                    onPrimaryClick(modifiers.appendSelection, modifiers.rangeSelection)
                }
                .onPressRelease(
                    key = entry.id,
                    onPressedChange = { pressed = it }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            LogEntryCell(entry.timeText, timeWidth, foreground)
            Spacer(Modifier.width(10.dp))
            LogEntryCell(entry.levelText, levelWidth, foreground)
            Spacer(Modifier.width(10.dp))
            LogEntryCell(entry.category, categoryWidth, foreground)
            Spacer(Modifier.width(10.dp))
            LogEntryMessageCell(entry.message, messageWidth, foreground)
        }
        overlay()
    }
}

@Composable
private fun LogEntryCell(
    text: String,
    width: Dp,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LogEntryMessageCell(
    text: String,
    width: Dp,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        softWrap = true,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun rememberLogLevelColor(level: LogLevel): Color {
    val textColors = JewelTheme.globalColors.text

    val traceFallback = JewelTheme.colorPalette
        .grayOrNull(if (JewelTheme.isDark) 8 else 4)
        ?: textColors.normal
    val infoFallback = JewelTheme.colorPalette
        .greenOrNull(if (JewelTheme.isDark) 8 else 4)
        ?: textColors.info
    val warningFallback = JewelTheme.colorPalette
        .orangeOrNull(if (JewelTheme.isDark) 8 else 4)
        ?: JewelTheme.colorPalette.yellowOrNull(if (JewelTheme.isDark) 8 else 4)
        ?: textColors.warning
    val errorFallback = JewelTheme.colorPalette
        .redOrNull(if (JewelTheme.isDark) 8 else 4)
        ?: textColors.error

    return when (level) {
        LogLevel.Trace -> traceFallback
        LogLevel.Information -> infoFallback
        LogLevel.Warning -> warningFallback
        LogLevel.Error -> errorFallback
    }
}