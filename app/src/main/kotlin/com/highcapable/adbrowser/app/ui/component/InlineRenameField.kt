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
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

/**
 * Shared inline rename editor used by both list and icon file presentations.
 *
 * The editor commits on focus loss to match desktop file-manager expectations when the user clicks
 * elsewhere. Callers can decide whether a rejected confirmation should keep the editor alive and
 * restore focus, or close the editor first and surface the failure through a separate dialog.
 */
@Composable
fun InlineRenameField(
    state: TextFieldState,
    sessionKey: Any,
    onConfirm: (String) -> Boolean,
    onCancel: () -> Unit,
    shouldRestoreFocusOnConfirmFailure: () -> Boolean = { true },
    modifier: Modifier = Modifier,
    fontSize: TextUnit = AdbrowserTheme.DefaultItemFontSize,
    minWidth: Dp = 0.dp,
    maxWidth: Dp = Dp.Unspecified,
    contentPadding: ComponentPadding = ComponentPadding.None,
    onBoundsInRootChanged: (Rect) -> Unit = {},
    centeredMultiline: Boolean = false
) {
    val backgroundColor = if (JewelTheme.isDark) Color.Black else Color.White
    val textAreaStyle = AdbrowserTheme.undecoratedTextAreaStyle

    val focusRequester = remember(sessionKey) { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var hasFocusedOnce by remember(sessionKey) { mutableStateOf(false) }
    var completed by remember(sessionKey) { mutableStateOf(false) }

    fun restoreFocus() {
        coroutineScope.launch {
            focusRequester.requestFocus()
            state.edit { selectAll() }
        }
    }

    fun confirmRename() {
        if (completed) return

        val value = state.text.toString().trim()
        when {
            onConfirm(value) -> completed = true
            shouldRestoreFocusOnConfirmFailure() -> restoreFocus()
            else -> completed = true
        }
    }

    fun cancelRename() {
        if (completed) return

        completed = true
        onCancel()
    }

    LaunchedEffect(sessionKey) {
        focusRequester.requestFocus()
        state.edit { selectAll() }
    }

    val containerModifier = modifier
        .widthIn(min = minWidth, max = maxWidth)

    val inputModifier = Modifier
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            when {
                focusState.isFocused -> hasFocusedOnce = true
                hasFocusedOnce && !completed -> confirmRename()
            }
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            when (event.key) {
                Key.Enter,
                Key.NumPadEnter -> {
                    confirmRename()
                    true
                }
                Key.Escape -> {
                    cancelRename()
                    true
                }
                else -> false
            }
        }

    Box(
        modifier = containerModifier
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .onGloballyPositioned { coordinates ->
                onBoundsInRootChanged(coordinates.boundsInRoot())
            }
    ) {
        if (centeredMultiline)
            TextArea(
                state = state,
                modifier = inputModifier.padding(contentPadding),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2),
                textStyle = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.contentColor,
                    fontSize = fontSize,
                    textAlign = TextAlign.Center
                ),
                style = textAreaStyle,
                scrollbarStyle = null,
                undecorated = true
            )
        else
            TextField(
                state = state,
                modifier = inputModifier.padding(contentPadding),
                textStyle = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.contentColor,
                    fontSize = fontSize
                ),
                undecorated = true
            )
    }
}