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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.frontend.ui.assets.AppIcons
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.IconKey

@Composable
fun FileListHint(
    iconKey: IconKey,
    message: String,
    modifier: Modifier = Modifier
) {
    StageHint(
        iconKey = iconKey,
        message = message,
        modifier = modifier
    )
}

@Composable
fun DeviceListHint(
    message: String,
    modifier: Modifier = Modifier
) {
    StageHint(
        iconKey = AppIcons.Devices,
        message = message,
        modifier = modifier
    )
}

@Composable
private fun StageHint(
    iconKey: IconKey,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    Column(
        modifier = modifier.alpha(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContentIcon(
            key = iconKey,
            contentDescription = message,
            modifier = Modifier.size(60.dp),
            tint = colors.primaryAccent
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.primaryAccent,
            fontSize = 16.sp
        )
    }
}