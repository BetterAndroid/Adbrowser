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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.assets.AppIcons
import com.highcapable.adbrowser.app.ui.component.ContentIcon
import com.highcapable.adbrowser.app.ui.component.PanelSurface
import com.highcapable.adbrowser.app.ui.dialog.ConfirmDialogIcon.Warning
import com.highcapable.adbrowser.app.ui.dialog.base.DialogScaffold
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel.PermissionAccess
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel.PermissionScope
import com.highcapable.adbrowser.app.ui.vm.MainStageModel
import com.highcapable.adbrowser.app.ui.vm.model.FileEntrySnapshot
import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.common.fs.FilePermission
import com.highcapable.adbrowser.core.common.utils.OsType
import com.highcapable.adbrowser.core.common.utils.extension.formatWithArgs
import com.highcapable.betterandroid.compose.extension.ui.ComponentPadding
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.awt.Window

@Composable
fun FilePropertiesDialog(
    snapshot: FileEntrySnapshot,
    onCloseRequest: () -> Unit,
    loadPermission: () -> OperationResult<FilePermission.Info>,
    applyPermission: (String) -> OperationResult<FilePermission.Info>,
    ownerWindow: Window? = null
) {
    val colors = AdbrowserTheme.colors

    var closeConfirmMessageRaw by remember { mutableStateOf<String?>(null) }
    val viewModel = remember(snapshot, loadPermission, applyPermission) {
        FilePropertiesDialogModel(
            snapshot = snapshot,
            loadPermissionAction = loadPermission,
            applyPermissionAction = applyPermission
        )
    }

    val invalidPermissionText = strings.dialogPropertiesInvalidPermission
    val unknownErrorText = strings.commonUnknownError
    val resolveDialogStatusText: (String?) -> String = { raw ->
        val value = raw.orEmpty()
        when {
            value.isBlank() -> unknownErrorText
            value == MainStageModel.INVALID_PERMISSION_TOKEN -> invalidPermissionText
            value == MainStageModel.UNKNOWN_ERROR_TOKEN -> unknownErrorText
            else -> value
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.initialize()
    }

    LaunchedEffect(viewModel.modeState) {
        snapshotFlow { viewModel.modeState.text.toString().trim() }
            .distinctUntilChanged()
            .collect(viewModel::onModeInputChanged)
    }

    DialogScaffold(
        title = strings.dialogPropertiesTitle,
        onCloseRequest = {
            when (val result = viewModel.close()) {
                FilePropertiesDialogModel.CloseResult.Close -> onCloseRequest()
                is FilePropertiesDialogModel.CloseResult.ConfirmDiscard ->
                    closeConfirmMessageRaw = resolveDialogStatusText(result.messageRaw)
            }
        },
        ownerWindow = ownerWindow,
        verticalSpacing = 10.dp,
        contentPadding = ComponentPadding(12.dp),
        // Make the dialog slightly wider on non-macOS platforms to
        // accommodate the permission mode input field, which is hidden on macOS.
        width = if (OsType.isMacOS) 300.dp else 320.dp
    ) {
        PanelSurface(
            modifier = Modifier.fillMaxWidth(),
            padding = ComponentPadding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContentIcon(
                    key = when {
                        snapshot.isDirectory && snapshot.isSymlink -> AppIcons.LinkedFolder
                        snapshot.isDirectory -> AppIcons.Folder
                        snapshot.isSymlink -> AppIcons.LinkedFile
                        else -> AppIcons.File
                    },
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    val state = remember(snapshot.name) { TextFieldState(snapshot.name) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            state = state,
                            readOnly = true,
                            undecorated = true,
                            textStyle = JewelTheme.defaultTextStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = snapshot.friendlySizeText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = strings.dialogPropertiesFieldModified.formatWithArgs(snapshot.friendlyModifiedAtText),
                        color = colors.pathBreadcrumbForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
        }
        PanelSurface(
            modifier = Modifier.fillMaxWidth(),
            padding = ComponentPadding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PropertyRow(
                    strings.dialogPropertiesFieldPath,
                    snapshot.fullPath,
                    selectableValue = true
                )
                PropertyRow(
                    strings.dialogPropertiesFieldType,
                    when {
                        snapshot.isSymlink -> strings.dialogPropertiesTypeSymlink
                        snapshot.isDirectory -> strings.dialogPropertiesTypeDirectory
                        else -> strings.dialogPropertiesTypeFile
                    }
                )
            }
        }
        PanelSurface(
            modifier = Modifier.fillMaxWidth(),
            padding = ComponentPadding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(ItemHorizontalWidth),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.dialogPropertiesFieldPermission,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (viewModel.hasPermissionChanges)
                            Text(
                                text = "*",
                                fontWeight = FontWeight.SemiBold,
                                color = JewelTheme.globalColors.text.error
                            )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = viewModel.symbolicPermission,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .width(80.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextField(
                            state = viewModel.modeState,
                            inputTransformation = InputTransformation.maxLength(PermissionModeMaxLength),
                            modifier = Modifier
                                .width(80.dp)
                                .height(AdbrowserTheme.DefaultTextFieldHeight)
                        )
                    }
                }
                PermissionBitsRow(
                    title = strings.dialogPropertiesOwner,
                    scope = PermissionScope.Owner,
                    viewModel = viewModel
                )
                PermissionBitsRow(
                    title = strings.dialogPropertiesGroup,
                    scope = PermissionScope.Group,
                    viewModel = viewModel
                )
                PermissionBitsRow(
                    title = strings.dialogPropertiesOther,
                    scope = PermissionScope.Other,
                    viewModel = viewModel
                )
            }
        }

        closeConfirmMessageRaw?.let { message ->
            ConfirmDialog(
                icon = Warning,
                title = strings.dialogPropertiesConfirmDialogTitle,
                message = message,
                confirmText = strings.dialogCommonOk,
                cancelText = strings.dialogCommonCancel,
                onCloseRequest = { closeConfirmMessageRaw = null },
                ownerWindow = window,
                onConfirm = {
                    onCloseRequest()
                    true
                }
            )
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    selectableValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(ItemHorizontalWidth),
            fontWeight = FontWeight.SemiBold
        )
        if (selectableValue) {
            val state = remember(value) { TextFieldState(value) }
            TextField(
                state = state,
                readOnly = true,
                undecorated = true,
                modifier = Modifier.weight(1f)
            )
        } else Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PermissionBitsRow(
    title: String,
    scope: PermissionScope,
    viewModel: FilePropertiesDialogModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.width(ItemHorizontalWidth),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.offset(x = (-4).dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CheckboxRow(
                checked = viewModel.isPermissionBitChecked(scope, PermissionAccess.Read),
                onCheckedChange = { viewModel.onPermissionBitChanged(scope, PermissionAccess.Read, it) }
            ) { Text(strings.dialogPropertiesRead) }
            CheckboxRow(
                checked = viewModel.isPermissionBitChecked(scope, PermissionAccess.Write),
                onCheckedChange = { viewModel.onPermissionBitChanged(scope, PermissionAccess.Write, it) }
            ) { Text(strings.dialogPropertiesWrite) }
            CheckboxRow(
                checked = viewModel.isPermissionBitChecked(scope, PermissionAccess.Execute),
                onCheckedChange = { viewModel.onPermissionBitChanged(scope, PermissionAccess.Execute, it) }
            ) { Text(strings.dialogPropertiesExecute) }
        }
    }
}

private val ItemHorizontalWidth = 65.dp
private const val PermissionModeMaxLength = 3