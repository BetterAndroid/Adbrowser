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
package com.highcapable.adbrowser.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.app.ui.modifier.edgeBorder
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import com.highcapable.adbrowser.core.common.utils.BuildVersion
import org.jetbrains.jewel.ui.component.Text

@Composable
fun StatusBar(
    text: String,
    currentDevice: AndroidDeviceItem?,
    devices: List<AndroidDeviceItem>,
    onDeviceSelected: (AndroidDeviceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    var lastVisibleDevice by remember { mutableStateOf(currentDevice) }

    if (currentDevice != null) lastVisibleDevice = currentDevice
    val animatedDevice = currentDevice ?: lastVisibleDevice

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.hintBackground)
            .edgeBorder(
                color = colors.hintBorder,
                top = true,
                bottom = false,
                start = false,
                end = false
            )
            .height(34.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        AnimatedVisibility(
            visible = currentDevice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeviceCard(
                    devices = devices,
                    selectedDevice = checkNotNull(animatedDevice),
                    onDeviceSelected = onDeviceSelected,
                    menuDirection = DeviceCardMenuDirection.Top,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
                Spacer(Modifier.width(5.dp))
                Delimiter()
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(
            text = BuildVersion.TEXT,
            color = colors.pathBreadcrumbForeground,
            fontSize = 12.sp
        )
    }
}