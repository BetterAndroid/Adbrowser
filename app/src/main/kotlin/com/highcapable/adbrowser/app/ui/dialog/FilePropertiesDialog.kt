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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.app.ui.dialog.base.DialogActionRow
import com.highcapable.adbrowser.app.ui.dialog.base.DialogScaffold
import com.highcapable.adbrowser.app.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel.PermissionAccess
import com.highcapable.adbrowser.app.ui.vm.FilePropertiesDialogModel.PermissionScope
import com.highcapable.adbrowser.app.ui.vm.MainStageModel
import com.highcapable.adbrowser.app.ui.vm.model.FileEntrySnapshot
import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.common.fs.FilePermission
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.awt.Window
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FilePropertiesDialog(
    snapshot: FileEntrySnapshot,
    onCloseRequest: () -> Unit,
    loadPermission: () -> OperationResult<FilePermission.Info>,
    applyPermission: (String) -> OperationResult<FilePermission.Info>,
    ownerWindow: Window? = null
) {
    val viewModel = remember(snapshot, loadPermission, applyPermission) {
        FilePropertiesDialogModel(
            snapshot = snapshot,
            loadPermissionAction = loadPermission,
            applyPermissionAction = applyPermission
        )
    }

    val invalidPermissionText = strings.dialogPropertiesInvalidPermission
    val unknownErrorText = strings.commonUnknownError
    val resolveDialogErrorText: (String?) -> String = { raw ->
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
        onCloseRequest = onCloseRequest,
        ownerWindow = ownerWindow,
        width = 620.dp
    ) {
        PropertyRow(strings.dialogPropertiesFieldName, snapshot.name)
        PropertyRow(strings.dialogPropertiesFieldPath, snapshot.fullPath, valueWrap = true)
        PropertyRow(
            strings.dialogPropertiesFieldType,
            when {
                snapshot.isSymlink -> strings.dialogPropertiesTypeSymlink
                snapshot.isDirectory -> strings.dialogPropertiesTypeDirectory
                else -> strings.dialogPropertiesTypeFile
            }
        )
        PropertyRow(
            strings.dialogPropertiesFieldSize,
            snapshot.friendlySizeText
        )
        PropertyRow(
            strings.dialogPropertiesFieldModified,
            DateFormatter.format(snapshot.modifiedAt.atZone(ZoneId.systemDefault()))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.dialogPropertiesFieldPermission,
                modifier = Modifier.width(140.dp),
                fontWeight = FontWeight.SemiBold
            )
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
                    modifier = Modifier
                        .width(80.dp)
                        .height(AdbrowserTheme.DefaultTextFieldHeight),
                    placeholder = { Text("---") }
                )
                OutlinedButton(
                    onClick = viewModel::applyPermission,
                    modifier = Modifier
                        .width(60.dp)
                        .height(AdbrowserTheme.DefaultTextFieldHeight)
                ) {
                    Text(strings.dialogPropertiesApply)
                }
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

        val errorText = viewModel.errorMessageRaw?.let(resolveDialogErrorText).orEmpty()
        DialogActionRow(
            primaryText = strings.dialogPropertiesClose,
            onPrimary = onCloseRequest,
            leadingText = errorText,
            leadingTextColor = JewelTheme.globalColors.text.error
        )
    }
}

@Composable
private fun PropertyRow(label: String, value: String, valueWrap: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(140.dp),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            maxLines = if (valueWrap) Int.MAX_VALUE else 1
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
            modifier = Modifier.width(140.dp),
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

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")