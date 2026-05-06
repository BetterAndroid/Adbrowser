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
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun LogListHeader(
    horizontalScrollState: ScrollState,
    timeWidth: Dp,
    levelWidth: Dp,
    categoryWidth: Dp,
    messageWidth: Dp,
    onResizeTimeAndLevel: (Float) -> Float,
    onResizeLevelAndCategory: (Float) -> Float,
    onResizeCategoryAndMessage: (Float) -> Float,
    onResizeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val density = LocalDensity.current
    val contentWidth = timeWidth + levelWidth + categoryWidth + messageWidth + 30.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.subtleControlBackground, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .horizontalScroll(horizontalScrollState)
        ) {
            Row(
                modifier = Modifier
                    .width(contentWidth)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogHeaderText(text = strings.logsHeaderTime, width = timeWidth)
                FileColumnSplitter(
                    onDragDelta = { deltaPx ->
                        val consumedDp = onResizeTimeAndLevel(with(density) { deltaPx.toDp().value })
                        with(density) { consumedDp.dp.toPx() }
                    },
                    onDragStopped = onResizeFinished
                )
                LogHeaderText(text = strings.logsHeaderLevel, width = levelWidth)
                FileColumnSplitter(
                    onDragDelta = { deltaPx ->
                        val consumedDp = onResizeLevelAndCategory(with(density) { deltaPx.toDp().value })
                        with(density) { consumedDp.dp.toPx() }
                    },
                    onDragStopped = onResizeFinished
                )
                LogHeaderText(text = strings.logsHeaderCategory, width = categoryWidth)
                FileColumnSplitter(
                    onDragDelta = { deltaPx ->
                        val consumedDp = onResizeCategoryAndMessage(with(density) { deltaPx.toDp().value })
                        with(density) { consumedDp.dp.toPx() }
                    },
                    onDragStopped = onResizeFinished
                )
                LogHeaderText(text = strings.logsHeaderMessage, width = messageWidth)
            }
        }
    }
}

@Composable
private fun LogHeaderText(
    text: String,
    width: Dp
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(vertical = 8.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
}