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
 * This file is created by fankes on 2026/4/22.
 */
@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.foundation.rememberInlineRenameWidth
import com.highcapable.adbrowser.app.ui.interaction.PointerBoundsGuard
import com.highcapable.adbrowser.app.ui.interaction.onRenameActivationInput
import com.highcapable.adbrowser.app.ui.interaction.trackBoundsInRoot
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.model.DeviceFileItem
import org.jetbrains.jewel.ui.component.Text

@Composable
fun FileListNameSection(
    item: DeviceFileItem,
    displayName: String,
    selected: Boolean,
    isInlineRenaming: Boolean,
    canTapLabelToRename: Boolean,
    inlineRenameInput: TextFieldState,
    nameWidth: Dp,
    labelBoundsGuard: PointerBoundsGuard,
    editorBoundsGuard: PointerBoundsGuard,
    shouldHandlePrimaryInteraction: () -> Boolean,
    onBeginInlineRename: () -> Unit,
    onOpen: () -> Unit,
    onConfirmInlineRename: (String) -> Boolean,
    onCancelInlineRename: () -> Unit
) {
    val fontSize = AdbrowserTheme.DefaultItemFontSize
    val foreground = if (selected) Color.White else Color.Unspecified

    val nameContentWidth = (nameWidth - 24.dp).coerceAtLeast(80.dp)
    val inlineRenameWidth = rememberInlineRenameWidth(
        text = displayName,
        maxWidth = nameContentWidth,
        maxLines = 1,
        softWrap = false,
        extraWidth = 12.dp
    )
    val labelSpacing = if (isInlineRenaming) 4.dp else 8.dp

    Row(
        modifier = Modifier.width(nameWidth),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileEntryIcon(
            item = item,
            selected = selected
        )
        Spacer(Modifier.width(labelSpacing))
        Box(
            modifier = Modifier
                .width(nameContentWidth)
                .trackBoundsInRoot(labelBoundsGuard)
                .onRenameActivationInput(
                    enabled = canTapLabelToRename && !isInlineRenaming,
                    key = item,
                    shouldHandle = shouldHandlePrimaryInteraction,
                    onRename = onBeginInlineRename,
                    onOpen = onOpen
                )
        ) {
            Text(
                text = displayName,
                color = foreground,
                fontSize = fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isInlineRenaming)
                InlineRenameField(
                    state = inlineRenameInput,
                    sessionKey = item.path to item.name,
                    onConfirm = onConfirmInlineRename,
                    onCancel = onCancelInlineRename,
                    modifier = Modifier.width(inlineRenameWidth),
                    contentPadding = PaddingValues(4.dp),
                    onBoundsInRootChanged = editorBoundsGuard::updateAvoidedBounds
                )
        }
    }
}

@Composable
fun FileIconNameSection(
    item: DeviceFileItem,
    displayName: String,
    selected: Boolean,
    isInlineRenaming: Boolean,
    canTapLabelToRename: Boolean,
    inlineRenameInput: TextFieldState,
    editorBoundsGuard: PointerBoundsGuard,
    modifier: Modifier = Modifier,
    shouldHandlePrimaryInteraction: () -> Boolean = { true },
    onBeginInlineRename: () -> Unit,
    onOpen: () -> Unit,
    onConfirmInlineRename: (String) -> Boolean,
    onCancelInlineRename: () -> Unit
) {
    val fontSize = AdbrowserTheme.DefaultItemFontSize
    val foreground = if (selected) Color.White else Color.Unspecified

    Box(
        modifier = modifier.onRenameActivationInput(
            enabled = canTapLabelToRename && !isInlineRenaming,
            key = item,
            shouldHandle = shouldHandlePrimaryInteraction,
            onRename = onBeginInlineRename,
            onOpen = onOpen
        )
    ) {
        BoxWithConstraints {
            val inlineRenameWidth = rememberInlineRenameWidth(
                text = displayName,
                maxWidth = maxWidth,
                maxLines = 2,
                softWrap = true
            )

            Text(
                text = displayName,
                color = foreground,
                fontSize = fontSize,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (isInlineRenaming)
                InlineRenameField(
                    state = inlineRenameInput,
                    sessionKey = item.path to item.name,
                    onConfirm = onConfirmInlineRename,
                    onCancel = onCancelInlineRename,
                    modifier = Modifier.width(inlineRenameWidth),
                    onBoundsInRootChanged = editorBoundsGuard::updateAvoidedBounds,
                    centeredMultiline = true
                )
        }
    }
}