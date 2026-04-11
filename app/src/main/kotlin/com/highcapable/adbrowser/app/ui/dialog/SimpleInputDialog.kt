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
 * This file is created by fankes on 2026/4/5.
 */
@file:Suppress("AssignedValueIsNeverRead")

package com.highcapable.adbrowser.app.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.component.DialogActionRow
import com.highcapable.adbrowser.app.ui.component.DialogScaffold
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun SimpleInputDialog(
    title: String,
    prompt: String,
    confirmText: String,
    cancelText: String,
    invalidInputText: String,
    onCloseRequest: () -> Unit,
    onConfirm: (String) -> Boolean,
    initialValue: String = "",
    selectAllOnOpen: Boolean = true,
    validate: (String) -> Boolean = { it.isNotBlank() }
) {
    val state = remember(initialValue) { TextFieldState(initialValue) }
    val focusRequester = remember { FocusRequester() }
    var showInvalid by remember { mutableStateOf(false) }

    LaunchedEffect(focusRequester, initialValue, selectAllOnOpen) {
        focusRequester.requestFocus()
        if (selectAllOnOpen && initialValue.isNotBlank()) state.edit { selectAll() }
    }

    DialogScaffold(
        title = title,
        onCloseRequest = onCloseRequest,
        width = 480.dp
    ) {
        Text(prompt)

        TextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .height(AdbrowserTheme.DefaultTextFieldHeight)
        )

        DialogActionRow(
            primaryText = confirmText,
            onPrimary = {
                val value = state.text.toString().trim()
                if (!validate(value)) {
                    showInvalid = true
                    return@DialogActionRow
                }

                showInvalid = false
                if (onConfirm(value)) onCloseRequest()
            },
            secondaryText = cancelText,
            onSecondary = onCloseRequest,
            leadingText = if (showInvalid) invalidInputText else "",
            leadingTextColor = JewelTheme.globalColors.text.error
        )
    }
}