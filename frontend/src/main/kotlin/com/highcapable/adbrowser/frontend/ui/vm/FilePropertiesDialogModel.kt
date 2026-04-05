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
package com.highcapable.adbrowser.frontend.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.backend.domain.OperationResult
import com.highcapable.adbrowser.backend.permission.model.FilePermissionInfo
import com.highcapable.adbrowser.frontend.ui.vm.base.ViewModel
import com.highcapable.adbrowser.frontend.ui.vm.model.FileEntrySnapshot

class FilePropertiesDialogModel(
    private val snapshot: FileEntrySnapshot,
    private val loadPermissionAction: () -> OperationResult<FilePermissionInfo>,
    private val applyPermissionAction: (String) -> OperationResult<FilePermissionInfo>
) : ViewModel() {

    enum class PermissionScope { Owner, Group, Other }

    enum class PermissionAccess { Read, Write, Execute }

    val modeState = TextFieldState("")

    var symbolicPermission by mutableStateOf(snapshot.symbolicPermission)
        private set
    var errorMessageRaw by mutableStateOf<String?>(null)
        private set

    private var ownerRead by mutableStateOf(false)
    private var ownerWrite by mutableStateOf(false)
    private var ownerExecute by mutableStateOf(false)

    private var groupRead by mutableStateOf(false)
    private var groupWrite by mutableStateOf(false)
    private var groupExecute by mutableStateOf(false)

    private var otherRead by mutableStateOf(false)
    private var otherWrite by mutableStateOf(false)
    private var otherExecute by mutableStateOf(false)

    private var initialized = false
    private var isSyncingModeField = false
    private var isSyncingBits = false

    fun initialize() {
        if (initialized) return

        initialized = true
        val result = loadPermissionAction()
        val info = result.data

        if (result.isOk && info != null) {
            syncFromMode(info.numericPermission)
            errorMessageRaw = null
        } else {
            if (modeState.text.isBlank()) {
                modeState.edit { replace(0, length, "") }
            }
            parsePermissionMode(snapshot.symbolicPermission)?.let { syncFromMode(it) }
            errorMessageRaw = result.errorMessage?.takeIf { it.isNotBlank() } ?: MainStageModel.UNKNOWN_ERROR_TOKEN
        }
    }

    fun onModeInputChanged(text: String) {
        if (isSyncingModeField) return

        val mode = parsePermissionMode(text) ?: return
        symbolicPermission = modeToSymbolic(mode)
        syncBitsFromMode(mode)
        errorMessageRaw = null
    }

    fun applyPermission() {
        val result = applyPermissionAction(modeState.text.toString().trim())
        val info = result.data

        if (result.isOk && info != null) {
            syncFromMode(info.numericPermission)
            errorMessageRaw = null
        } else errorMessageRaw = result.errorMessage?.takeIf { it.isNotBlank() } ?: MainStageModel.UNKNOWN_ERROR_TOKEN
    }

    fun isPermissionBitChecked(scope: PermissionScope, access: PermissionAccess): Boolean = when (scope) {
        PermissionScope.Owner -> when (access) {
            PermissionAccess.Read -> ownerRead
            PermissionAccess.Write -> ownerWrite
            PermissionAccess.Execute -> ownerExecute
        }
        PermissionScope.Group -> when (access) {
            PermissionAccess.Read -> groupRead
            PermissionAccess.Write -> groupWrite
            PermissionAccess.Execute -> groupExecute
        }
        PermissionScope.Other -> when (access) {
            PermissionAccess.Read -> otherRead
            PermissionAccess.Write -> otherWrite
            PermissionAccess.Execute -> otherExecute
        }
    }

    fun onPermissionBitChanged(scope: PermissionScope, access: PermissionAccess, checked: Boolean) {
        when (scope) {
            PermissionScope.Owner -> when (access) {
                PermissionAccess.Read -> ownerRead = checked
                PermissionAccess.Write -> ownerWrite = checked
                PermissionAccess.Execute -> ownerExecute = checked
            }
            PermissionScope.Group -> when (access) {
                PermissionAccess.Read -> groupRead = checked
                PermissionAccess.Write -> groupWrite = checked
                PermissionAccess.Execute -> groupExecute = checked
            }
            PermissionScope.Other -> when (access) {
                PermissionAccess.Read -> otherRead = checked
                PermissionAccess.Write -> otherWrite = checked
                PermissionAccess.Execute -> otherExecute = checked
            }
        }

        if (isSyncingBits) return

        val mode = buildModeFromBits()
        symbolicPermission = modeToSymbolic(mode)
        syncModeField(mode)
        errorMessageRaw = null
    }

    private fun syncFromMode(mode: Int) {
        symbolicPermission = modeToSymbolic(mode)
        syncModeField(mode)
        syncBitsFromMode(mode)
    }

    private fun syncModeField(mode: Int) {
        isSyncingModeField = true
        modeState.edit {
            replace(0, length, mode.toString())
        }
        isSyncingModeField = false
    }

    private fun syncBitsFromMode(mode: Int) {
        val owner = mode / 100
        val group = (mode / 10) % 10
        val other = mode % 10

        isSyncingBits = true

        ownerRead = (owner and 4) != 0
        ownerWrite = (owner and 2) != 0
        ownerExecute = (owner and 1) != 0

        groupRead = (group and 4) != 0
        groupWrite = (group and 2) != 0
        groupExecute = (group and 1) != 0

        otherRead = (other and 4) != 0
        otherWrite = (other and 2) != 0
        otherExecute = (other and 1) != 0

        isSyncingBits = false
    }

    private fun buildModeFromBits(): Int {
        val owner = (if (ownerRead) 4 else 0) + (if (ownerWrite) 2 else 0) + (if (ownerExecute) 1 else 0)
        val group = (if (groupRead) 4 else 0) + (if (groupWrite) 2 else 0) + (if (groupExecute) 1 else 0)
        val other = (if (otherRead) 4 else 0) + (if (otherWrite) 2 else 0) + (if (otherExecute) 1 else 0)

        return owner * 100 + group * 10 + other
    }

    private fun parsePermissionMode(modeText: String?): Int? {
        val parsed = modeText?.trim()?.toIntOrNull() ?: return null
        if (parsed !in 0..777) return null

        val owner = parsed / 100
        val group = (parsed / 10) % 10
        val other = parsed % 10

        @Suppress("KotlinConstantConditions")
        if (owner > 7 || group > 7 || other > 7) return null

        return parsed
    }

    private fun modeToSymbolic(mode: Int): String {
        val owner = mode / 100
        val group = (mode / 10) % 10
        val other = mode % 10

        return "${octalToRwx(owner)}${octalToRwx(group)}${octalToRwx(other)}"
    }

    private fun octalToRwx(value: Int): String {
        val read = if ((value and 4) != 0) 'r' else '-'
        val write = if ((value and 2) != 0) 'w' else '-'
        val execute = if ((value and 1) != 0) 'x' else '-'

        return "$read$write$execute"
    }
}