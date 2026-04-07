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
 * This file is created by fankes on 2026/4/3.
 */
package com.highcapable.adbrowser.frontend.ui.vm

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.adbrowser.backend.setting.AppSettings
import com.highcapable.adbrowser.frontend.cl.AppState
import com.highcapable.adbrowser.frontend.locale.Locales
import com.highcapable.adbrowser.frontend.locale.normalizeStoredLanguageTag
import com.highcapable.adbrowser.frontend.ui.utils.SystemFileChooser
import com.highcapable.adbrowser.frontend.ui.vm.base.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Window
import java.nio.file.Files
import java.nio.file.Path

class PreferencesStageModel(private val appState: AppState) : ViewModel() {

    private companion object {

        const val DEFAULT_DEVICE_PANE_WIDTH = 300.0
        const val DEFAULT_FILE_COLUMN_WIDTH_NAME = 260.0
        const val DEFAULT_FILE_COLUMN_WIDTH_SIZE = 140.0
        const val DEFAULT_FILE_COLUMN_WIDTH_MODIFIED = 240.0
        const val DEFAULT_FILE_COLUMN_WIDTH_PERMISSION = 150.0
        const val ADB_VALIDATE_TIMEOUT_MS = 5000L
    }

    private val settingsService get() = appState.appServices.settingsService
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    enum class Tab { General, Files, Device }
    enum class StatusCategory { Normal, Error }

    sealed interface Status {
        data object None : Status
        data object SidebarSpacingReset : Status
        data object FileColumnWidthsReset : Status
        data object PreferencesSaved : Status
        data object Cancelled : Status
        data object AdbPathNotFound : Status
        data object AdbExecutableInvalid : Status
        data object AdbPathEmpty : Status
        data class Failed(val reason: String?) : Status
    }

    var currentTab by mutableStateOf(Tab.General)

    var selectedLanguageTag by mutableStateOf(Locales.FOLLOW_SYSTEM)

    var showHiddenFiles by mutableStateOf(false)
    var foldersFirst by mutableStateOf(true)
    var rememberLastDisplayStyle by mutableStateOf(true)

    val adbExecPath = TextFieldState("")
    var superuser by mutableStateOf(false)

    var status by mutableStateOf<Status>(Status.None)
    var isSaving by mutableStateOf(false)
        private set

    val statusCategory get() = when (status) {
        is Status.None,
        is Status.SidebarSpacingReset,
        is Status.FileColumnWidthsReset,
        is Status.PreferencesSaved,
        is Status.Cancelled -> StatusCategory.Normal
        is Status.AdbPathNotFound,
        is Status.AdbExecutableInvalid,
        is Status.AdbPathEmpty,
        is Status.Failed -> StatusCategory.Error
    }

    init {
        restoreFromSettings()
    }

    fun resetSidebarSpacing() {
        val success = runPersistAction(
            successStatus = Status.SidebarSpacingReset
        ) {
            settingsService.current.devicePaneWidth = DEFAULT_DEVICE_PANE_WIDTH
            settingsService.save()
        }
        if (success) appState.sync()
    }

    fun resetFileColumnWidths() {
        val success = runPersistAction(
            successStatus = Status.FileColumnWidthsReset
        ) {
            settingsService.current.fileColumnWidthName = DEFAULT_FILE_COLUMN_WIDTH_NAME
            settingsService.current.fileColumnWidthSize = DEFAULT_FILE_COLUMN_WIDTH_SIZE
            settingsService.current.fileColumnWidthModified = DEFAULT_FILE_COLUMN_WIDTH_MODIFIED
            settingsService.current.fileColumnWidthPermission = DEFAULT_FILE_COLUMN_WIDTH_PERMISSION
            settingsService.save()
        }
        if (success) appState.sync()
    }

    fun save(onSuccess: () -> Unit = {}) {
        if (isSaving) return

        modelScope.launch {
            isSaving = true
            try {
                when (validateAdbPath()) {
                    ValidationResult.PathEmpty -> {
                        status = Status.AdbPathEmpty
                        return@launch
                    }
                    ValidationResult.PathNotFound -> {
                        status = Status.AdbPathNotFound
                        return@launch
                    }
                    ValidationResult.ExecutableInvalid -> {
                        status = Status.AdbExecutableInvalid
                        return@launch
                    }
                    ValidationResult.Ok -> Unit
                }

                val current = settingsService.current
                val shouldRefreshFileList = current.showHiddenFiles != showHiddenFiles ||
                    current.foldersFirst != foldersFirst ||
                    current.rememberLastDisplayStyle != rememberLastDisplayStyle

                val saved = runPersistActionAsync(
                    successStatus = Status.PreferencesSaved
                ) {
                    settingsService.current.language = normalizeStoredLanguageTag(selectedLanguageTag)
                    settingsService.current.adbExecPath = adbExecPath.text.toString().trim()
                    settingsService.current.useSuperuser = superuser
                    settingsService.current.showHiddenFiles = showHiddenFiles
                    settingsService.current.foldersFirst = foldersFirst
                    settingsService.current.rememberLastDisplayStyle = rememberLastDisplayStyle
                    if (!rememberLastDisplayStyle) {
                        settingsService.current.lastFileViewMode = AppSettings.FileViewMode.List
                    }
                    withContext(Dispatchers.IO) {
                        settingsService.save()
                    }
                }

                if (saved) {
                    appState.sync(refreshFileList = shouldRefreshFileList)
                    onSuccess()
                }
            } finally {
                isSaving = false
            }
        }
    }
    
    fun dispose() {
        modelScope.cancel()
    }

    fun browseAdbPath(parentWindow: Window?, dialogTitle: String) {
        val selectedPath = SystemFileChooser.chooseFile(
            parent = parentWindow,
            title = dialogTitle,
            initialPath = adbExecPath.text.toString()
        ) ?: return

        setAdbPath(selectedPath)
    }

    fun cancel() {
        restoreFromSettings()
        status = Status.Cancelled
    }

    private fun restoreFromSettings() {
        val settings = settingsService.current
        selectedLanguageTag = normalizeStoredLanguageTag(settings.language)
        showHiddenFiles = settings.showHiddenFiles
        foldersFirst = settings.foldersFirst
        rememberLastDisplayStyle = settings.rememberLastDisplayStyle
        superuser = settings.useSuperuser
        setAdbPath(settings.adbExecPath)
    }

    private fun runPersistAction(successStatus: Status, block: suspend () -> Unit): Boolean =
        runCatching { runBlocking { block() } }
            .onSuccess { status = successStatus }
            .onFailure { status = Status.Failed(it.message) }
            .isSuccess

    private suspend fun runPersistActionAsync(successStatus: Status, block: suspend () -> Unit): Boolean =
        runCatching { block() }
            .onSuccess { status = successStatus }
            .onFailure { status = Status.Failed(it.message) }
            .isSuccess

    private enum class ValidationResult {
        Ok,
        PathEmpty,
        PathNotFound,
        ExecutableInvalid
    }

    private suspend fun validateAdbPath(): ValidationResult {
        val path = adbExecPath.text.toString().trim()
        if (path.isBlank()) return ValidationResult.PathEmpty

        val parsedPath = runCatching { Path.of(path) }.getOrNull()
            ?: return ValidationResult.PathNotFound
        if (!Files.exists(parsedPath)) return ValidationResult.PathNotFound

        val result = withContext(Dispatchers.IO) {
            withTimeoutOrNull(ADB_VALIDATE_TIMEOUT_MS) {
                runCatching {
                    appState.appServices.adbClient.validateAdbExecPath(path)
                }.getOrNull()
            }
        }

        if (result?.isOk != true) return ValidationResult.ExecutableInvalid

        return ValidationResult.Ok
    }

    private fun setAdbPath(path: String) {
        val value = path.trim()
        adbExecPath.edit {
            replace(0, length, value)
        }
    }
}