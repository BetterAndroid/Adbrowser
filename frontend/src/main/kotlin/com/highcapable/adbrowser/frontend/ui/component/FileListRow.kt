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

package com.highcapable.adbrowser.frontend.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.adbrowser.frontend.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.frontend.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.model.DeviceFileItem
import org.jetbrains.jewel.ui.component.Text

@Composable
fun FileListRow(
    horizontalScrollState: ScrollState,
    item: DeviceFileItem,
    selected: Boolean,
    nameWidth: Dp,
    sizeWidth: Dp,
    modifiedWidth: Dp,
    permissionWidth: Dp,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val colors = AdbrowserTheme.colors
    val contentWidth = nameWidth + sizeWidth + modifiedWidth + permissionWidth + 30.dp
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSecondaryPress(pass = PointerEventPass.Initial, onSecondaryPress = onSecondaryClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .hoverable(interactionSource = interactionSource)
                .onPointerEvent(PointerEventType.Press, pass = PointerEventPass.Initial) { event ->
                    if (!event.buttons.isPrimaryPressed) return@onPointerEvent

                    event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return@onPointerEvent
                    pressed = true
                    onPrimaryClick(
                        event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed,
                        event.keyboardModifiers.isShiftPressed
                    )
                }
                .pointerInput(item) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            pressed = false
                        },
                        onDoubleTap = { onDoubleClick() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
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
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(nameWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FileEntryIcon(
                            item = item,
                            selected = selected,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            color = foreground,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    FileRowText(text = item.sizeText, width = sizeWidth, color = foreground)
                    Spacer(Modifier.width(10.dp))
                    FileRowText(text = item.modifiedText, width = modifiedWidth, color = foreground)
                    Spacer(Modifier.width(10.dp))
                    FileRowText(text = item.permission, width = permissionWidth, color = foreground)
                }
            }
        }
        overlay()
    }
}

@Composable
private fun FileRowText(
    text: String,
    width: Dp,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}