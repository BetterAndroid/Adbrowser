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
package com.highcapable.adbrowser.app.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.app.ui.vm.base.ViewModel
import com.highcapable.adbrowser.app.ui.vm.model.FileEntrySnapshot
import com.highcapable.adbrowser.core.adb.model.OperationResult
import com.highcapable.adbrowser.core.common.fs.FilePermission

class FilePropertiesDialogModel(
    private val snapshot: FileEntrySnapshot,
    private val loadPermissionAction: () -> OperationResult<FilePermission.Info>,
    private val applyPermissionAction: (String) -> OperationResult<FilePermission.Info>
) : ViewModel() {

    /**
     * UI-facing permission scopes used by the dialog checkbox matrix.
     */
    enum class PermissionScope { Owner, Group, Other }

    /**
     * UI-facing permission access bits used by the dialog checkbox matrix.
     */
    enum class PermissionAccess { Read, Write, Execute }

    sealed interface CloseResult {
        data object Close : CloseResult
        data class ConfirmDiscard(val messageRaw: String) : CloseResult
    }

    // Keep the editable permission state in one place. Everything else in the dialog is derived
    // from this value so text input and checkbox toggles behave consistently.
    private var currentMode by mutableStateOf(
        runCatching { FilePermission.toNumeric(snapshot.symbolicPermission) }.getOrDefault(0)
    )
    private var baselineMode by mutableStateOf(currentMode)

    private var initialized = false
    private var isSyncingModeField = false

    val modeState = TextFieldState("")
    var symbolicPermission by mutableStateOf(snapshot.symbolicPermission)
        private set

    var statusMessageRaw by mutableStateOf<String?>(null)
        private set

    var hasPermissionChanges by mutableStateOf(false)
        private set

    /**
     * Loads the latest permission info when the dialog is shown.
     *
     * If backend loading fails, the dialog still falls back to the permission already present in
     * the file snapshot so the UI remains usable and the failure can be shown inline.
     */
    fun initialize() {
        if (initialized) return

        initialized = true
        val result = loadPermissionAction()
        val info = result.data

        if (result.isOk && info != null) {
            syncFromMode(info.numericPermission)
            baselineMode = info.numericPermission
            clearStatusMessage()
        } else {
            if (modeState.text.isBlank()) modeState.edit { replace(0, length, "") }

            runCatching { FilePermission.toNumeric(snapshot.symbolicPermission) }
                .getOrNull()
                ?.let {
                    syncFromMode(it)
                    baselineMode = it
                }
            clearStatusMessage()
        }
        updatePermissionChangedState(modeState.text.toString())
    }

    /**
     * Applies user edits from the octal mode field back into the shared permission state.
     *
     * Clearing the field is treated as "cancel manual editing": the checkbox matrix and symbolic
     * preview snap back to the last committed permission while the text field itself stays blank.
     */
    fun onModeInputChanged(text: String) {
        // Ignore the callback triggered by our own programmatic field updates.
        if (isSyncingModeField) return

        clearStatusMessage()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            syncPermissionViewFromMode(baselineMode)
            hasPermissionChanges = false
            return
        }

        val mode = parseMode(trimmed) ?: run {
            updatePermissionChangedState(text)
            return
        }
        syncFromMode(mode)
        updatePermissionChangedState(text)
    }

    /**
     * Attempts to close the dialog, applying permission changes first when needed.
     *
     * If backend validation or persistence fails, the dialog stays open and returns a discard
     * confirmation payload that the UI can surface through a secondary confirmation dialog.
     */
    fun close(): CloseResult {
        if (!hasPermissionChanges) return CloseResult.Close

        val result = applyPermissionAction(submitModeText())
        val info = result.data

        if (result.isOk && info != null) {
            syncFromMode(info.numericPermission)
            baselineMode = info.numericPermission
            hasPermissionChanges = false
            clearStatusMessage()
            return CloseResult.Close
        }

        val message = result.errorMessage?.takeIf { it.isNotBlank() } ?: MainStageModel.UNKNOWN_ERROR_TOKEN
        statusMessageRaw = message
        return CloseResult.ConfirmDiscard(message)
    }

    /** Returns the current checkbox state for one permission bit in the dialog matrix. */
    fun isPermissionBitChecked(scope: PermissionScope, access: PermissionAccess): Boolean = when (scope) {
        PermissionScope.Owner,
        PermissionScope.Group,
        PermissionScope.Other -> FilePermission.hasAccess(
            mode = currentMode,
            scope = scope.toCoreScope(),
            access = access.toCoreAccess()
        )
    }

    /** Updates one checkbox bit and rebuilds the shared permission mode from it. */
    fun onPermissionBitChanged(scope: PermissionScope, access: PermissionAccess, checked: Boolean) {
        val mode = FilePermission.setAccess(
            mode = currentMode,
            scope = scope.toCoreScope(),
            access = access.toCoreAccess(),
            enabled = checked
        )
        syncFromMode(mode)
        clearStatusMessage()
        updatePermissionChangedState(mode.toString())
    }

    /** Pushes a new mode into every derived dialog representation. */
    private fun syncFromMode(mode: Int) {
        syncPermissionViewFromMode(mode)
        syncModeField(mode)
    }

    /** Updates only the derived permission preview state without rewriting the text field value. */
    private fun syncPermissionViewFromMode(mode: Int) {
        currentMode = mode
        symbolicPermission = FilePermission.toSymbolic(mode)
    }

    /**
     * Updates the mode text field without re-entering [onModeInputChanged].
     *
     * Compose text state emits changes for both user input and programmatic edits, so this guard
     * prevents a harmless sync write from being interpreted as another manual edit cycle.
     */
    private fun syncModeField(mode: Int) {
        isSyncingModeField = true
        modeState.edit {
            replace(0, length, mode.toString())
        }
        isSyncingModeField = false
    }

    private fun updatePermissionChangedState(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            hasPermissionChanges = false
            return
        }
        val parsedMode = parseMode(trimmed)

        hasPermissionChanges = if (parsedMode != null)
            parsedMode != baselineMode
        else trimmed.isNotEmpty()
    }

    private fun submitModeText() = modeState.text.toString().trim()

    private fun parseMode(text: String) = FilePermission.parseMode(text.trim())

    private fun clearStatusMessage() {
        statusMessageRaw = null
    }

    private fun PermissionScope.toCoreScope() = when (this) {
        PermissionScope.Owner -> FilePermission.Scope.Owner
        PermissionScope.Group -> FilePermission.Scope.Group
        PermissionScope.Other -> FilePermission.Scope.Other
    }

    private fun PermissionAccess.toCoreAccess() = when (this) {
        PermissionAccess.Read -> FilePermission.Access.Read
        PermissionAccess.Write -> FilePermission.Access.Write
        PermissionAccess.Execute -> FilePermission.Access.Execute
    }
}