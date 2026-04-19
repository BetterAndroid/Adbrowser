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

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DevicePanePanel(
    devices: List<AndroidDeviceItem>,
    selectedDevice: AndroidDeviceItem?,
    listState: LazyListState,
    title: String,
    noDeviceMessage: String,
    onOpenActionMenu: (Offset) -> Unit,
    onRefresh: () -> Unit,
    onDeviceClick: (AndroidDeviceItem) -> Unit,
    popupHostCoordinates: () -> LayoutCoordinates?,
    onDeviceSecondaryClick: (AndroidDeviceItem, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var actionButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val offsetExtra = with(density) { 4.dp.toPx() }

    PanelSurface(
        modifier = modifier,
        padding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (devices.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "(${devices.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContentIconButton(
                        key = AppIcons.Plus,
                        outlined = true,
                        contentDescription = "Open Device Operations Menu",
                        modifier = Modifier.onGloballyPositioned { actionButtonCoordinates = it },
                        onClick = {
                            val host = popupHostCoordinates()
                            val button = actionButtonCoordinates
                            if (host == null || button == null) return@ContentIconButton

                            onOpenActionMenu(
                                host.localPositionOf(button, Offset.Zero) +
                                    Offset(x = 0f, y = button.size.height.toFloat() + offsetExtra)
                            )
                        }
                    )
                    ContentIconButton(
                        key = AppIcons.Refresh,
                        outlined = true,
                        contentDescription = "Refresh Device List",
                        onClick = onRefresh
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PanelSurface(
                modifier = Modifier.fillMaxSize(),
                clipContent = true
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(devices) { device ->
                        DeviceRow(
                            item = device,
                            selected = selectedDevice == device,
                            onClick = { onDeviceClick(device) },
                            popupHostCoordinates = popupHostCoordinates,
                            onSecondaryClick = { onDeviceSecondaryClick(device, it) },
                        )
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
                if (devices.isEmpty())
                    DeviceListHint(
                        message = noDeviceMessage,
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
    }
}