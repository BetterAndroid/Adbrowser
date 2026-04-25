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
package com.highcapable.adbrowser.app.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.component.ButtonActionRow
import com.highcapable.adbrowser.app.ui.component.NormalIcon
import com.highcapable.adbrowser.app.ui.dialog.base.DialogScaffold
import com.highcapable.adbrowser.app.ui.interaction.ProvidePrimaryAction
import org.jetbrains.jewel.ui.component.Text
import java.awt.Window

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onCloseRequest: () -> Unit,
    ownerWindow: Window? = null,
    cancelText: String? = null,
    tertiaryText: String? = null,
    icon: ConfirmDialogIcon = ConfirmDialogIcon.Information,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    tertiaryEnabled: Boolean = true,
    checkboxText: String? = null,
    checkboxChecked: Boolean = false,
    checkboxEnabled: Boolean = true,
    onCheckboxCheckedChange: ((Boolean) -> Unit)? = null,
    onTertiary: (() -> Unit)? = null,
    onConfirm: () -> Boolean
) {
    DialogScaffold(
        title = title,
        onCloseRequest = onCloseRequest,
        ownerWindow = ownerWindow
    ) {
        ProvidePrimaryAction(window) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NormalIcon(
                    key = when (icon) {
                        ConfirmDialogIcon.Information -> AppIcons.DialogInformation
                        ConfirmDialogIcon.Warning -> AppIcons.DialogWarning
                        ConfirmDialogIcon.Question -> AppIcons.DialogQuestion
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(message)
            }
            ButtonActionRow(
                primaryText = confirmText,
                onPrimary = {
                    if (onConfirm()) onCloseRequest()
                },
                primaryEnabled = confirmEnabled,
                secondaryText = cancelText,
                onSecondary = onCloseRequest,
                secondaryEnabled = cancelEnabled,
                tertiaryText = tertiaryText,
                onTertiary = onTertiary,
                tertiaryEnabled = tertiaryEnabled,
                checkboxText = checkboxText,
                checkboxChecked = checkboxChecked,
                checkboxEnabled = checkboxEnabled,
                onCheckboxCheckedChange = onCheckboxCheckedChange
            )
        }
    }
}

enum class ConfirmDialogIcon { 
    Information,
    Warning,
    Question
}