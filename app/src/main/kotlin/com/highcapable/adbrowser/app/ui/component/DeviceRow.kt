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
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.AndroidDeviceItem
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DeviceRow(
    item: AndroidDeviceItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AdbrowserTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val foreground = if (selected) Color.White else Color.Unspecified
    val statusColor = if (item.isOnline) OnlineStatusColor else OfflineStatusColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .hoverable(interactionSource = interactionSource)
            .pointerInput(item.serial) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onClick()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(Modifier.width(8.dp))
            ContentIcon(
                key = AppIcons.Device,
                selected = selected,
                contentDescription = "Device Icon",
                tint = colors.primaryAccent
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.brandModel,
                color = foreground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.alpha(0.75f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.systemVersion.isNotBlank()) {
                    Text(
                        text = item.systemVersion,
                        color = foreground,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "|",
                        color = foreground,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .alpha(0.75f)
                            .padding(horizontal = 3.dp)
                    )
                }
                Text(
                    text = item.serial,
                    color = foreground,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val OnlineStatusColor = Color(0xFF2DB455)
private val OfflineStatusColor = Color(0xFFE46868)