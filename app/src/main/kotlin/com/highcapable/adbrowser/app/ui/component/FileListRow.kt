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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.interaction.avoidingBounds
import com.highcapable.adbrowser.app.ui.interaction.onPressRelease
import com.highcapable.adbrowser.app.ui.interaction.onSecondaryPress
import com.highcapable.adbrowser.app.ui.interaction.onSelectionPrimaryPress
import com.highcapable.adbrowser.app.ui.interaction.rememberPointerBoundsGuard
import com.highcapable.adbrowser.app.ui.interaction.trackPointerContainer
import com.highcapable.adbrowser.app.ui.modifier.resolveListItemBackground
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import org.jetbrains.jewel.ui.component.Text

@Composable
fun FileListRow(
    horizontalScrollState: ScrollState,
    item: DeviceFileItem,
    displayName: String,
    selected: Boolean,
    canTapLabelToRename: Boolean,
    isInlineRenaming: Boolean,
    inlineRenameInput: TextFieldState,
    nameWidth: Dp,
    sizeWidth: Dp,
    modifiedWidth: Dp,
    permissionWidth: Dp,
    onPrimaryClick: (appendSelection: Boolean, rangeSelection: Boolean) -> Unit,
    onDoubleClick: () -> Unit,
    onSecondaryClick: (Offset) -> Unit,
    shouldHandlePrimaryInteraction: () -> Boolean = { true },
    onBeginInlineRename: () -> Unit,
    onConfirmInlineRename: (String) -> Boolean,
    onCancelInlineRename: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val colors = AdbrowserTheme.colors

    val contentWidth = nameWidth + sizeWidth + modifiedWidth + permissionWidth + 30.dp
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val hovered by interactionSource.collectIsHoveredAsState()
    var pressed by remember { mutableStateOf(false) }
    val labelBoundsGuard = rememberPointerBoundsGuard(item)
    val editorBoundsGuard = rememberPointerBoundsGuard(item)
    val background = resolveListItemBackground(
        colors = colors,
        selected = selected,
        hovered = hovered,
        pressed = pressed
    )
    val foreground = if (selected) Color.White else Color.Unspecified
    val rowVerticalPadding = if (isInlineRenaming) 4.dp else 8.dp
    val shouldAvoidLabelBounds = canTapLabelToRename && !isInlineRenaming
    val shouldHandleSecondaryPress: (Offset) -> Boolean =
        if (isInlineRenaming) editorBoundsGuard::shouldHandle else { _ -> true }
    val shouldHandlePrimaryPress: (Offset) -> Boolean =
        if (shouldAvoidLabelBounds) { position ->
            labelBoundsGuard.shouldHandle(position) && shouldHandlePrimaryInteraction()
        } else { _ ->
            shouldHandlePrimaryInteraction()
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .trackPointerContainer(labelBoundsGuard)
            .trackPointerContainer(editorBoundsGuard)
            .onSecondaryPress(
                pass = PointerEventPass.Initial,
                shouldHandle = shouldHandleSecondaryPress
            ) { position ->
                onSecondaryClick(position)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .hoverable(interactionSource = interactionSource)
                .avoidingBounds(labelBoundsGuard) {
                    if (!isInlineRenaming)
                        onSelectionPrimaryPress(
                            shouldHandle = shouldHandlePrimaryPress,
                            onPressedChange = { pressed = it }
                        ) { modifiers ->
                            onPrimaryClick(modifiers.appendSelection, modifiers.rangeSelection)
                        }.onPressRelease(
                            key = item,
                            shouldHandle = shouldHandlePrimaryPress,
                            onPressedChange = { pressed = it },
                            onDoubleTap = currentOnDoubleClick
                        )
                    else this
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
                        .padding(horizontal = 10.dp, vertical = rowVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileListNameSection(
                        item = item,
                        displayName = displayName,
                        selected = selected,
                        isInlineRenaming = isInlineRenaming,
                        canTapLabelToRename = canTapLabelToRename,
                        inlineRenameInput = inlineRenameInput,
                        nameWidth = nameWidth,
                        labelBoundsGuard = labelBoundsGuard,
                        editorBoundsGuard = editorBoundsGuard,
                        shouldHandlePrimaryInteraction = shouldHandlePrimaryInteraction,
                        onBeginInlineRename = onBeginInlineRename,
                        onOpen = currentOnDoubleClick,
                        onConfirmInlineRename = onConfirmInlineRename,
                        onCancelInlineRename = onCancelInlineRename
                    )
                    Spacer(Modifier.width(10.dp))
                    FileRowText(text = item.friendlySizeText, width = sizeWidth, color = foreground)
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
    val fontSize = AdbrowserTheme.DefaultItemFontSize

    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}