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
package com.highcapable.adbrowser.frontend.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.permission.model.FilePermissionInfo
import com.highcapable.adbrowser.frontend.ui.component.DialogScaffold
import com.highcapable.adbrowser.frontend.ui.theme.AdbrowserTheme
import com.highcapable.adbrowser.frontend.ui.vm.FilePropertiesDialogModel
import com.highcapable.adbrowser.frontend.ui.vm.FilePropertiesDialogModel.PermissionAccess
import com.highcapable.adbrowser.frontend.ui.vm.FilePropertiesDialogModel.PermissionScope
import com.highcapable.adbrowser.frontend.ui.vm.MainStageModel
import com.highcapable.adbrowser.frontend.ui.vm.model.FileEntrySnapshot
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FilePropertiesDialog(
    snapshot: FileEntrySnapshot,
    onCloseRequest: () -> Unit,
    loadPermission: () -> OperationResult<FilePermissionInfo>,
    applyPermission: (String) -> OperationResult<FilePermissionInfo>
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
        width = 620.dp,
        height = 420.dp
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
            if (snapshot.isDirectory) "-" else "%,d".format(snapshot.sizeBytes)
        )
        PropertyRow(
            strings.dialogPropertiesFieldModified,
            DateFormatter.format(snapshot.modifiedAt.atZone(ZoneId.systemDefault()))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.dialogPropertiesFieldPermission,
                modifier = Modifier.width(140.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = viewModel.symbolicPermission,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(70.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.dialogPropertiesFieldPermissionBits,
                modifier = Modifier.width(140.dp),
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionBitsColumn(
                    title = strings.dialogPropertiesOwner,
                    scope = PermissionScope.Owner,
                    viewModel = viewModel
                )
                PermissionBitsColumn(
                    title = strings.dialogPropertiesGroup,
                    scope = PermissionScope.Group,
                    viewModel = viewModel
                )
                PermissionBitsColumn(
                    title = strings.dialogPropertiesOther,
                    scope = PermissionScope.Other,
                    viewModel = viewModel
                )
            }
        }

        val errorText = viewModel.errorMessageRaw?.let(resolveDialogErrorText).orEmpty()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (errorText.isNotBlank()) {
                Text(
                    text = errorText,
                    color = Color(0xFFE46868),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else Spacer(Modifier.weight(1f))
            DefaultButton(onClick = onCloseRequest) {
                Text(strings.dialogPropertiesClose)
            }
        }
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
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            maxLines = if (valueWrap) Int.MAX_VALUE else 1
        )
    }
}

@Composable
private fun PermissionBitsColumn(
    title: String,
    scope: PermissionScope,
    viewModel: FilePropertiesDialogModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 3.dp))
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

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")