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
 * This file is created by fankes on 2026/5/7.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import kotlin.math.roundToInt

@Composable
fun DeviceCard(
    devices: List<AndroidDeviceItem>,
    selectedDevice: AndroidDeviceItem,
    onDeviceSelected: (AndroidDeviceItem) -> Unit,
    menuDirection: DeviceCardMenuDirection = DeviceCardMenuDirection.Bottom,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val statusColor = if (selectedDevice.isOnline) DeviceOnlineStatusColor else DeviceOfflineStatusColor
    val menuVerticalOffsetPx = with(density) { 4.dp.roundToPx() }
    var buttonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuAnchor by remember { mutableStateOf<DeviceCardMenuAnchor?>(null) }
    val isMenuVisible = menuAnchor != null

    LaunchedEffect(isMenuVisible) {
        if (isMenuVisible) focusRequester.requestFocus()
        else {
            focusManager.clearFocus(force = true)
            focusRequester.freeFocus()
        }
    }

    ActionButton(
        onClick = {
            menuAnchor = buttonCoordinates?.boundsInRoot()?.let {
                DeviceCardMenuAnchor(
                    left = it.left.roundToInt(),
                    right = it.right.roundToInt(),
                    top = it.top.roundToInt(),
                    bottom = it.bottom.roundToInt()
                )
            }
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onGloballyPositioned { buttonCoordinates = it },
        contentPadding = ComponentPadding.Zero
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.width(14.dp)) {
                ContentIcon(
                    key = AppIcons.Device,
                    contentDescription = "Device Icon",
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = selectedDevice.brandModel.ifBlank { selectedDevice.serial },
                fontSize = 12.sp
            )
        }
    }

    menuAnchor?.let { anchor ->
        PopupMenu(
            onDismissRequest = { menuAnchor = null; true },
            popupPositionProvider = remember(anchor, menuVerticalOffsetPx, menuDirection) {
                DeviceCardMenuPositionProvider(anchor, menuVerticalOffsetPx, menuDirection)
            },
            popupProperties = PopupProperties(focusable = false)
        ) {
            devices.forEach { device ->
                val isSelected = device == selectedDevice

                selectableItem(
                    iconKey = if (isSelected) AllIconsKeys.Actions.Checked else null,
                    selected = isSelected,
                    onClick = {
                        menuAnchor = null
                        if (!isSelected) onDeviceSelected(device)
                    }
                ) {
                    Text(device.brandModel.ifBlank { device.serial })
                }
            }
        }
    }
}

enum class DeviceCardMenuDirection {
    Top,
    Bottom
}

private data class DeviceCardMenuAnchor(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)

private class DeviceCardMenuPositionProvider(
    private val anchor: DeviceCardMenuAnchor,
    private val verticalOffsetPx: Int,
    private val direction: DeviceCardMenuDirection
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)

        return IntOffset(
            x = when (direction) {
                DeviceCardMenuDirection.Bottom -> (anchor.right - popupContentSize.width).coerceIn(0, maxX)
                DeviceCardMenuDirection.Top -> (anchor.right - popupContentSize.width).coerceIn(0, maxX)
            },
            y = when (direction) {
                DeviceCardMenuDirection.Bottom -> (anchor.bottom + verticalOffsetPx).coerceIn(0, maxY)
                DeviceCardMenuDirection.Top -> (anchor.top - popupContentSize.height - verticalOffsetPx).coerceIn(0, maxY)
            }
        )
    }
}