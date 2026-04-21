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
 * This file is created by fankes on 2026/4/21.
 */
package com.highcapable.adbrowser.app.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Measures the visual width of inline-rename text using the app's themed text style.
 *
 * This is a UI-foundation helper rather than a platform utility: it depends on Compose density,
 * text measurement, and Jewel theme state, and is shared by list/icon rename surfaces.
 */
@Composable
fun rememberInlineRenameWidth(
    text: String,
    fontSize: TextUnit = AdbrowserTheme.DefaultItemFontSize,
    maxWidth: Dp,
    maxLines: Int,
    softWrap: Boolean,
    extraWidth: Dp = 0.dp
): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = JewelTheme.defaultTextStyle.copy(fontSize = fontSize)

    return remember(text, maxWidth, maxLines, softWrap, extraWidth, density, textMeasurer, textStyle) {
        with(density) {
            val measuredText = textMeasurer.measure(
                text = AnnotatedString(text.ifEmpty { " " }),
                style = textStyle,
                overflow = TextOverflow.Ellipsis,
                softWrap = softWrap,
                maxLines = maxLines,
                constraints = Constraints(maxWidth = maxWidth.roundToPx())
            )

            (measuredText.size.width.toDp() + extraWidth).coerceAtMost(maxWidth)
        }
    }
}