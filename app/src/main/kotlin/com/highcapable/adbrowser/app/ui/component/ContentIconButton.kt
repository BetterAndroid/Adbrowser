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
 * This file is created by fankes on 2026/4/4.
 */
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.icon.IconKey

@Composable
fun ContentIconButton(
    key: IconKey,
    contentDescription: String,
    enabled: Boolean = true,
    outlined: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (outlined)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            style = AdbrowserTheme.outlineButtonStyle(ComponentPadding.None),
            modifier = modifier.size(DefaultContentSize)
        ) {
            ContentIcon(
                key = key,
                enabled = enabled,
                contentDescription = contentDescription,
                modifier = Modifier.size(DefaultIconSize)
            )
        }
    else
        DefaultButton(
            onClick = onClick,
            enabled = enabled,
            style = AdbrowserTheme.defaultButtonStyle(ComponentPadding.None),
            modifier = modifier.size(DefaultContentSize)
        ) {
            ContentIcon(
                key = key,
                enabled = enabled,
                contentDescription = contentDescription,
                modifier = Modifier.size(DefaultIconSize)
            )
        }
}

private val DefaultContentSize = 30.dp
private val DefaultIconSize = 16.dp